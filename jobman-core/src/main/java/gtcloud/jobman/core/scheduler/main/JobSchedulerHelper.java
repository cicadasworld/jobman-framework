package gtcloud.jobman.core.scheduler.main;

import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;

/**
 * 该接口的实现类用于为JobScheduler提供所需的外围环境信息。
 */
public interface JobSchedulerHelper {
    /**
     * 获得给定名称的参数值。
     * @param name
     * @param defaultVal
     * @return
     */
    String getProperty(String name, String defaultVal);

    /**
     * 发送登录应答给指定节点。
     * @param nodeBaseURL 形如 http://127.0.0.1:44852/gtjobproc
     * @param ack
     */
    void sendLogonAck(String nodeBaseURL, LogonAckDO ack) throws Exception;

    /**
     * 发送心跳应答给指定节点。
     * @param nodeBaseURL 形如 http://127.0.0.1:44852/gtjobproc
     * @param ack
     */
    void sendHeartbeatAck(String nodeBaseURL, HeartbeatReportAckDO ack) throws Exception ;

    /**
     * 发送子作业分派请求给指定节点。
     * @param nodeBaseURL 形如 http://127.0.0.1:44852/gtjobproc
     * @param subjobCB
     * @param body
     */
    void sendSubjobReq(String nodeBaseURL, SubjobControlBlockDO subjobCB, byte[] body) throws Exception ;

}
