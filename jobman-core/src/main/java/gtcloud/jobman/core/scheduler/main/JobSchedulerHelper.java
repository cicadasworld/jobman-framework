package gtcloud.jobman.core.scheduler.main;

import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;

/**
 * �ýӿڵ�ʵ��������ΪJobScheduler�ṩ�������Χ������Ϣ��
 */
public interface JobSchedulerHelper {
    /**
     * ��ø������ƵĲ���ֵ��
     * @param name
     * @param defaultVal
     * @return
     */
    String getProperty(String name, String defaultVal);

    /**
     * ���͵�¼Ӧ���ָ���ڵ㡣
     * @param nodeBaseURL ���� http://127.0.0.1:44852/gtjobproc
     * @param ack
     */
    void sendLogonAck(String nodeBaseURL, LogonAckDO ack) throws Exception;

    /**
     * ��������Ӧ���ָ���ڵ㡣
     * @param nodeBaseURL ���� http://127.0.0.1:44852/gtjobproc
     * @param ack
     */
    void sendHeartbeatAck(String nodeBaseURL, HeartbeatReportAckDO ack) throws Exception ;

    /**
     * ��������ҵ���������ָ���ڵ㡣
     * @param nodeBaseURL ���� http://127.0.0.1:44852/gtjobproc
     * @param subjobCB
     * @param body
     */
    void sendSubjobReq(String nodeBaseURL, SubjobControlBlockDO subjobCB, byte[] body) throws Exception ;

}
