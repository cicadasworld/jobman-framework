package gtcloud.jobman.core.processor.main;

import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;

public interface LocalProccessNodeHelper {
	/**
	 * 获得给定名称的参数值。
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	String getProperty(String name, String defaultVal);

	/**
	 * 返回本地的服务基地址，形如"http://host:port/gtjobproc"
	 * @return
	 */
	String getLocalServiceBaseURL();
	
	/**
	 * 给调度器发送登录请求。
	 * @param req
	 * @throws Exception
	 */
	void sendLogonReq(LogonReqDO req) throws Exception;

	/**
	 * 给调度器发送登出请求。 
	 * @param req
	 * @throws Exception
	 */
	void sendLogoffReq(LogoffDO req) throws Exception;
	
	/**
	 * 给调度器发送心跳报告。
	 * @param req
	 * @throws Exception
	 */
	void sendHeartbeatReportReq(HeartbeatReportReqDO req) throws Exception;

	/**
	 * 给调度器发送子作业状态报告。
	 * @param snapshot
	 * @throws Exception
	 */
	void sendSubjobStatusReport(SubjobStatusReportDO snapshot) throws Exception;
	
	/**
	 * 给调度器发送子作业分派应答。
	 * @param ack
	 */
	void sendSubjobDispatchAck(SubjobDispatchAckDO ack) throws Exception;
}
