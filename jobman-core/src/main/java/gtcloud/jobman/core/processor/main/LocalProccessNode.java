package gtcloud.jobman.core.processor.main;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.utils.NodeId;
import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.JobCategoryDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;
import platon.Message;

// 代表本地子作业处理节点
public class LocalProccessNode {

    private static Logger LOG = LoggerFactory.getLogger(LocalProccessNode.class);

    private NodeId nodeId = null;

    private final long nodeEpoch = System.currentTimeMillis();

    private final SubjobProcessorCreator processorCreator = new SubjobProcessorCreator();

    // 当前的全局调度器的实例标识
    private AtomicReference<String> schedulerId = new AtomicReference<>();
    
    private volatile int heartbeatIntervalMillis = 10*1000;

    // 目前已经发出请求, 但还未获得应答的消息序列号: String -> timestamp
    private final ConcurrentHashMap<String, Long> pendingRequests = new ConcurrentHashMap<>();

    private SubjobManager subjobMan = null;

    private final CountDownLatch stopLatch;
    
    private final LocalProccessNodeHelper helper;

    // 像调度器发送的需要保证发送顺序的message队列, 目前包括SubjobDispatchAckDO, SubjobStatusReportDO
    private final LinkedBlockingQueue<Message> outMsgQueue = new LinkedBlockingQueue<>();
    
	public LocalProccessNode(LocalProccessNodeHelper helper,
			                 NodeId nodeId,
			                 CountDownLatch stopLatch) throws Exception {
    	this.helper = helper;
    	this.stopLatch = stopLatch;
    	this.nodeId = nodeId;
        this.subjobMan = new SubjobManager(this);
    }

    SubjobProcessorCreator getSubjobProcessorCreator() {
        return this.processorCreator;
    }
    
    LocalProccessNodeHelper getHelper() {
    	return helper;
    }

    public void run(String[] args) throws Exception {
        // 启动心跳发送线程
    	this.processorCreator.init();
        Thread t1 = new Thread(() -> {
        	runHeartbeatLoop();        	
        });
        t1.setDaemon(true);
        t1.start();
        
        // 启动message发送线程
        Thread t2 = new Thread(() -> {
			runMessageSendLoop();
        });
        t2.setDaemon(true);
        t2.start();
    }
    
	public void stop() {
        LOG.info("stop() received.");
        
        // 签退
        if (this.schedulerId.get() != null) {
            logoffToScheduler();
        }

        // 清理处理
        this.processorCreator.fini();
    }

    private void runHeartbeatLoop() {
    	if (LOG.isInfoEnabled()) {
    		LOG.info("runHeartbeatLoop()线程开始执行.");
    	}    	
        try {
            runHeartbeatLoop_i();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void runHeartbeatLoop_i() throws InterruptedException {
        boolean done = false;
        final CountDownLatch latch = this.stopLatch;
        for (long round = 0; !done; round ++) {
        	long waitMillis;
        	boolean loggoned = this.schedulerId.get() != null;
        	if (loggoned) {
        		// 已经登录了，现在发送心跳
        		waitMillis = this.heartbeatIntervalMillis;
        	} else if (round == 0) {
        		// 刚启动，首次登录        		
        		waitMillis = 1000;
        	} else {
        		waitMillis = Math.min(5000, this.heartbeatIntervalMillis);        		
        	}
        	
            done = latch.await(waitMillis, TimeUnit.MILLISECONDS);
            if (!done) {
                if (this.schedulerId.get() != null) {
                    // 登录成功后才发送心跳
                    heartbeatToScheduler();
                } else {
                    // 继续登录
                    logonToScheduler();
                }
            }
        }
    }

    private void logonToScheduler() {
        try {
            logonToScheduler_i();
        } catch (Exception e) {
            LOG.error("logonToScheduler() failed: " + e);
        }
    }

    private void logonToScheduler_i() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("logonToScheduler()...");
        }

        final String seqNo = UUID.randomUUID().toString();
        final String poolSize = this.subjobMan.getMainThreadPoolSize() + "";
        LogonReqDO req = new LogonReqDO();
        req.setNodeId(this.nodeId.toString());
        req.setNodeEpoch(this.nodeEpoch);
        req.setSeqNo(seqNo);
        req.getOptions().put(ConstKeys.OPTION_PROCESSOR_POOL_SIZE, poolSize);
        req.getOptions().put(ConstKeys.OPTION_PROCESSOR_BASE_URL, this.helper.getLocalServiceBaseURL());
        
        ArrayList<String> cateList = new ArrayList<>();
        this.processorCreator.getSupportedJobCategoryList(cateList);
        for (String cate : cateList) {
            JobCategoryDO cateDO = new JobCategoryDO();
            cateDO.setJobCategory(cate);
            req.getJobCategoryList().add(cateDO);
        }

        final Long now = Long.valueOf(System.currentTimeMillis());
        this.pendingRequests.put(seqNo, now);
        try {
        	this.helper.sendLogonReq(req);
        } catch (Exception e) {
            this.pendingRequests.remove(seqNo);
            throw e;
        }
    }

    private void logoffToScheduler() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("logoffToScheduler()...");
        }
        try {
            LogoffDO req = new LogoffDO();
            req.setNodeId(this.nodeId.toString());
        	this.helper.sendLogoffReq(req);
        } catch (Exception e) {
            LOG.error("logoffToScheduler() failed: " + e);
        }
    }

    private void heartbeatToScheduler() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("heartbeatToScheduler()...");
        }
        final String seqNo = UUID.randomUUID().toString();
        HeartbeatReportReqDO req = new HeartbeatReportReqDO();
        req.setNodeId(this.nodeId.toString());
        req.setSeqNo(seqNo);
        req.setSchedulerId(this.schedulerId.get());
        req.getOptions().put(ConstKeys.OPTION_PROCESSOR_BASE_URL, this.helper.getLocalServiceBaseURL());
        
        final Long now = Long.valueOf(System.currentTimeMillis());
        this.pendingRequests.put(seqNo, now);
        try {
        	this.helper.sendHeartbeatReportReq(req);
        } catch (Exception e) {
            this.pendingRequests.remove(seqNo);
            LOG.error("heartbeatToScheduler() failed: " + e);
        }
    }

    public void handleLogonAck(LogonAckDO ack) {
        final String seqNo = ack.getSeqNo();
        Long old = this.pendingRequests.remove(seqNo);
        if (old != null) {
            // 确实是我们之前发出的请求
            this.heartbeatIntervalMillis = ack.getHeartbeatIntervalMillis();
            final String sid = ack.getSchedulerId();
            this.schedulerId.set(sid);
        }
    }

    public void handleHeartbeatAck(HeartbeatReportAckDO ack) throws Exception {
        final String seqNo = ack.getSeqNo();
        Long old = this.pendingRequests.remove(seqNo);
        if (old != null) {
            // 确实是我们之前发出的请求
            final String lastestSchedulerId = ack.getSchedulerId();
            final String oldSchedulerId = this.schedulerId.get();
            if (!lastestSchedulerId.equals(oldSchedulerId)) {
                // scheduler可能重新启动了
                if (LOG.isInfoEnabled()) {
                    LOG.info("globalScheduler可能重新启动了, 尝试再次发送登录请求.");
                }
                this.schedulerId.set(null);
                this.logonToScheduler();
            }
        }
    }

    void reportSubjobStatus(SubjobStatusReportDO snapshot) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("reportSubjobStatus()...");
        }
    	snapshot.setNodeId(this.nodeId.toString());
    	this.outMsgQueue.offer(snapshot);
    }
    
    public void handleSubjobReq(SubjobControlBlockDO req, byte[] body) {
        // 回送一个应答给scheduler
        SubjobDispatchAckDO ack = new SubjobDispatchAckDO();
        ack.setNodeId(this.nodeId.toString());
        ack.setSchedulerId(this.schedulerId.get());
        ack.setJobCategory(req.getJobCategory());
        ack.setJobId(req.getJobId());
        ack.setSubjobSeqNo(req.getSubjobSeqNo());
        this.outMsgQueue.offer(ack);
        
        // 执行子作作业
        this.subjobMan.dispatchSubjob(req, body);
    }
    
    private void runMessageSendLoop() {
    	if (LOG.isInfoEnabled()) {
    		LOG.info("runMessageSendLoop()线程开始执行.");
    	}
        try {
        	runMessageSendLoop_i();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }    	
    }
    
    private void runMessageSendLoop_i() throws InterruptedException {
        for (;;) {
            Message msg = this.outMsgQueue.poll(10, TimeUnit.DAYS);
            if (msg == null) {
                continue;
            }
            
            if (msg instanceof SubjobStatusReportDO) {
            	SubjobStatusReportDO r = (SubjobStatusReportDO)msg;
            	try {
            		this.helper.sendSubjobStatusReport(r);
            	} catch (Exception e) {
            		LOG.error("发送SubjobStatusReport给调度器节点失败", e);
            	}
            	continue;
            }
            
            if (msg instanceof SubjobDispatchAckDO) {
            	SubjobDispatchAckDO ack = (SubjobDispatchAckDO)msg;
            	try {
            		this.helper.sendSubjobDispatchAck(ack);
				} catch (Exception e) {
					LOG.error("发送SubjobDispatchAck给调度器节点失败", e);
				}            	
            	continue;
            }
        }
	}
}
