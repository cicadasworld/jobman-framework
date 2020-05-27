package gtcloud.jobman.core.processor.main;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.pdo.Phase;
import gtcloud.jobman.core.pdo.StepTag;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobStatusDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;
import gtcloud.jobman.core.processor.SubjobContext;
import gtcloud.jobman.core.processor.SubjobHandle;
import gtcloud.jobman.core.processor.SubjobProcessor;
import gtcloud.jobman.core.processor.SubjobRetriableException;
import platon.BooleanHolder;

class SubjobManager implements SubjobContext {

    private static Logger LOG = LoggerFactory.getLogger(SubjobManager.class);

    private final ExecutorService mainThreadPool;

    private final int mainThreadPoolSize;

    private static class ThreadPoolEntry {
        // 线程池对象
        final ExecutorService pool;

        // 线程池大小
        final int poolSize;

        // 正在执行的命令个数
        final AtomicInteger cmdCount;

        ThreadPoolEntry(int poolSize) {
            ThreadFactory tf = MiscUtils.getThreadFactory();
            this.poolSize = poolSize;
            this.pool = Executors.newFixedThreadPool(poolSize, tf);
            this.cmdCount = new AtomicInteger(0);
        }
    }

    // 线程池表: threadPoolName --> ThreadPoolEntry
    private final ConcurrentHashMap<String, ThreadPoolEntry> poolTable = new ConcurrentHashMap<>();

    private static final String DEFAULT_THREAD_POOL_NAME = "general";

    // 正在处理的子作业: subjobKey --> DefaultSubjobEntity
    private final ConcurrentHashMap<String, DefaultSubjobEntity> runningSubjobs = new ConcurrentHashMap<>();

    private final LocalProccessNode currentNode;

    SubjobManager(LocalProccessNode currentNode) {
        this.currentNode = currentNode;

        // 创建线程池
        final int ncpu = Runtime.getRuntime().availableProcessors();
        final ThreadFactory tf = MiscUtils.getThreadFactory();
        String t = currentNode.getHelper().getProperty("gtcloud.jobman.executor.mainThreadPoolSize", null);
        this.mainThreadPoolSize = t != null ? Integer.parseInt(t) : ncpu;
        this.mainThreadPool = Executors.newFixedThreadPool(this.mainThreadPoolSize, tf);
    }

    public void dispatchSubjob(SubjobControlBlockDO scb, byte[] body) {
        final DefaultSubjobEntity subjob = DefaultSubjobEntity.createInstance(scb, body);
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispatchSubjob(): " + subjob);
        }

        // 获得执行该任务的处理器
        final String jobCategory = subjob.getHandle().getJobCategory();
        final String jobId = subjob.getHandle().getJobId();
        final SubjobProcessorCreator creator = this.currentNode.getSubjobProcessorCreator();
        final SubjobProcessor subjobProc = creator.getSubjobProcessor(jobCategory);
        if (subjobProc == null) {
            LOG.error("cannot find subjob-processor for subjob with jobCategory={} and jobId={}", jobCategory, jobId);
            return;
        }

        final String key = subjob.getHandle().getSubjobKey();
        DefaultSubjobEntity old = this.runningSubjobs.putIfAbsent(key, subjob);
        if (old != null) {
            // subjobKey重复了
            if (LOG.isWarnEnabled()) {
                LOG.warn("subjobKey=" + key + " conflicted.");
            }
            return;
        }

        // 投入线程池运行
        this.mainThreadPool.execute(() -> {
            runSubjob(subjobProc, subjob);
        });
    }

    private void runSubjob(final SubjobProcessor subjobProc, final DefaultSubjobEntity subjob) {
        final SubjobHandle jh = subjob.getHandle();
        final SubjobStatusDO currStatus = subjob.getStatusDO();
        SubjobStatusReportDO report;
        try {
            // 报告子作业执行开始
            report = createStatusReportDO(jh, Phase.PHASE_BEGIN, null);
            this.currentNode.reportSubjobStatus(report);

            if (subjob.isReduce()) {
                subjobProc.processReduce(this, subjob);
            } else {
                subjobProc.processSubjob(this, subjob);
            }

            // 报告子作业执行完成
            synchronized (currStatus) {
                report = createStatusReportDO(jh, Phase.PHASE_DONE, currStatus);
            }
            this.currentNode.reportSubjobStatus(report);

        } catch (Throwable ex) {
        	String emsg = String.format("子作业[%s/%s/%d]处理过程中发生异常", 
        			jh.getJobCategory(),
        			jh.getJobId(),
        			jh.getSubjobSeqNo());
            LOG.error(emsg, ex);
            
            synchronized (currStatus) {
                report = createStatusReportDO(jh, Phase.PHASE_DONE, currStatus);
            }
            report.getStatusDO().setStatusCode(-1);
            report.getStatusDO().setStatusMessage(ex.getMessage());
            if (ex instanceof SubjobRetriableException) {
            	// 这是一个可以让调度器尝试"重做"的异常
            	report.getStatusDO().getOptions().put("doRetry", "1");
            }
            this.currentNode.reportSubjobStatus(report);

        } finally {
            // 子作业执行完成了，从队列中移除
            final String key = jh.getSubjobKey();
            this.runningSubjobs.remove(key);
        }
    }

    @Override
    public void reportStepBegin(SubjobHandle jh, String stepId) {
        if (stepId != null) {
            reportStep_i(jh, stepId, StepTag.BEGIN);
        }
    }

    @Override
    public void reportStepEnd(SubjobHandle jh, String stepId) {
        if (stepId != null) {
            reportStep_i(jh, stepId, StepTag.END);
        }
    }

    @Override
    public void reportProgress(SubjobHandle jh, double completedWorkload) {
        final DefaultSubjobHandle djh = (DefaultSubjobHandle)jh;
        final DefaultSubjobEntity subjob = (DefaultSubjobEntity)djh.getSubjobEntity();
        final SubjobStatusDO status = subjob.getStatusDO();
        SubjobStatusReportDO report;
        synchronized (status) {
            status.setCompletedWorkload(completedWorkload);
            report = createStatusReportDO(jh, Phase.PHASE_INPROGRESS, status);
        }
        this.currentNode.reportSubjobStatus(report);
    }

    private void reportStep_i(SubjobHandle jh, String stepId, int stepTag) {
        SubjobStatusReportDO report = createStatusReportDO(jh, Phase.PHASE_INPROGRESS, null);
        report.getStatusDO().setStepId(stepId);
        report.getStatusDO().setStepTag(stepTag);        
        report.getStatusDO().getOptions().put("onlyStep", "1");
        this.currentNode.reportSubjobStatus(report);
    }

    private static SubjobStatusReportDO createStatusReportDO(SubjobHandle jh, int phase, SubjobStatusDO status) {
        SubjobStatusReportDO report = new SubjobStatusReportDO();
        report.setJobCategory(jh.getJobCategory());
        report.setJobId(jh.getJobId());
        report.setSubjobSeqNo(jh.getSubjobSeqNo());
        if (status != null) {
            report.getStatusDO().copyFrom(status);
        }
        report.getStatusDO().setPhase(phase);
        return report;
    }

    private int getParamOfThreadPoolSize(String threadPoolName) {
        final String key = "gtcloud.jobman.executor." + threadPoolName + "ThreadPoolSize";
        final String t = this.currentNode.getHelper().getProperty(key, "-1");
        int n = Integer.parseInt(t);
        if (n <= 0) {
            n = this.mainThreadPoolSize;
        }
        return n;
    }

    private ThreadPoolEntry getOrCreateThreadPool(String threadPoolName) {
        if (threadPoolName == null) {
            threadPoolName = DEFAULT_THREAD_POOL_NAME;
        }
        ThreadPoolEntry e = this.poolTable.get(threadPoolName);
        if (e == null) {
            int poolSize = getParamOfThreadPoolSize(threadPoolName);
            e = new ThreadPoolEntry(poolSize);
            ThreadPoolEntry old = this.poolTable.putIfAbsent(threadPoolName, e);
            if (old != null) {
                e.pool.shutdownNow();
                e = old;
            }
        }
        return e;
    }

    int getMainThreadPoolSize() {
        return this.mainThreadPoolSize;
    }

    private static class CmdWrapper implements Runnable {
        private final Runnable cmd;
        private final AtomicInteger count;

        CmdWrapper(Runnable cmd, AtomicInteger count) {
            this.cmd = cmd;
            this.count = count;
            this.count.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                this.cmd.run();
            } finally {
                this.count.decrementAndGet();
            }
        }
    }

    @Override
    public Future<?> executeCommand(String threadPoolName, Runnable cmd, BooleanHolder isBusy) {
        ThreadPoolEntry e = getOrCreateThreadPool(threadPoolName);
        Future<?> result = null;
        if (cmd != null) {
            CmdWrapper w = new CmdWrapper(cmd, e.cmdCount);
            result = e.pool.submit(w);
        }

        if (isBusy != null) {
            int n = 0 + e.poolSize;
            isBusy.value = e.cmdCount.get() >= n;
        }

        return result;
    }
}
