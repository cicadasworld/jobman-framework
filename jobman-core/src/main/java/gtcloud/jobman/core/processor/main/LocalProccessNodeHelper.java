package gtcloud.jobman.core.processor.main;

import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;

public interface LocalProccessNodeHelper {
	/**
	 * ��ø������ƵĲ���ֵ��
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	String getProperty(String name, String defaultVal);

	/**
	 * ���ر��صķ������ַ������"http://host:port/gtjobproc"
	 * @return
	 */
	String getLocalServiceBaseURL();
	
	/**
	 * �����������͵�¼����
	 * @param req
	 * @throws Exception
	 */
	void sendLogonReq(LogonReqDO req) throws Exception;

	/**
	 * �����������͵ǳ����� 
	 * @param req
	 * @throws Exception
	 */
	void sendLogoffReq(LogoffDO req) throws Exception;
	
	/**
	 * �������������������档
	 * @param req
	 * @throws Exception
	 */
	void sendHeartbeatReportReq(HeartbeatReportReqDO req) throws Exception;

	/**
	 * ����������������ҵ״̬���档
	 * @param snapshot
	 * @throws Exception
	 */
	void sendSubjobStatusReport(SubjobStatusReportDO snapshot) throws Exception;
	
	/**
	 * ����������������ҵ����Ӧ��
	 * @param ack
	 */
	void sendSubjobDispatchAck(SubjobDispatchAckDO ack) throws Exception;
}
