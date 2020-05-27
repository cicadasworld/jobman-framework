package gtcloud.jobman.core.scheduler.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.JobCategoryDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.LogonReqDO;

/**
 * �������еĴ���ڵ�
 * XXX: ������̰߳�ȫ, ��JobScheduler��֤�̰߳�ȫ���⡣
 */
class WorkerNodeManager {

    private static Logger LOG = LoggerFactory.getLogger(WorkerNodeManager.class);

    // nodeId --> �ڵ�
    private final HashMap<String, WorkerNode> nodes = new HashMap<>();

    private final JobSchedulerHelper helper;
    
    private final int heartbeatIntervalMillis;

    private int nodeSeqSeed = 0;

    private final String SID = UUID.randomUUID().toString().replace("-", "");
    
    WorkerNodeManager(JobScheduler scheduler, JobSchedulerHelper helper) {
        this.helper = helper;
        final int t = Integer.parseInt(this.helper.getProperty("gtcloud.processorNode.heartbeatIntervalMillis", "-1"));
        this.heartbeatIntervalMillis = t > 0 ? t : 5000;
    }

    int getHeartbeatIntervalMillis() {
        return this.heartbeatIntervalMillis;
    }

    JobSchedulerHelper getHelper() {
		return helper;
	}
    
    // ���֧�ָ�����ҵ�������߽ڵ��б�
    ArrayList<WorkerNode> getAliveNodeList(String jobCategory) {
        ArrayList<WorkerNode> result = new ArrayList<>();
        for (WorkerNode node : this.nodes.values()) {
            if (node.isCategorySupported(jobCategory) && node.isAlive()) {
                result.add(node);
            }
        }
        return result;
    }

    ArrayList<WorkerNode> getAllNodeList() {
        return new ArrayList<>(this.nodes.values());
    }

    int getAliveNodeCount() {
    	int n = 0;
        for (WorkerNode node : this.nodes.values()) {
            if (node.isAlive()) {
                n += 1;
            }
        }
        return n;
    }
    
    void handleNodeLogonReq(String nodeBaseURL, LogonReqDO pdo)  {
        final ArrayList<String> cateList = new ArrayList<>();
        for (JobCategoryDO e : pdo.getJobCategoryList()) {
            final String cate = e.getJobCategory();
            cateList.add(cate);
        }

        final String nodeId = pdo.getNodeId();
        final long epoch = pdo.getNodeEpoch();

        // �������ڵ��ϵ��̳߳ش�С
        String poolSizeStr = pdo.getOptions().get(ConstKeys.OPTION_PROCESSOR_POOL_SIZE);
        int poolSize = -1;
        if (poolSizeStr != null) {
            poolSize = Integer.parseInt(poolSizeStr);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("handleNodeLogonReq(), nodeId={}, nodeBaseURL={}", nodeId, nodeBaseURL);
        }

        WorkerNode oldNode = this.nodes.get(nodeId);
        boolean effectiveLogon = false;
        if (oldNode != null) {
            if (oldNode.isAlive()) {
                // ����ͬһ�ڵ��ٴε�¼������, һ���Ǵ���ڵ㴦�����������, �ٴ���������ͬ���ò�����processor
                // ����������, ����֮
            } else {
                oldNode.afterLogon(nodeBaseURL, epoch, cateList, poolSize);
                effectiveLogon = true;
            }
        } else {
            final int nodeSeq = getNextNodeSeq();
            WorkerNode node =
                    new WorkerNode(nodeId, nodeSeq, nodeBaseURL, epoch, cateList, poolSize, this);
            this.nodes.put(nodeId, node);
            effectiveLogon = true;
        }

        if (effectiveLogon) {
            try {
                sendLogonAck(nodeBaseURL, pdo);
            } catch (Exception e) {
                LOG.error("sendLogonAck() failed: " + e);
                return;
            }
        }
    }

    private void sendLogonAck(String nodeBaseURL, final LogonReqDO req) throws Exception {
        LogonAckDO ack = new LogonAckDO();
        ack.setSeqNo(req.getSeqNo());
        ack.setHeartbeatIntervalMillis(heartbeatIntervalMillis);
        ack.setSchedulerId(SID);
        this.helper.sendLogonAck(nodeBaseURL, ack);
    }

    void handleNodeHeartbeatReq(String nodeBaseURL, HeartbeatReportReqDO pdo) {
        final String nodeId = pdo.getNodeId();
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleNodeHeartbeatReq(), nodeId=" + nodeId);
        }

        WorkerNode node = this.nodes.get(nodeId);
        if (node != null) {
            node.afterHeartbeatReceived();
        }

        try {
            sendHeartbeatAck(nodeBaseURL, pdo);
        } catch (Exception e) {
            LOG.error("sendHeartbeatAck() failed: " + e);
            //e.printStackTrace();
        }
    }

    private void sendHeartbeatAck(String nodeBaseURL,
                                  HeartbeatReportReqDO req) throws Exception {
        HeartbeatReportAckDO ack = new HeartbeatReportAckDO();
        ack.setSeqNo(req.getSeqNo());
        ack.setSchedulerId(SID);
        this.helper.sendHeartbeatAck(nodeBaseURL, ack);
    }

    WorkerNode getNodeById(String nodeId) {
        return this.nodes.get(nodeId);
    }

    private int getNextNodeSeq() {
        return this.nodeSeqSeed ++;
    }

    void handleNodeLogoffReq(LogoffDO pdo) {
        final String nodeId = pdo.getNodeId();
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleNodeLogoffReq(), nodeId=" + nodeId);
        }

        WorkerNode node = this.nodes.get(nodeId);
        if (node != null) {
            node.afterLogoff();
        }
    }

}
