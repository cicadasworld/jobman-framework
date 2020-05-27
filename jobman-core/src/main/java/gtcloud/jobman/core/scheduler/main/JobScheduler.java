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

    // �����߳��ڸö����ϵȴ�����
    private final LinkedBlockingQueue<Object> jobCmdQueue = new LinkedBlockingQueue<>();

    private final JobSchedulerHelper helper;

    // �������ҵ��
    private final ConcurrentHashMap<String, JobEntry> finishedJobs = new ConcurrentHashMap<>();

    // δ�����ҵ��
    private final ConcurrentHashMap<String, JobEntry> pendingJobs = new ConcurrentHashMap<>();

    // ��ҵ�־û���
    private final FlatJobPersister jobPersister;

    private final long finishedJobKeepDays;

    private long whenLastPurgeStaleFinishedJobs = System.currentTimeMillis();

    // ����ĳ������ҵ���κ�ʱ����ֻ��λ������4������֮һ��
    private final TreeSet<SubjobEntry>               waitingQueue;    //�ȴ����У��ȴ������ɣ�
    private final LinkedHashMap<String, SubjobEntry> sendingQueue;    //���Ͷ��У��ö����е�����ҵ�Ѿ������͸���ĳ������ڵ㣬����δ�յ�ack
    private final LinkedHashMap<String, SubjobEntry> dispatchedQueue; //�ѷ��ɶ��У��ö����е�����ҵ�Ѿ������͸���ĳ������ڵ㣬�����յ�ack������δִ����
    private final LinkedHashMap<String, SubjobEntry> retryQueue;      //���Զ��У��ö����е�����ҵ֮ǰ����ʧ���ˣ������ȴ�����

    // ����ҵʧ�ܺ����������Զ��ٴ�
    private final int maxSubjobRetryTimes;

    // ����ҵʧ�ܺ���Եȴ���ʱ����
    private final int retrySubjobIntervalMillis;

    // "����ҵ����"����
    // XXX: ������ʹ���˷��������ִ�����Լ������������������"xxxCommand"����Ӧ�Ĵ�������������"processJobCmd_xxx"����ͬ;
    // �� NewJobReceivedCommand �Ĵ�������Ϊ processJobCmd_NewJobReceived
    private static class NewJobReceivedCommand {
        JobControlBlockDO jobCB;
        ByteSeq           jobBody;
        JobDispatcher     dispatcher;
    }

    // "�˳�"����
    private static class QuitCommand {
        final CountDownLatch latch = new CountDownLatch(1);
    }

    // "��������"����
    private static class DispatchCommand {
        static final DispatchCommand INSTANCE = new DispatchCommand();
    }

    // "����"����
    private static class WakeupCommand {
        static final WakeupCommand INSTANCE = new WakeupCommand();
    }

    // "����ҵ����Ӧ���Ѿ�����"
    private static class SubjobDispatchAckCommand {
        final SubjobDispatchAckDO ack;
        SubjobDispatchAckCommand(SubjobDispatchAckDO ack) {
            this.ack = ack;
        }
    }

    // "�ڵ��¼"
    private static class NodeLogonReqCommand {
        final String nodeBaseURL;
        final LogonReqDO pdo;
        NodeLogonReqCommand(String nodeBaseURL, LogonReqDO pdo) {
            this.nodeBaseURL = nodeBaseURL;
            this.pdo = pdo;
        }
    }

    // "�ڵ�ǳ�"
    private static class NodeLogoffReqCommand {
        final LogoffDO pdo;
        NodeLogoffReqCommand(LogoffDO pdo) {
            this.pdo = pdo;
        }
    }

    // "�ڵ���������"
    private static class NodeHeartbeatReqCommand {
        final String nodeBaseURL;
        final HeartbeatReportReqDO pdo;
        NodeHeartbeatReqCommand(String nodeBaseURL, HeartbeatReportReqDO pdo) {
            this.nodeBaseURL = nodeBaseURL;
            this.pdo = pdo;
        }
    }

    // "����ҵ״̬����"
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

        // �Ѿ�ִ����ɵ���ҵ��"finishedĿ¼"��������? ֮����ƶ����鵵Ŀ¼
        String sdays = helper.getProperty("gtcloud.scheduler.finishedJobKeepDays", "7");
        this.finishedJobKeepDays = Integer.parseInt(sdays);

        String paramName = "gtcloud.scheduler.jobDataTopDir";
        String jobDataTopDir = helper.getProperty(paramName, null);
        if (jobDataTopDir == null) {
            throw new Exception("ȱ�����ò���: " + paramName);
        }
        this.jobPersister = new FlatJobPersister(jobDataTopDir, finishedJobKeepDays);

        this.maxSubjobRetryTimes = Integer.parseInt(helper.getProperty("gtcloud.scheduler.jobRetryTimes", "5"));
        this.retrySubjobIntervalMillis = Integer.parseInt(helper.getProperty("gtcloud.scheduler.retryJobWaitIntervalMillis", "5000"));
    }

    public void run(String[] args) throws Exception {
        // �ӳ־ô洢�м���֮ǰ�������µ���ҵ�б�
        this.jobPersister.readJobs(pendingJobs, finishedJobs);

        // ������δ��ɵ�����ҵ��ӣ��ȴ���������
        for (JobEntry job : pendingJobs.values()) {
            for (SubjobEntry subjob : job.getSubjobs()) {
                if (subjob.getState() == JobState.PENDING) {
                    this.waitingQueue.add(subjob);
                }
            }
        }

        // ����������ҵ��Ƭ��
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

        // ���������߳�
        Thread t = new Thread(() -> {
            runCommandLoop();
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        // ֹͣ�����߳�
        LOG.info("stop() received.");
        QuitCommand cmd = new QuitCommand();
        this.jobCmdQueue.offer(cmd);
        try {
            cmd.latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ֹͣ������ҵ������
        for (JobDispatcher d : this.dispatcherLookup.values()) {
            d.onStop();
        }

        // ������
        this.dispatcherCreator.fini();
    }

    private void runCommandLoop() {
        // ɨ����������
        final String t = this.helper.getProperty("gtcloud.scheduler.jobScanIntervalMillis", null);
        final long intervalMillis = t != null ? Long.parseLong(t) : 30*1000;

        long whenLastRunPeriodicalWork = 0;

        LOG.info("runCommandLoop() begin, interval={}(ms)", intervalMillis);
        for (long round=0; ; round++) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("runDispatchLoop(), round=" + round);
            }
            try {
                // �ȴ�����ʽ����
                processRestCommands_i();

                // �ٴ�����ҵ�����������
                boolean goingon = processJobCommands_i(round, intervalMillis);
                if (!goingon) {
                    break;
                }

                // ����������
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

    // ���յ��˳������false, ���򷵻�true
    private boolean processJobCommands_i(long round, long intervalMillis) {
        // ɨ����������
        if (round == 0) {
            intervalMillis = 5 * 1000;
        }

        Object cmd;
        try {
            cmd = this.jobCmdQueue.poll(intervalMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }

        if (cmd == null) {  //��ʱ
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

        // �������ִ����������
        Method method = getProcessCmdMethod(cmd, "processJobCmd_", "Command");
        if (method != null) {
            try {
                method.invoke(this, cmd);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOG.error("�������ִ��ʧ��", ex);
            }
        }

        return true;
    }

    private void runPeriodicalWork() {
        this.jobPersister.doPeriodicalWork();
        tryPurgeStaleFinishedJobs();
        processJobCmd_Dispatch();
    }

    // ��"�������ҵ����"���Ƴ���Щ����ʱ�䳬�޵���ҵ���ͷ��ڴ�ռ�
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

    // ��ҵ�ύ�ͻ����ύ��ҵ.
    // ����״̬��, 0��ʾ�ɹ�, ������ʾ����.
    public int handleJobScheduleRequest(JobControlBlockDO jobCB,
                                        ByteSeq jobBody,
                                        StringHolder statusMessage) {
        // ���jodId�Ƿ��ظ�
        final String jobId = jobCB.getJobId();
        if (this.pendingJobs.containsKey(jobId) || this.finishedJobs.containsKey(jobId)) {
            String emsg = statusMessage.value = "jobId�ظ�, jobId=" + jobId;
            LOG.error(emsg);
            return -1;
        }

        // ���֧�ָ�����ҵ�������߽ڵ��б�
        final String jobCategory = jobCB.getJobCategory();
        if (jobCB.getOptions().get("aliveNodeMustBePresent") != null) {
            ArrayList<WorkerNode> nodeList = this.nodeManager.getAliveNodeList(jobCategory);
            if (nodeList == null || nodeList.size() == 0) {
                String emsg = statusMessage.value = "��ǰû�п��õ���ҵ����ڵ�";
                LOG.error(emsg);
                return -1;
            }
        }

        // �����ҵ������
        final JobDispatcher dispatcher = this.dispatcherLookup.get(jobCategory);
        if (dispatcher == null) {
            String emsg = statusMessage.value = "�Ҳ�����ҵ������, ��ҵ���=" + jobCategory;
            LOG.error(emsg);
            return -1;
        }

        // ����������������
        NewJobReceivedCommand cmd = new NewJobReceivedCommand();
        cmd.jobCB = jobCB;
        cmd.jobBody = jobBody;
        cmd.dispatcher = dispatcher;
        this.jobCmdQueue.offer(cmd);

        return 0;
    }

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processJobCmd_NewJobReceived(Object command) {
        NewJobReceivedCommand cmd = (NewJobReceivedCommand)command;
        try {
            // ������ҵ���
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

            // д��־ô洢
            long now = System.currentTimeMillis();
            for (SubjobEntry subjob : subjobs) {
                subjob.setBornTime(now);
            }
            JobEntry job = new JobEntry(this.jobPersister, cmd.jobCB, subjobs, now);
            this.jobPersister.insertNewJob(job);

            // ���뵽�ڴ��б���
            String jobId = job.getControlBlock().getJobId();
            this.pendingJobs.put(jobId, job);

            // ����ҵ���, �ȴ�����
            for (SubjobEntry subjob : subjobs) {
                this.waitingQueue.add(subjob);
            }

            // ������һ�ֵ���
            this.jobCmdQueue.offer(DispatchCommand.INSTANCE);

        } catch (Throwable ex) {
            LOG.error("����NewJobReceivedCommand��������", ex);
        }
    }

    private void processJobCmd_Dispatch() {
        // ����"���Ͷ���"������ҵ��"�ȴ�����"��
        // (1) Ԥ��ʱ���ڻ�û���յ�"DispatchAck"
        // (2) ����ڵ������
        if (!this.sendingQueue.isEmpty()) {
            recycleSubjobsInSendingQueue();
        }

        // ����"�ѷ��ɶ���"�е�����ҵ��������ڵ�����ˣ�����뵽"��������"��
        if (!this.dispatchedQueue.isEmpty()) {
            recycleSubjobsInDispatchedQueue();
        }

        // ����"���Զ���"�е�����ҵ����������ʱ���ѵ��������¼��뵽"�ȴ�����"��
        if (!this.retryQueue.isEmpty()) {
            recycleSubjobsInRetryQueue();
        }

        // ����"�ȴ�����"�еĸ�������ҵ
        if (!this.waitingQueue.isEmpty()) {
            if (this.nodeManager.getAliveNodeCount() > 0) {
                dispatchSubjobsInWaitingQueue();
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("��ǰû�п��õ���ҵ����ڵ�, �޷���������ҵ��");
            }
        }
    }

    // ����"���Ͷ���"������ҵ��"�ȴ�����"��
    // (1) Ԥ��ʱ���ڻ�û���յ�"DispatchAck"
    // (2) ����ڵ������
    private void recycleSubjobsInSendingQueue() {
        //
        // ����ҵ���͸�����ڵ��, ���ڸò���ָ���ĺ��������յ�����ڵ��Ӧ������Ϊ���ͳɹ�, ������Ϊ����ʧ��
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
                this.waitingQueue.add(subjob); //�ȴ���һ����������
                it.remove();
            }

            // �����ڴ���ά����node�������ص�״̬
            if (isExpired && node != null) {
                String jobId = subjob.getSubjobCB().getJobId();
                int subjobSeqNo = subjob.getSubjobCB().getSubjobSeqNo();
                node.afterSubjobDispatchReqExpired(jobId, subjobSeqNo);
            }
        }
    }

    // ����"�ѷ��ɶ���"�е�����ҵ��������ڵ�����ˣ�����뵽"��������"��
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

            // ��"�ѷ��ɶ���"���Ƴ�
            it.remove();

            // ����"��������"
            final long now = System.currentTimeMillis();
            subjob.setWhenLastEnterRetryQueue(now);
            this.retryQueue.put(e.getKey(), subjob);
        }
    }

    // ����"���Զ���"�е�����ҵ����������ʱ���ѵ��������¼��뵽"�ȴ�����"��
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
                this.waitingQueue.add(subjob); //�ȴ���һ����������
                it.remove();
            }
        }
    }

    private static class NodeListHolder {
        int nodeIndex = 0;
        ArrayList<WorkerNode> nodeList = null;
        boolean hasAvailableNode = true; //���һ���Ƿ��п��õĽڵ�
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

    // ����"�ȴ�����"�еĸ�������ҵ
    private void dispatchSubjobsInWaitingQueue() {
        final HashMap<String, NodeListHolder> cache = new HashMap<>();
        Iterator<SubjobEntry> it = this.waitingQueue.iterator();
        while (it.hasNext()) {
            SubjobEntry subjob = it.next();
            final SubjobControlBlockDO scb = subjob.getSubjobCB();
            final String jobId = scb.getJobId();

            if (scb.getIsReduce()) {
                // ���Ǹ�"ɨβ"����ҵ�����ж�������ҵ�е�����������ҵ�Ƿ��Ѿ�ִ�����
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
                    LOG.warn("��������ҵʧ��: ��ǰû�п��õ���ҵ����ڵ�, jobCategory={}", cate);
                }
                continue;
            }
            if (!h.hasAvailableNode) {
                // ֮ǰ������һ��ͬ����subjobʱ�Ѿ�����û�п��õ�node��
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
                    // �ýڵ�Ŀǰ̫æ
                    continue;
                }
                h.hasAvailableNode = true;

                // ������ҵ���ɵ�node�ڵ���ȥ
                boolean sendOk = sendSubjobDispatchReqToNode(subjob, node);
                if (sendOk) {
                    // ������ҵ��"�ȴ�����"�ƶ���"���Ͷ���"
                    it.remove();
                    this.sendingQueue.put(subjob.getKey(), subjob);

                    // �����ڴ���ά����node�������ص�״̬
                    final int subjobSeqNo = scb.getSubjobSeqNo();
                    node.afterSubjobDispatchReqSent(jobId, subjobSeqNo);

                    // ��������ҵ��eventlist
                    long now = System.currentTimeMillis();
                    SubjobDispatchReqSentEvent event = new SubjobDispatchReqSentEvent(now, node.getNodeId());
                    subjob.processEvent(event);
                }
                break;
            }
            h.nodeIndex = (i == nodeN) ? 0 : i;
        }
    }

    // ���͸�����"����ҵ��������"��ָ���Ĵ���ڵ㡣
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

    // "����ڵ��յ�������ҵ"
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

    // ��reflect���Ƶ���
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

        // �ƶ���"�ѷ��ɶ���"
        this.sendingQueue.remove(subjobKey);
        this.dispatchedQueue.put(subjobKey, subjob);

        // ��������ҵ��eventlist
        final long now = System.currentTimeMillis();
        final String nodeId = pdo.getNodeId();
        SubjobDispatchAckReceivedEvent event = new SubjobDispatchAckReceivedEvent(now, nodeId);
        subjob.processEvent(event);

        // �����ڴ���ά����node�������ص�״̬
        WorkerNode node = this.nodeManager.getNodeById(nodeId);
        if (node != null) {
            node.afterSubjobDispatchAckReceived(jobId, subjobSeqNo);
        }
    }

    public void handleNodeLogonReq(String nodeBaseURL, LogonReqDO pdo) {
        NodeLogonReqCommand cmd = new NodeLogonReqCommand(nodeBaseURL, pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processJobCmd_NodeLogonReq(Object command) {
        NodeLogonReqCommand cmd = (NodeLogonReqCommand)command;
        this.nodeManager.handleNodeLogonReq(cmd.nodeBaseURL, cmd.pdo);
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE); //������һ�ֵ���
    }

    public void handleNodeLogoffReq(LogoffDO pdo) {
        NodeLogoffReqCommand cmd = new NodeLogoffReqCommand(pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processJobCmd_NodeLogoffReq(Object command) {
        NodeLogoffReqCommand cmd = (NodeLogoffReqCommand)command;
        this.nodeManager.handleNodeLogoffReq(cmd.pdo);
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE); //������һ�ֵ���: ��֮ǰ������ýڵ��ڵ�����ҵ����ת��
    }

    public void handleNodeHeartbeatReq(String nodeBaseURL, HeartbeatReportReqDO pdo) {
        NodeHeartbeatReqCommand cmd = new NodeHeartbeatReqCommand(nodeBaseURL, pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processJobCmd_NodeHeartbeatReq(Object command) {
        NodeHeartbeatReqCommand cmd = (NodeHeartbeatReqCommand)command;
        this.nodeManager.handleNodeHeartbeatReq(cmd.nodeBaseURL, cmd.pdo);
    }

    public void handleNodeStatusReport(SubjobStatusReportDO pdo) {
        SubjobStatusReportCommand cmd = new SubjobStatusReportCommand(pdo);
        this.jobCmdQueue.offer(cmd);
    }

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processJobCmd_SubjobStatusReport(Object command) {
        SubjobStatusReportCommand cmd = (SubjobStatusReportCommand)command;
        // ��"�ѷ��ɶ���"���ҵ�����ҵ����
        final SubjobStatusReportDO pdo = cmd.pdo;
        final String jobId = pdo.getJobId();
        final int subjobSeqNo = pdo.getSubjobSeqNo();
        final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
        final SubjobEntry subjob = this.dispatchedQueue.get(subjobKey);
        if (subjob == null) {
            if (pdo.getStatusDO().getPhase() == Phase.PHASE_DONE) {
                // �п����Ǵ���ڵ��ظ�����"����ҵ���"
                ;
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("handleSubjobStatusReport(): �ѷ��ɶ������Ҳ���subjob={}/{}/{}",
                        pdo.getJobCategory(),
                        pdo.getJobId(),
                        pdo.getSubjobSeqNo());
                }
            }
            return;
        }

        // ��������ҵ��eventlist
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

        // �����ڴ���ά����node�������ص�״̬
        final String nodeId = pdo.getNodeId();
        WorkerNode node = this.nodeManager.getNodeById(nodeId);
        if (node != null) {
            boolean isDone = s.getPhase() == Phase.PHASE_DONE;
            node.afterSubjobStatusReportReceived(jobId, subjobSeqNo, isDone);
        }

        if (s.getPhase() != Phase.PHASE_DONE) {
            return;
        }

        // ��"�ѷ��ɶ���"���Ƴ�
        this.dispatchedQueue.remove(subjobKey);

        // ������һ�ֵ���
        this.jobCmdQueue.offer(DispatchCommand.INSTANCE);

        // ����ҵִ�����, ��ִ�гɹ�
        if (statusCode == 0) {
            subjob.setSuccessFinished();
            checkIfJobFinished(jobId);
            return;
        }

        //---------------------------------
        // ����ҵִ�����, ��ִ��ʧ����
        //---------------------------------
        final String cate = subjob.getSubjobCB().getJobCategory();
        JobDispatcherFactory f = this.dispatcherCreator.getJobDispatcherFactory(cate);
        if (!f.isSubjobRetriable(cate) || subjob.getRetryTimes() >= this.maxSubjobRetryTimes) {
            // ��������ҵ��֧�����ԣ������Դ������ޣ���ǳ�"ʧ�����"������ҵ�������
            subjob.setFailureFinished();
            checkIfJobFinished(jobId);
            return;
        }

        // ��"����ڵ�"���Ƿ�������"������"���, �������ˣ�˵�����Ǹ�"�ɻָ�����"
        final boolean isRetriable = s.getOptions().containsKey("doRetry");
        if (isRetriable) {
            // ����"��������"
            subjob.setWhenLastEnterRetryQueue(now);
            this.retryQueue.put(subjobKey, subjob);
        } else {
            subjob.setFailureFinished();
            checkIfJobFinished(jobId);
        }
    }

    // �����ҵ�Ƿ���ִ�����
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
     * ��ø������ƵĲ���ֵ��
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
        // ��������÷�����
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
            LOG.error("���������classȷ����Ӧ�Ĵ�����ʧ��", ex);
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
    // ����REST����, ��Щ����ͨ������ǰ�˵Ľ���ʽ���󣬱����������Ŀǰ��Ϊ������:
    // (1) Job������أ�
    // (2) Mission�������(Mission = Job + ҵ����Ϣ��չ��Ϊ����֮ǰ������Ѿ�д�ɵĴ��룬������mission�ĸ���)��
    //
    ////////////////////////////////////////////////////////////////

    // ����REST������������
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
                    LOG.error("�������ִ��ʧ��", ex);
                }
            }
        }
    }

    //
    //------------------------------------------
    // (1) Job�������
    //------------------------------------------
    //
    // XXX: Լ������������������"xxxRestCmd"����Ӧ�Ĵ�������������"processRestCmd_xxx"����ͬ
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

    // ����״̬��, 0��ʾ�ɹ�, ������ʾ����.
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

    // ��reflect���Ƶ���
    @SuppressWarnings("unused")
    private void processRestCmd_GetJobStatus(Object command) {
        GetJobStatusRestCmd cmd = (GetJobStatusRestCmd)command;
        try {
            JobEntry job = getJobById(cmd.jobId);
            if (job == null) {
                if (cmd.statusMessage != null) {
                    cmd.statusMessage.value = String.format("�Ҳ���jobId=%s����ҵ", cmd.jobId);
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

    // ����״̬��, 0��ʾ�ɹ�, ������ʾ����.
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

    // ��reflect���Ƶ���
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

            // ��ʱ�䵹�����У�Խ�µ�Խ����ǰ��
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

    // ����״̬��, 0��ʾ�ɹ�, ������ʾ����.
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

    // ��reflect���Ƶ���
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
    // (2) Mission�������
    //------------------------------------------
    //
    private static class GetAllJobsAsMissionRestCmd {
        final CountDownLatch latch = new CountDownLatch(1);
        final AdminMissionListDO result = new AdminMissionListDO();
    }

    /**
     * ���������ҵ�����ݣ���Ϊmission���ء�
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

    // ��reflect���Ƶ���
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
            // ��ʱ�䵹�����У�Խ�µ�Խ����ǰ��
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

    // ��reflect���Ƶ���
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

    // ��reflect���Ƶ���
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
            // ��ʱ�䵹�����У�Խ�µ�Խ����ǰ��
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

    // ��reflect���Ƶ���
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

    // missioItemId����Ϊ"jobId|subjobSeqNo"
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
    // (3) Dump & Debug ���
    //------------------------------------------
    //

    private static class GetStateSummaryRestCmd {
        final SchedulerStateSummaryDO result = new SchedulerStateSummaryDO();
        final CountDownLatch latch = new CountDownLatch(1);
    }

    // ����ڲ�״̬
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

    // ��reflect���Ƶ���
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

            // �������ö���
            pdo.setWaitingQueueLength(this.waitingQueue.size());
            pdo.setSendingQueueLength(this.sendingQueue.size());
            pdo.setDispatchedQueueLength(this.dispatchedQueue.size());
            pdo.setRetryQueueLength(this.retryQueue.size());

            // ����ڵ�״̬
            for (WorkerNode e : this.nodeManager.getAllNodeList()) {
                WorkerNode node = (WorkerNode)e;
                SubjobProcessNodeDO nodeDO = new SubjobProcessNodeDO();
                node.getStatus(nodeDO);
                pdo.getProcessNodes().add(nodeDO);
            }

            // ��Ҫ����
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

    // ��reflect���Ƶ���
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

    // ��reflect���Ƶ���
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
