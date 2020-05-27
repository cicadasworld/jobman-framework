package gtcloud.jobman.core.scheduler.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import gtcloud.jobman.core.common.Helper;
import gtcloud.jobman.core.pdo.SubjobProcessNodeDO;
import gtcloud.jobman.core.scheduler.SubjobProcessNode;

/**
 * ����һ������ڵ㡣
 * XXX: ������̰߳�ȫ, ��JobScheduler��֤�̰߳�ȫ���⡣
 */
class WorkerNode implements SubjobProcessNode {

    private final String nodeId;

    private final int nodeSeq;

    private volatile long nodeEpoch = 0;

    private volatile boolean isOnline = false;

    private final WorkerNodeManager manager;

    private final HashSet<String> supportedCategorySet = new HashSet<>();

    private long lastActiveMillis;

    // ����ڵ��REST����˵�
    private String nodeBaseURL;

    // processor�ڵ�˵��̳߳ش�С
    private volatile int workerNodeThreadPoolSize = 4;
    
    // �Ѿ����ɸ���ǰ����ڵ㡢�����ڵ�ǰ�ڵ��ϴ��������ҵ����
    private final HashMap<String, Boolean> pendingSubjobKeys = new HashMap<>();
    
    WorkerNode(String nodeId,
               int nodeSeq,
               String nodeBaseURL,
               long nodeEpoch,
               ArrayList<String> cateList,
               int poolSize,
               WorkerNodeManager mgr) {
    	this.nodeId = nodeId;
        this.nodeSeq = nodeSeq;
        this.nodeBaseURL = nodeBaseURL;
        this.nodeEpoch = nodeEpoch;
        if (cateList != null && cateList.size() > 0) {
            this.supportedCategorySet.addAll(cateList);
        }
        if (poolSize > 0) {
            this.workerNodeThreadPoolSize = poolSize;
        }
        this.manager = mgr;
        this.lastActiveMillis = System.currentTimeMillis();
        this.isOnline = true;
    }

    @Override
    public String getNodeId() {
        return this.nodeId;
    }

    @Override
    public int getNodeSeqNo() {
        return this.nodeSeq;
    }
    
    @Override
    public boolean isAlive() {
    	if (!this.isOnline) {
    		return false;
    	}
    	long now = System.currentTimeMillis();
    	long t0 = this.lastActiveMillis;
    	return (now - t0) <= 2 * this.manager.getHeartbeatIntervalMillis();
    }

    boolean isOverloaded(String jobCategory) {
    	return this.pendingSubjobKeys.size() >= this.workerNodeThreadPoolSize;
    }

    boolean isCategorySupported(String jobCategory) {
        return this.supportedCategorySet.contains(jobCategory);
    }

    void afterLogon(String nodeBaseURL, long epoch, ArrayList<String> cateList, int poolSize) {
        final long oldEpoch = updateEpoch(epoch);
        if (oldEpoch != epoch) {
            resetQueues();
        }
        updateSupportedCategoryList(cateList);
        this.nodeBaseURL = nodeBaseURL;
        this.isOnline = true;
        if (poolSize > 0) {
            this.workerNodeThreadPoolSize = poolSize;
        }
        touchLastActiveTime();
    }

    void afterHeartbeatReceived() {
        touchLastActiveTime();
    }

    void afterLogoff() {
        resetQueues();
        this.isOnline = false;
    }

    private void touchLastActiveTime() {
        long now = System.currentTimeMillis();
        this.lastActiveMillis = now;
    }

    private long updateEpoch(long epoch) {
        long old = this.nodeEpoch;
        this.nodeEpoch = epoch;
        return old;
    }

    private void updateSupportedCategoryList(ArrayList<String> cateList) {
        this.supportedCategorySet.clear();
        if (cateList != null && cateList.size() > 0) {
            this.supportedCategorySet.addAll(cateList);
        }
    }

    private void resetQueues() {
    	this.pendingSubjobKeys.clear();
    }

	String getForwardToURL() {
		return this.nodeBaseURL;
	}

	void afterSubjobDispatchReqSent(String jobId, int subjobSeqNo) {
		final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
		this.pendingSubjobKeys.put(subjobKey, Boolean.TRUE);
	}
	
	void afterSubjobDispatchAckReceived(String jobId, int subjobSeqNo) {
		touchLastActiveTime();
	}

	void afterSubjobDispatchReqExpired(String jobId, int subjobSeqNo) {
		final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
		this.pendingSubjobKeys.remove(subjobKey);
	}

	void afterSubjobStatusReportReceived(String jobId, int subjobSeqNo, boolean isFinished) {
		touchLastActiveTime();
		if (isFinished) {
			final String subjobKey = Helper.makeSubjobKey(jobId, subjobSeqNo);
			this.pendingSubjobKeys.remove(subjobKey);
		}
	}

    void getStatus(SubjobProcessNodeDO nodeDO) {
        int qlen = this.pendingSubjobKeys.size();
        nodeDO.setNodeId(this.nodeId);
        nodeDO.setSubjobQueueLength(qlen);
        nodeDO.setIsAlive(isAlive());
        nodeDO.setMillisSinceLastAlive(System.currentTimeMillis() - this.lastActiveMillis);
        nodeDO.getSupportedJobCategoryList().addAll(this.supportedCategorySet);
        
        nodeDO.getOptions().put("nodeBaseURL", this.nodeBaseURL);
        nodeDO.getOptions().put("workerNodeThreadPoolSize", this.workerNodeThreadPoolSize+"");   
        nodeDO.getOptions().put("isLogoned", this.isOnline + "");
    }    
}
