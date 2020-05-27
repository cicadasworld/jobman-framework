package gtcloud.jobman.core.scheduler.main;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.jobman.core.common.Helper;
import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.JobStatusDO;
import gtcloud.jobman.core.pdo.JobStatusListDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.Phase;
import gtcloud.jobman.core.pdo.SchedulerStateSummaryDO;
import gtcloud.jobman.core.pdo.StepTag;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobDumpStateListDO;
import gtcloud.jobman.core.pdo.SubjobProcessNodeDO;
import gtcloud.jobman.core.pdo.SubjobProcessNodeListDO;
import gtcloud.jobman.core.pdo.SubjobStatusDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;
import gtcloud.jobman.core.scheduler.JobDispatcher;
import gtcloud.jobman.core.scheduler.JobDispatcherContext;
import gtcloud.jobman.core.scheduler.JobDispatcherFactory;
import gtcloud.jobman.core.scheduler.JobEntry;
import gtcloud.jobman.core.scheduler.JobState;
import gtcloud.jobman.core.scheduler.Subjob;
import gtcloud.jobman.core.scheduler.SubjobComparator;
import gtcloud.jobman.core.scheduler.SubjobEntry;
import gtcloud.jobman.core.scheduler.SubjobProcessNode;
import gtcloud.jobman.core.scheduler.event.SubjobBaseEvent;
import gtcloud.jobman.core.scheduler.event.SubjobDispatchAckReceivedEvent;
import gtcloud.jobman.core.scheduler.event.SubjobDispatchReqSentEvent;
import gtcloud.jobman.core.scheduler.event.SubjobStatusReportEvent;
import gtcloud.jobman.core.scheduler.event.SubjobStepBeginEvent;
import gtcloud.jobman.core.scheduler.event.SubjobStepEndEvent;
import gtcloud.jobman.core.scheduler.mission.MissionQueryFilter;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionDO;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionItemDO;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionListDO;
import platon.ByteSeq;
import platon.IntHolder;
import platon.StringHolder;

public class JobScheduler implements JobDispatcherContext {

    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

    private final WorkerNodeManager nodeManager;

    private final JobDispatcherCreator dispatcherCreator = new JobDispatcherCreator();

    // JobCategory --> JobDispatcher
    private final HashMap<String, JobDispatcher> dispatcherLookup = new HashMap<>();

    // 调度线程在该队列上等待任务
    private final LinkedBlockingQueue<Object> jobCmdQueue = new LinkedBlockingQueue<>();

    private final JobSchedulerHelper helper;

    // 已完成作业表
    private final ConcurrentHashMap<String, JobEntry> finishedJobs = new ConcurrentHashMap<>();

    // 未完成作业表
    private final ConcurrentHashMap<String, JobEntry> pendingJobs = new ConcurrentHashMap<>();

    // 作业持久化器
    private final FlatJobPersister jobPersister;

    private final long finishedJobKeepDays;

    private long whenLastPurgeStaleFinishedJobs = System.currentTimeMillis();

    // 对于某个子作业，任何时候它只能位于下列4个队列之一中
    private final TreeSet<SubjobEntry>               waitingQueue;    //等待队列（等待被分派）
    private final LinkedHashMap<String, SubjobEntry> sendingQueue;    //发送队列，该队列中的子作业已经被发送给了某个处理节点，但还未收到ack
    private final LinkedHashMap<String, SubjobEntry> dispatchedQueue; //已分派队列，该队列中的子作业已经被发送给了某个处理节点，且已收到ack，但还未执行完
    private final LinkedHashMap<String, SubjobEntry> retryQueue;      //重试队列，该队列中的子作业之前处理失败了，现正等待重做

    // 子作业失败后最多可以重试多少次
    private final int maxSubjobRetryTimes;

    // 子作业失败后可以等待的时间间隔
    private final int retrySubjobIntervalMillis;

    // "新作业到来"命令
    // XXX: 代码中使用了反射机制来执行命令，约定命令类名必须形如"xxxCommand"，对应的处理方法必须形如"processJobCmd_xxx"，下同;
    // 如 NewJobReceivedCommand 的处理方法名为 processJobCmd_NewJobReceived
    private static class NewJobReceivedCommand {
        JobControlBlockDO jobCB;
        ByteSeq           jobBody;
        JobDispatcher     dispatcher;
    }

    // "退出"命令
    private static class QuitCommand {
        final CountDownLatch latch = new CountDownLatch(1);
    }

    // "触发调度"命令
    private static class DispatchCommand {
        static final DispatchCommand INSTANCE = new DispatchCommand();
    }

    // "唤醒"命令
    private static class WakeupCommand {
        static final WakeupCommand INSTANCE = new WakeupCommand();
    }

    // "子作业分派应答已经接收"
    private static class SubjobDispatchAckCommand {
        final SubjobDispatchAckDO ack;
        SubjobDispatchAckCommand(SubjobDispatchAckDO ack) {
            this.ack = ack;
        }
    }

    // "节点登录"
    private static class NodeLogonReqCommand {
        final String nodeBaseURL;
        final LogonReqDO pdo;
        NodeLogonReqCommand(String nodeBaseURL, LogonReqDO pdo) {
            this.nodeBaseURL = nodeBaseURL;
            this.pdo = pdo;
        }
    }

    // "节点登出"
    private static class NodeLogoffReqCommand {
        final LogoffDO pdo;
        NodeLogoffReqCommand(LogoffDO pdo) {
            this.pdo = pdo;
        }
    }

    // "节点心跳报告"
    private static class NodeHeartbeatReqCommand {
        final String nodeBaseURL;
        final HeartbeatReportReqDO pdo;
        NodeHeartbeatReqCommand(String nodeBaseURL, HeartbeatReportReqDO pdo) {
            this.nodeBaseURL = nodeBaseURL;
            this.pdo = pdo;
        }
    }

    // "子作业状态报告"
    private static class SubjobStatusReportCommand {
        final SubjobStatusReportDO pdo;
        SubjobStatusReportCommand(SubjobStatusReportDO pdo) {
            this.pdo = pdo;
        }
    }

    public JobScheduler(JobSchedulerHelper helper) throws Exception {
        this.helper = helper;
        this.nodeManager = new WorkerNodeManager(this, helper);
        this.waitingQueue = new TreeSet<>(new SubjobComparator());
        this.sendingQueue = new LinkedHashMap<>();
        this.dispatchedQueue = new LinkedHashMap<>();
        this.retryQueue = new LinkedHashMap<>();

        // 已经执行完成的作业在"finished目录"保留几天? 之后会移动到归档目录
        String sdays = helper.getProperty("gtcloud.scheduler.finishedJobKeepDays", "7");
        this.finishedJobKeepDays = Integer.parseInt(sdays);

        String paramName = "gtcloud.scheduler.jobDataTopDir";
        String jobDataTopDir = helper.getProperty(paramName, null);
        if (jobDataTopDir == null) {
            throw new Exception("缺少配置参数: " + paramName);
        }
        this.jobPersister = new FlatJobPersister(jobDataTopDir, finishedJobKeepDays);

        this.maxSubjobRetryTimes = Integer.parseInt(helper.getProperty("gtcloud.scheduler.jobRetryTimes", "5"));
        this.retrySubjobIntervalMillis = Integer.parseInt(helper.getProperty("gtcloud.scheduler.retryJobWaitIntervalMillis", "5000"));
    }

    public void run(String[] args) throws Exception {
        // 从持久存储中加载之前运行留下的作业列表
        this.jobPersister.readJobs(pendingJobs, finishedJobs);

        // 将各个未完成的子作业入队，等待后续调度
        for (JobEntry job : pendingJobs.values()) {
            for (SubjobEntry subjob : job.getSubjobs()) {
                if (subjob.getState() == JobState.PENDING) {
                    this.waitingQueue.add(subjob);
                }
            }
        }

        // 启动各个作业分片器
        this.dispatcherCreator.init();
        final JobDispatcherContext ctx = this;
        for (String jobCategory : this.dispatcherCreator.getJobCategoryList()) {
            JobDispatcher d = this.dispatcherCreator.getJobDispatcher(jobCategory);
            if (d == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("cannot get jobdispatcher, jobcategory={}", jobCategory);
                }
                continue;
            }
            d.onStart(ctx);
            this.dispatcherLookup.put(jobCategory, d);
        }

        // 启动分派线程
        Thread t = new Thread(() -> {
            runCommandLoop();
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        // 停止分派线程
        LOG.info("stop() received.");
        QuitCommand cmd = new QuitCommand();
        this.jobCmdQueue.offer(cmd);
        try {
            cmd.latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 停止各个作业分派器
        for (JobDispatcher d : this.dispatcherLookup.values()) {
            d.onStop();
        }

        // 清理处理
        this.dispatcherCreator.fini();
    }

    private void runCommandLoop() {
        // 扫描间隔毫秒数
        final String t = this.helper.getProperty("gtcloud.scheduler.jobScanIntervalMillis", null);
        final long intervalMillis = t != null ? Long.parseLong(t) : 30*1000;

        long whenLastRunPeriodicalWork = 0;

        LOG.info("runCommandLoop() begin, interval={}(ms)", intervalMillis);
        for (long round=0; ; round++) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("runDispatchLoop(), round=" + round);
            }
            try {
                // 先处理交互式命令
                processRestCommands_i();

                // 再处理作业调度相关命令
                boolean goingon = processJobCommands_i(round, intervalMillis);
                if (!goingon) {
                    break;
                }

                // 处理定期事宜
                long now = System.currentTimeMillis();
                if (now - whenLastRunPeriodicalWork >= intervalMillis) {
                    whenLastRunPeriodicalWork = now;
                    runPeriodicalWork();
                }
            } catch (Throwable ex) {
                LOG.error("runCommandLoop_i(): exception caught.", ex);
            }
        }
    }

    // 若收到退出命令返回false, 否则返回true
    private boolean processJobCommands_i(long round, long intervalMillis) {
        // 扫描间隔毫秒数
        if (round == 0) {
            intervalMillis = 5 * 1000;
        }

        Object cmd;
        try {
            cmd = this.jobCmdQueue.poll(intervalMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }

        if (cmd == null) {  //超时
            return true;
        }

        if (cmd instanceof DispatchCommand) {
            processJobCmd_Dispatch();
            return true;
        }

        if (cmd instanceof WakeupCommand) {
            return true;
        }

        if (cmd instanceof QuitCommand) {
            QuitCommand c = (QuitCommand)cmd;
            c.latch.countDown();
            return false;
        }

        // 反射机制执行其它命令
        Method method = getProcessCmdMethod(cmd, "processJobCmd_", "Command");
        if (method != null) {
            try {
                method.invoke(this, cmd);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOG.error("命令处理方法执行失败", ex);
            }
        }

        return true;
    }

    private void runPeriodicalWork() {
        this.jobPersister.doPeriodicalWork();
        tryPurgeStaleFinishedJobs();
        processJobCmd_Dispatch();
    }

    // 从"已完成作业队列"中移除那些保留时间超限的作业，释放内存空间
    private void tryPurgeStaleFinishedJobs() {
        final long now = System.currentTimeMillis();
        final long elapsedMillis = now - this.whenLastPurgeStaleFinishedJobs;
        final long TEN_MINUTES = 10*60*1000L;
        if (elapsedMillis < TEN_MINUTES) {
            return;
        }
        this.whenLastPurgeStaleFinishedJobs = now;

        final long T0 = System.currentTimeMillis() - (this.finishedJobKeepDays * 24*3600*1000L);
        final ArrayList<String> jobIdsToPurge = new ArrayList<>();
        for (Entry<String, JobEntry> e : this.finishedJobs.entrySet()) {
            JobEntry job = e.getValue();
            if (job.getBornTime() <= T0) {
                jobIdsToPurge.add(e.getKey());
            }
        }
        for (String jobId : jobIdsToPurge) {
            this.finishedJobs.remove(jobId);
        }
    }

    // 作业提交客户端提交作业.
    // 返回状态码, 0表示成功, 其它表示错误.
    public int handleJobScheduleRequest(JobControlBlockDO jobCB,
                                        ByteSeq jobBody,
                                        StringHolder statusMessage) {
        // 检查jodId是否重复
        final String jobId = jobCB.getJobId();
        if (this.pendingJobs.containsKey(jobId) || this.finishedJobs.containsKey(jobId)) {
            String emsg = statusMessage.value = "jobId重复, jobId=" + jobId;
            LOG.error(emsg);
            return -1;
        }

        // 获得支持给定作业类别的在线节点列表
        final String jobCategory = jobCB.getJobCategory();
        if (jobCB.getOptions().get("aliveNodeMustBePresent") != null) {
            ArrayList<WorkerNode> nodeList = this.nodeManager.getAliveNodeList(jobCategory);
            if (nodeList == null || nodeList.size() == 0) {
                String emsg = statusMessage.value = "当前没有可用的作业处理节点";
                LOG.error(emsg);
                return -1;
            }
        }

        // 获得作业分派器
        final JobDispatcher dispatcher = this.dispatcherLookup.get(jobCategory);
        if (dispatcher == null) {
            String emsg = statusMessage.value = "找不到作业分派器, 作业类别=" + jobCategory;
            LOG.error(emsg);
            return -1;
        }

        // 将请求放入命令队列
        NewJobReceivedCommand cmd = new NewJobReceivedCommand();
        cmd.jobCB = jobCB;
        cmd.jobBody = jobBody;
        cmd.dispatcher = dispatcher;
        this.jobCmdQueue.offer(cmd);

        return 0;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_NewJobReceived(Object command) {
        NewJobReceivedCommand cmd = (NewJobReceivedCommand)command;
        try {
            // 进行作业拆分
            final String jobCategory = cmd.jobCB.getJobCategory();
            final ArrayList<WorkerNode> nodeList = this.nodeManager.getAliveNodeList(jobCategory);
            final ArrayList<SubjobProcessNode> nodes = new ArrayList<>(nodeList);
            ArrayList<Subjob> rawSubjobs = cmd.dispatcher.onSplitJob(cmd.jobCB, cmd.jobBody, nodes);
            if (rawSubjobs == null || rawSubjobs.size() == 0) {
                return;
            }
            ArrayList<SubjobEntry> subjobs = new ArrayList<>();
            rawSubjobs.forEach((s) -> {
                subjobs.add(new SubjobEntry(this.jobPersister, s));
            });

            // 写入持久存储
            long now = System.currentTimeMillis();
            for (SubjobEntry subjob : subjobs) {
                subjob.setBornTime(now);
            }
            JobEntry job = new JobEntry(this.jobPersister, cmd.jobCB, subjobs, now);
            this.jobPersister.insertNewJob(job);

            // 加入到内存列表中
            String jobId = job.getControlBlock().getJobId();
            this.pendingJobs.put(jobId, job);

            // 子作业入队, 等待分派
            for (SubjobEntry subjob : subjobs) {
                this.waitingQueue.add(subjob);
            }

            // 触发下一轮调度
            this.jobCmdQueue.offer(DispatchCommand.INSTANCE);

        } catch (Throwable ex) {
            LOG.error("处理NewJobReceivedCommand发生错误", ex);
        }
    }

    private void processJobCmd_Dispatch() {
        // 回收"发送队列"中子作业到"等待队列"中
        // (1) 预定时间内还没有收到"DispatchAck"
        // (2) 处理节点故障了
        if (!this.sendingQueue.isEmpty()) {
            recycleSubjobsInSendingQueue();
        }

        // 回收"已分派队列"中的子作业，若处理节点故障了，则加入到"重做队列"中
        if (!this.dispatchedQueue.isEmpty()) {
            recycleSubjobsInDispatchedQueue();
        }

        // 回收"重试队列"中的子作业，若其重做时刻已到，则重新加入到"等待队列"中
        if (!this.retryQueue.isEmpty()) {
            recycleSubjobsInRetryQueue();
        }

        // 调度"等待队列"中的各个子作业
        if (!this.waitingQueue.isEmpty()) {
            if (this.nodeManager.getAliveNodeCount() > 0) {
                dispatchSubjobsInWaitingQueue();
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("当前没有可用的作业处理节点, 无法调度子作业。");
            }
        }
    }

    // 回收"发送队列"中子作业到"等待队列"中
    // (1) 预定时间内还没有收到"DispatchAck"
    // (2) 处理节点故障了
    private void recycleSubjobsInSendingQueue() {
        //
        // 子作业发送给处理节点后, 若在该参数指定的毫秒数内收到处理节点的应答则认为发送成功, 否则认为发送失败
        //
        final long TIMEOUT_MILLIS = 30 * 1000;

        final Set<Entry<String, SubjobEntry>> set = this.sendingQueue.entrySet();
        final Iterator<Entry<String, SubjobEntry>> it = set.iterator();
        final long now = System.currentTimeMillis();
        while (it.hasNext()) {
            Entry<String, SubjobEntry> e = it.next();
            SubjobEntry subjob = e.getValue();
            final long t = subjob.getWhenLastDispatchReqSent();
            final boolean isExpired = now - t >= TIMEOUT_MILLIS;

            final String nodeId = subjob.getLastWorkerNodeId();
            final WorkerNode node = this.nodeManager.getNodeById(nodeId);
            final boolean isNodeCrashed = !((node != null) && node.isAlive());

            if (isExpired || isNodeCrashed) {
                this.waitingQueue.add(subjob); //等待下一次正常分派
                it.remove();
            }

            // 更新内存中维护的node工作负载等状态
            if (isExpired && node != null) {
                String jobId = subjob.getSubjobCB().getJobId();
                int subjobSeqNo = subjob.getSubjobCB().getSubjobSeqNo();
                node.afterSubjobDispatchReqExpired(jobId, subjobSeqNo);
            }
        }
    }

    // 回收"已分派队列"中的子作业，若处理节点故障了，则加入到"重做队列"中
    private void recycleSubjobsInDispatchedQueue() {
        final Set<Entry<String, SubjobEntry>> set = this.dispatchedQueue.entrySet();
        final Iterator<Entry<String, SubjobEntry>> it = set.iterator();
        while (it.hasNext()) {
            Entry<String, SubjobEntry> e = it.next();
            SubjobEntry subjob = e.getValue();

            final String nodeId = subjob.getLastWorkerNodeId();
            final WorkerNode node = this.nodeManager.getNodeById(nodeId);
            final boolean isNodeCrashed = !((node != null) && node.isAlive());
            if (!isNodeCrashed) {
                continue;
            }

            // 从"已分派队列"中移除
            it.remove();

            // 加入"重做队列"
            final long now = System.currentTimeMillis();
            subjob.setWhenLastEnterRetryQueue(now);
            this.retryQueue.put(e.getKey(), subjob);
        }
    }

    // 回收"重试队列"中的子作业，若其重做时刻已到，则重新加入到"等待队列"中
    private void recycleSubjobsInRetryQueue() {
        final Set<Entry<String, SubjobEntry>> set = this.retryQueue.entrySet();
        final Iterator<Entry<String, SubjobEntry>> it = set.iterator();
        final long now = System.currentTimeMillis();
        while (it.hasNext()) {
            Entry<String, SubjobEntry> e = it.next();
            SubjobEntry subjob = e.getValue();

            long whenLastEnterRetryQueue = subjob.getWhenLastEnterRetryQueue();
            int k = Math.min(subjob.getRetryTimes(), 5);
            long dueTime = whenLastEnterRetryQueue + this.retrySubjobIntervalMillis * (int)Math.pow(2, k);
            if (now > dueTime) {
                subjob.increaseRetryTimes();
                this.waitingQueue.add(subjob); //等待下一次正常分派
                it.remove();
            }
        }
    }

    private static class NodeListHolder {
        int nodeIndex = 0;
        ArrayList<WorkerNode> nodeList = null;
        boolean hasAvailableNode = true; //最近一次是否有可用的节点
    }

    private NodeListHolder getNodeListHolder(HashMap<String, NodeListHolder> cache,
                                             String jobCategory) {
        NodeListHolder h = cache.get(jobCategory);
        if (h != null) {
            return h;
        }

        h = new NodeListHolder();
        h.nodeList = getAliveNodeList(jobCategory);
        if (h.nodeList == null || h.nodeList.isEmpty()) {
            return null;
        }
        cache.put(jobCategory, h);

        return h;
    }

    // 调度"等待队列"中的各个子作业
    private void dispatchSubjobsInWaitingQueue() {
        final HashMap<String, NodeListHolder> cache = new HashMap<>();
        Iterator<SubjobEntry> it = this.waitingQueue.iterator();
        while (it.hasNext()) {
            SubjobEntry subjob = it.next();
            final SubjobControlBlockDO scb = subjob.getSubjobCB();
            final String jobId = scb.getJobId();

            if (scb.getIsReduce()) {
                // 这是个"扫尾"子作业，得判断所属作业中的其它常规作业是否已经执行完毕
                final JobEntry job = this.pendingJobs.get(jobId);
                assert job != null;
                if (job != null && !job.canReduce()) {
                    continue;
                }
            }

            String cate = subjob.getSubjobCB().getJobCategory();
            NodeListHolder h = this.getNodeListHolder(cache, cate);
            if (h == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("调度子作业失败: 当前没有可用的作业处理节点, jobCategory={}", cate);
                }
                continue;
            }
            if (!h.hasAvailableNode) {
                // 之前调度上一个同类别的subjob时已经发现没有可用的node了
                continue;
            }

            final int nodeN = h.nodeList.size();
            int i = h.nodeIndex;
            h.hasAvailableNode = false;
            while (i < nodeN) {
                final WorkerNode node = h.nodeList.get(i++);
                if (!node.isAlive()) {
                    continue;
                }
                if (node.isOverloaded(cate)) {
                    // 该节点目前太忙
                    continue;
                }
                h.hasAvailableNode = true;

                // 将子作业分派到node节点上去
                boolean sendOk = sendSubjobDispatchReqToNode(subjob, node);
                if (sendOk) {
                    // 将子作业从"等待队列"移动到"发送队列"
                    it.remove();
                    this.sendingQueue.put(subjob.getKey(), subjob);

                    // 更新内存中维护的node工作负载等状态
                    final int subjobSeqNo = scb.getSubjobSeqNo();
                    node.afterSubjobDispatchReqSent(jobId, subjobSeqNo);

                    // 更新子作业的eventlist
                    long now = System.currentTimeMillis();
                    SubjobDispatchReqSentEvent event = new SubjobDispatchReqSentEvent(now, node.getNodeId());
                    subjob.processEvent(event);
                }
                break;
            }
            h.nodeIndex = (i == nodeN) ? 0 : i;
        }
    }

    // 发送给定的"子作业分派请求"给指定的处理节点。
    private boolean sendSubjobDispatchReqToNode(final SubjobEntry subjob, final WorkerNode node) {
        final SubjobControlBlockDO scb = subjob.getSubjobCB();
        final String forwardToURL = node.getForwardToURL();
        try {
            final byte[] body = subjob.getBodyBytes();
            this.helper.sendSubjobReq(forwardToURL, scb, body);

            if (LOG.isInfoEnabled()) {
                LOG.info("sendSubjobDispatchReq[jobId={}, seq={}/{}, jobCategory={}, priority={}, retryTimes={}] to [{}] OK.",
                        scb.getJobId(),
                        scb.getSubjobSeqNo(),
                        scb.getSubjobCount(),
                        scb.getJobCategory(),
                        scb.getJobPriority(),
                        subjob.getRetryTimes(),
                        node.getNodeId());
            }
            return true;
        } catch (Throwable ex) {
            if (LOG.isErrorEnabled()) {
                LOG.error("sendSubjobDispatchReq[jobId={}, seq={}/{}, jobCategory={}, priority={}, retryTimes={}] to [{}] FAILED: {}",
                        scb.getJobId(),
                        scb.getSubjobSeqNo(),
                        scb.getSubjobCount(),
                        scb.getJobCategory(),
                        scb.getJobPriority(),
                        subjob.getRetryTimes(),
                        node.getNodeId(),
                        ex.getMessage());
            }
            return false;
        }
    }

    // "处理节点收到了子作业"
    public void handleSubjobDispatchAck(SubjobDispatchAckDO pdo) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("handleSubjobDispatchAck(), nodeId={}, subjob={}/{}/{}",
              pdo.getNodeId(),
              pdo.getJobCategory(),
              pdo.getJobId(),
              pdo.getSubjobSeqNo());
        }
        SubjobDispatchAckCommand cmd = new SubjobDispatchAckCommand(pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_SubjobDispatchAck(Object command) {
        SubjobDispatchAckCommand cmd = (SubjobDispatchAckCommand)command;
        final SubjobDispatchAckDO pdo = cmd.ack;
        final String jobId = pdo.getJobId();
        final int subjobSeqNo = pdo.getSubjobSeqNo();
        final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
        SubjobEntry subjob = this.sendingQueue.get(subjobKey);
        if (subjob == null) {
            return;
        }

        // 移动到"已分派队列"
        this.sendingQueue.remove(subjobKey);
        this.dispatchedQueue.put(subjobKey, subjob);

        // 更新子作业的eventlist
        final long now = System.currentTimeMillis();
        final String nodeId = pdo.getNodeId();
        SubjobDispatchAckReceivedEvent event = new SubjobDispatchAckReceivedEvent(now, nodeId);
        subjob.processEvent(event);

        // 更新内存中维护的node工作负载等状态
        WorkerNode node = this.nodeManager.getNodeById(nodeId);
        if (node != null) {
            node.afterSubjobDispatchAckReceived(jobId, subjobSeqNo);
        }
    }

    public void handleNodeLogonReq(String nodeBaseURL, LogonReqDO pdo) {
        NodeLogonReqCommand cmd = new NodeLogonReqCommand(nodeBaseURL, pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_NodeLogonReq(Object command) {
        NodeLogonReqCommand cmd = (NodeLogonReqCommand)command;
        this.nodeManager.handleNodeLogonReq(cmd.nodeBaseURL, cmd.pdo);
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE); //触发新一轮调度
    }

    public void handleNodeLogoffReq(LogoffDO pdo) {
        NodeLogoffReqCommand cmd = new NodeLogoffReqCommand(pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_NodeLogoffReq(Object command) {
        NodeLogoffReqCommand cmd = (NodeLogoffReqCommand)command;
        this.nodeManager.handleNodeLogoffReq(cmd.pdo);
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE); //触发新一轮调度: 将之前分配给该节点在的子作业进行转移
    }

    public void handleNodeHeartbeatReq(String nodeBaseURL, HeartbeatReportReqDO pdo) {
        NodeHeartbeatReqCommand cmd = new NodeHeartbeatReqCommand(nodeBaseURL, pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_NodeHeartbeatReq(Object command) {
        NodeHeartbeatReqCommand cmd = (NodeHeartbeatReqCommand)command;
        this.nodeManager.handleNodeHeartbeatReq(cmd.nodeBaseURL, cmd.pdo);
    }

    public void handleNodeStatusReport(SubjobStatusReportDO pdo) {
        SubjobStatusReportCommand cmd = new SubjobStatusReportCommand(pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processJobCmd_SubjobStatusReport(Object command) {
        SubjobStatusReportCommand cmd = (SubjobStatusReportCommand)command;
        // 从"已分派队列"中找到子作业对象
        final SubjobStatusReportDO pdo = cmd.pdo;
        final String jobId = pdo.getJobId();
        final int subjobSeqNo = pdo.getSubjobSeqNo();
        final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
        final SubjobEntry subjob = this.dispatchedQueue.get(subjobKey);
        if (subjob == null) {
            if (pdo.getStatusDO().getPhase() == Phase.PHASE_DONE) {
                // 有可能是处理节点重复报告"子作业完成"
                ;
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("handleSubjobStatusReport(): 已分派队列中找不到subjob={}/{}/{}",
                        pdo.getJobCategory(),
                        pdo.getJobId(),
                        pdo.getSubjobSeqNo());
                }
            }
            return;
        }

        // 更新子作业的eventlist
        final SubjobStatusDO s = pdo.getStatusDO();
        final long now = System.currentTimeMillis();
        final int statusCode = s.getStatusCode();
        do {
            SubjobBaseEvent event;
            final String stepId = s.getStepId();
            if (stepId != null && stepId.length() > 0 && (s.getStepTag() & StepTag.BEGIN) != 0) {
                event = new SubjobStepBeginEvent(now, stepId);
                subjob.processEvent(event);
            }

            if (!s.getOptions().containsKey("onlyStep")) {
                event = new SubjobStatusReportEvent(now,
                    statusCode,
                    s.getStatusMessage(),
                    s.getCompletedWorkload(),
                    s.getPhase());
                subjob.processEvent(event);
            }

            if (stepId != null && stepId.length() > 0 && (s.getStepTag() & StepTag.END) != 0) {
                event = new SubjobStepEndEvent(now, stepId);
                subjob.processEvent(event);
            }
        }
        while (false);

        // 更新内存中维护的node工作负载等状态
        final String nodeId = pdo.getNodeId();
        WorkerNode node = this.nodeManager.getNodeById(nodeId);
        if (node != null) {
            boolean isDone = s.getPhase() == Phase.PHASE_DONE;
            node.afterSubjobStatusReportReceived(jobId, subjobSeqNo, isDone);
        }

        if (s.getPhase() != Phase.PHASE_DONE) {
            return;
        }

        // 从"已分派队列"中移除
        this.dispatchedQueue.remove(subjobKey);

        // 触发新一轮调度
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE);

        // 子作业执行完成, 且执行成功
        if (statusCode == 0) {
            subjob.setSuccessFinished();
            checkIfJobFinished(jobId);
            return;
        }

        //---------------------------------
        // 子作业执行完成, 但执行失败了
        //---------------------------------
        final String cate = subjob.getSubjobCB().getJobCategory();
        JobDispatcherFactory f = this.dispatcherCreator.getJobDispatcherFactory(cate);
        if (!f.isSubjobRetriable(cate) || subjob.getRetryTimes() >= this.maxSubjobRetryTimes) {
            // 该类子作业不支持重试，或重试次数超限，标记成"失败完成"，子作业处理结束
            subjob.setFailureFinished();
            checkIfJobFinished(jobId);
            return;
        }

        // 看"处理节点"上是否设置了"可重做"标记, 若设置了，说明这是个"可恢复错误"
        final boolean isRetriable = s.getOptions().containsKey("doRetry");
        if (isRetriable) {
            // 加入"重做队列"
            subjob.setWhenLastEnterRetryQueue(now);
            this.retryQueue.put(subjobKey, subjob);
        } else {
            subjob.setFailureFinished();
            checkIfJobFinished(jobId);
        }
    }

    // 检查作业是否已执行完成
    private void checkIfJobFinished(String jobId) {
        JobEntry job = this.pendingJobs.get(jobId);
        assert job != null;
        if (job == null) {
            return;
        }

        JobState state = JobEntry.evaluateJobState(job);
        if (state == JobState.FINISHED_FAILED || state == JobState.FINISHED_SUCCESS) {
            if (state == JobState.FINISHED_SUCCESS) {
                job.setSuccessFinished();
            } else {
                job.setFailureFinished();
            }
            this.pendingJobs.remove(jobId);
            this.finishedJobs.put(jobId, job);
        }
    }

    /**
     * 获得给定名称的参数值。
     * @param name
     * @param defaultVal
     * @return
     */
    @Override
    public String getProperty(String name, String defaultVal) {
        return this.helper.getProperty(name, defaultVal);
    }

    private ArrayList<WorkerNode> getAliveNodeList(String jobCategory) {
        return this.nodeManager.getAliveNodeList(jobCategory);
    }

    private Method getProcessCmdMethod(Object cmd, String methodNamePrefix, String cmdNameSuffixToStrip) {
        // 由类名求得方法名
        // NodeLogoffReqCommand --> processJobCmd_NodeLogoffReq"
        String className = getCmdClassName(cmd);
        String methodName = methodNamePrefix + className;
        if (methodName.endsWith(cmdNameSuffixToStrip)) {
            int end = methodName.length() - cmdNameSuffixToStrip.length();
            methodName = methodName.substring(0, end);
        }
        try {
            return getClass().getDeclaredMethod(methodName, Object.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            LOG.error("根据命令的class确定对应的处理方法失败", ex);
            return null;
        }
    }

    private static String getCmdClassName(Object cmd) {
        String className = cmd.getClass().getName();
        int pos = className.lastIndexOf('$');
        if (pos < 0) {
            pos = className.lastIndexOf('.');
        }
        if (pos > 0) {
            className = className.substring(pos + 1);
        }
        return className;
    }

    ////////////////////////////////////////////////////////////////
    //
    // 处理REST命令, 这些命令通常来自前端的交互式请求，比如浏览器。目前分为两大类:
    // (1) Job管理相关；
    // (2) Mission管理相关(Mission = Job + 业务信息扩展，为兼容之前浏览器已经写成的代码，还保留mission的概念)。
    //
    ////////////////////////////////////////////////////////////////

    // 处理REST请求的命令队列
    private final LinkedBlockingQueue<Object> restCmdQueue = new LinkedBlockingQueue<>();

    private void processRestCommands_i() {
        for (;;) {
            Object cmd = this.restCmdQueue.poll();
            if (cmd == null) {
                return;
            }
            Method method = getProcessCmdMethod(cmd, "processRestCmd_", "RestCmd");
            if (method != null) {
                try {
                    method.invoke(this, cmd);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOG.error("命令处理方法执行失败", ex);
                }
            }
        }
    }

    //
    //------------------------------------------
    // (1) Job管理相关
    //------------------------------------------
    //
    // XXX: 约定命令类名必须形如"xxxRestCmd"，对应的处理方法必须形如"processRestCmd_xxx"，下同
    private static class GetJobStatusRestCmd {
        final String jobId;
        final JobStatusDO jobStatus;
        final StringHolder statusMessage;
        final CountDownLatch latch = new CountDownLatch(1);
        int statusCode = 0;
        GetJobStatusRestCmd(String jobId, JobStatusDO jobStatus, StringHolder statusMessage) {
            this.jobId = jobId;
            this.jobStatus = jobStatus;
            this.statusMessage = statusMessage;
        }
    }

    // 返回状态码, 0表示成功, 其它表示错误.
    public int getJobStatus(String jobId, JobStatusDO jobStatus, StringHolder statusMessage) {
        GetJobStatusRestCmd cmd = new GetJobStatusRestCmd(jobId, jobStatus, statusMessage);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            return -1;
        }
        return cmd.statusCode;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetJobStatus(Object command) {
        GetJobStatusRestCmd cmd = (GetJobStatusRestCmd)command;
        try {
            JobEntry job = getJobById(cmd.jobId);
            if (job == null) {
                if (cmd.statusMessage != null) {
                    cmd.statusMessage.value = String.format("找不到jobId=%s的作业", cmd.jobId);
                }
                cmd.statusCode = -1;
                return;
            }
            job.getStatus(cmd.jobStatus);
        }
        finally {
            cmd.latch.countDown();
        }
    }

    private static class GetAllJobStatusRestCmd {
        final JobStatusListDO jobStatusList;
        final CountDownLatch latch = new CountDownLatch(1);
        GetAllJobStatusRestCmd(JobStatusListDO jobStatusList) {
            this.jobStatusList = jobStatusList;
        }
    }

    // 返回状态码, 0表示成功, 其它表示错误.
    public int getAllJobStatus(JobStatusListDO jobStatusList) {
        GetAllJobStatusRestCmd cmd = new GetAllJobStatusRestCmd(jobStatusList);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            return -1;
        }
        return 0;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetAllJobStatus(Object command) {
        GetAllJobStatusRestCmd cmd = (GetAllJobStatusRestCmd)command;
        try {
            this.pendingJobs.values().forEach((job) -> {
                JobStatusDO jobStatus = new JobStatusDO();
                job.getStatus(jobStatus);
                jobStatus.getOptions().put("_jobTag", "pendingJob");
                cmd.jobStatusList.add(jobStatus);
            });
            this.finishedJobs.values().forEach((job) -> {
                JobStatusDO jobStatus = new JobStatusDO();
                job.getStatus(jobStatus);
                jobStatus.getOptions().put("_jobTag", "finishedJob");
                cmd.jobStatusList.add(jobStatus);
            });

            // 按时间倒序排列，越新的越排在前边
            cmd.jobStatusList.sort((s1, s2) -> {
                int diff = (int)(s2.getJobEpoch() - s1.getJobEpoch());
                return diff == 0 ? 0 : (diff > 0 ? 1 : -1);
            });
        }
        finally {
            cmd.latch.countDown();
        }
    }

    private static class GetAllProcessNodeStatusRestCmd {
        final SubjobProcessNodeListDO nodeList;
        final CountDownLatch latch = new CountDownLatch(1);
        GetAllProcessNodeStatusRestCmd(SubjobProcessNodeListDO nodeList) {
            this.nodeList = nodeList;
        }
    }

    // 返回状态码, 0表示成功, 其它表示错误.
    public int getAllProcessNodeStatus(SubjobProcessNodeListDO nodeList) {
        GetAllProcessNodeStatusRestCmd cmd = new GetAllProcessNodeStatusRestCmd(nodeList);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            return -1;
        }
        return 0;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetAllProcessNodeStatus(Object command) {
        GetAllProcessNodeStatusRestCmd cmd = (GetAllProcessNodeStatusRestCmd)command;
        try {
            for (WorkerNode e : this.nodeManager.getAllNodeList()) {
                WorkerNode node = (WorkerNode)e;
                SubjobProcessNodeDO nodeDO = new SubjobProcessNodeDO();
                node.getStatus(nodeDO);
                cmd.nodeList.add(nodeDO);
            }
        } finally {
            cmd.latch.countDown();
        }
    }

    //
    //------------------------------------------
    // (2) Mission管理相关
    //------------------------------------------
    //
    private static class GetAllJobsAsMissionRestCmd {
        final CountDownLatch latch = new CountDownLatch(1);
        final AdminMissionListDO result = new AdminMissionListDO();
    }

    /**
     * 获得所有作业的数据，作为mission返回。
     * @return
     */
    public AdminMissionListDO getAllJobsAsMission() {
        GetAllJobsAsMissionRestCmd cmd = new GetAllJobsAsMissionRestCmd();
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetAllJobsAsMission(Object command) {
        GetAllJobsAsMissionRestCmd cmd = (GetAllJobsAsMissionRestCmd)command;
        try {
            this.pendingJobs.values().forEach((job) -> {
                AdminMissionDO pdo = job.asMission();
                pdo.getOptions().put("_missionTag", "pendingJob");
                cmd.result.add(pdo);
            });
            this.finishedJobs.values().forEach((job) -> {
                AdminMissionDO pdo = job.asMission();
                pdo.getOptions().put("_missionTag", "finishedJob");
                cmd.result.add(pdo);
            });
            // 按时间倒序排列，越新的越排在前边
            cmd.result.sort((s1, s2) -> {
                int diff = (int)(s2.getMissionEpoch() - s1.getMissionEpoch());
                return diff == 0 ? 0 : (diff > 0 ? 1 : -1);
            });
        } finally {
            cmd.latch.countDown();
        }
    }

    private static class GetJobAsMissionRestCmd {
        final String jobId;
        final CountDownLatch latch = new CountDownLatch(1);
        AdminMissionDO result = null;
        GetJobAsMissionRestCmd(String jobId) {
            this.jobId = jobId;
        }
    }

    public AdminMissionDO getJobAsMission(String jobId) {
        GetJobAsMissionRestCmd cmd = new GetJobAsMissionRestCmd(jobId);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetJobAsMission(Object command) {
        GetJobAsMissionRestCmd cmd = (GetJobAsMissionRestCmd)command;
        try {
            JobEntry job = getJobById(cmd.jobId);
            if (job != null) {
                cmd.result = job.asMission();
            }
        } finally {
            cmd.latch.countDown();
        }
    }

    private static class GetJobsAsMissionByFilterRestCmd {
        final MissionQueryFilter filter;
        final CountDownLatch latch = new CountDownLatch(1);
        final AdminMissionListDO result = new AdminMissionListDO();
        GetJobsAsMissionByFilterRestCmd(MissionQueryFilter filter) {
            this.filter = filter;
        }
    }

    public AdminMissionListDO getJobsAsMissionByFilter(MissionQueryFilter filter) {
        GetJobsAsMissionByFilterRestCmd cmd = new GetJobsAsMissionByFilterRestCmd(filter);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetJobsAsMissionByFilter(Object command) {
        GetJobsAsMissionByFilterRestCmd cmd = (GetJobsAsMissionByFilterRestCmd)command;
        try {
            final Date from = cmd.filter.getFrom();
            final Date to = cmd.filter.getTo();
            final long fromBornTime = from == null ? 0 : from.toInstant().toEpochMilli();
            final long toBornTime = to == null ? 0 : to.toInstant().toEpochMilli();
            final String host = cmd.filter.getHost();

            this.pendingJobs.values().forEach((job) -> {
                if (job.filter(fromBornTime, toBornTime, host)) {
                    AdminMissionDO pdo = job.asMission();
                    pdo.getOptions().put("_missionTag", "pendingJob");
                    cmd.result.add(pdo);
                }
            });
            this.finishedJobs.values().forEach((job) -> {
                if (job.filter(fromBornTime, toBornTime, host)) {
                    AdminMissionDO pdo = job.asMission();
                    pdo.getOptions().put("_missionTag", "finishedJob");
                    cmd.result.add(pdo);
                }
            });
            // 按时间倒序排列，越新的越排在前边
            cmd.result.sort((s1, s2) -> {
                int diff = (int)(s2.getMissionEpoch() - s1.getMissionEpoch());
                return diff == 0 ? 0 : (diff > 0 ? 1 : -1);
            });

        } finally {
            cmd.latch.countDown();
        }
    }

    private static class GetSubjobAsMissionItemRestCmd {
        final String misssioItemId;
        final CountDownLatch latch = new CountDownLatch(1);
        AdminMissionItemDO result = null;
        GetSubjobAsMissionItemRestCmd(String misssioItemId) {
            this.misssioItemId = misssioItemId;
        }
    }

    public AdminMissionItemDO getSubjobAsMissionItem(String misssioItemId) {
        GetSubjobAsMissionItemRestCmd cmd = new GetSubjobAsMissionItemRestCmd(misssioItemId);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetSubjobAsMissionItem(Object command) {
        GetSubjobAsMissionItemRestCmd cmd = (GetSubjobAsMissionItemRestCmd)command;
        try {
            SubjobEntry subjob = getSubjobByMissionItemId(cmd.misssioItemId);
            if (subjob != null) {
                cmd.result = subjob.asMissionItem();
            }
        } finally {
            cmd.latch.countDown();
        }
    }

    private JobEntry getJobById(String jobId) {
        JobEntry job = this.pendingJobs.get(jobId);
        if (job == null) {
            job = this.finishedJobs.get(jobId);
        }
        return job;
    }

    // missioItemId构成为"jobId|subjobSeqNo"
    private SubjobEntry getSubjobByMissionItemId(String missioItemId) {
        StringHolder jobId = new StringHolder();
        IntHolder subjobSeqNo = new IntHolder();
        if (!Helper.parseMissionItemId(missioItemId, jobId, subjobSeqNo)) {
            return null;
        }

        JobEntry job = getJobById(jobId.value);
        if (job == null) {
            return null;
        }
        return job.getSubjobBySeqNo(subjobSeqNo.value);
    }

    //
    //------------------------------------------
    // (3) Dump & Debug 相关
    //------------------------------------------
    //

    private static class GetStateSummaryRestCmd {
        final SchedulerStateSummaryDO result = new SchedulerStateSummaryDO();
        final CountDownLatch latch = new CountDownLatch(1);
    }

    // 获得内部状态
    public SchedulerStateSummaryDO getStateSummary() {
        GetStateSummaryRestCmd cmd = new GetStateSummaryRestCmd();
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_GetStateSummary(Object command) {
        GetStateSummaryRestCmd cmd = (GetStateSummaryRestCmd)command;
        final SchedulerStateSummaryDO pdo = cmd.result;
        try {
            pdo.setTotalPendingJobCount(this.pendingJobs.size());

            int failureFinishedJobCount = 0;
            int sucessFinishedJobCount = 0;
            for (JobEntry job : this.finishedJobs.values()) {
                if (job.getState() == JobState.FINISHED_FAILED) {
                    failureFinishedJobCount += 1;
                } else {
                    sucessFinishedJobCount += 1;
                }
            }
            pdo.setTotalFinishedJobCount(this.finishedJobs.size());
            pdo.setFailureFinishedJobCount(failureFinishedJobCount);
            pdo.setSuccessFinishedJobCount(sucessFinishedJobCount);

            // 各调度用队列
            pdo.setWaitingQueueLength(this.waitingQueue.size());
            pdo.setSendingQueueLength(this.sendingQueue.size());
            pdo.setDispatchedQueueLength(this.dispatchedQueue.size());
            pdo.setRetryQueueLength(this.retryQueue.size());

            // 处理节点状态
            for (WorkerNode e : this.nodeManager.getAllNodeList()) {
                WorkerNode node = (WorkerNode)e;
                SubjobProcessNodeDO nodeDO = new SubjobProcessNodeDO();
                node.getStatus(nodeDO);
                pdo.getProcessNodes().add(nodeDO);
            }

            // 重要参数
            pdo.getOptions().put("jobDataTopDir", this.jobPersister.getJobDbTopDir().getAbsolutePath());
            pdo.getOptions().put("finishedJobKeepDays", this.finishedJobKeepDays + "");
            pdo.getOptions().put("maxSubjobRetryTimes", this.maxSubjobRetryTimes + "");
            pdo.getOptions().put("retrySubjobIntervalMillis", this.retrySubjobIntervalMillis + "");

        } finally {
            cmd.latch.countDown();
        }
    }

    private static class DumpAllSubjobsStateByJobIdRestCmd {
        final SubjobDumpStateListDO result = new SubjobDumpStateListDO();
        final CountDownLatch latch = new CountDownLatch(1);
        final String jobId;
        DumpAllSubjobsStateByJobIdRestCmd(String jobId) {
            this.jobId = jobId;
        }
    }

    public SubjobDumpStateListDO dumpAllSubjobsStateByJobId(String jobId) {
        DumpAllSubjobsStateByJobIdRestCmd cmd = new DumpAllSubjobsStateByJobIdRestCmd(jobId);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
        return cmd.result;
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_DumpAllSubjobsStateByJobId(Object command) {
        DumpAllSubjobsStateByJobIdRestCmd cmd = (DumpAllSubjobsStateByJobIdRestCmd)command;
        final SubjobDumpStateListDO list = cmd.result;
        try {
            JobEntry job = getJobById(cmd.jobId);
            if (job != null) {
                job.dumpAllSubjobsState(cmd.result);
            }
        } finally {
            cmd.latch.countDown();
        }
    }

    private static class DumpSubjobEventLogsRestCmd {
        final CountDownLatch latch = new CountDownLatch(1);
        final String jobId;
        final int subjobSeqNo;
        final ByteArrayOutputStream os;
        DumpSubjobEventLogsRestCmd(String jobId, int subjobSeqNo, ByteArrayOutputStream os) {
            this.jobId = jobId;
            this.subjobSeqNo = subjobSeqNo;
            this.os = os;
        }
    }

    public void dumpSubjobEventLogs(String jobId, int subjobSeqNo, ByteArrayOutputStream out) {
        DumpSubjobEventLogsRestCmd cmd = new DumpSubjobEventLogsRestCmd(jobId, subjobSeqNo, out);
        this.restCmdQueue.offer(cmd);
        this.jobCmdQueue.offer(WakeupCommand.INSTANCE);
        try {
            cmd.latch.await();
        } catch (InterruptedException e) {
            ;
        }
    }

    // 由reflect机制调用
    @SuppressWarnings("unused")
    private void processRestCmd_DumpSubjobEventLogs(Object command) {
        DumpSubjobEventLogsRestCmd cmd = (DumpSubjobEventLogsRestCmd)command;
        try {
            JobEntry job = getJobById(cmd.jobId);
            if (job != null) {
                job.dumpSubjobEventLogs(cmd.subjobSeqNo, cmd.os);
            }
        } finally {
            cmd.latch.countDown();
        }
    }

}
