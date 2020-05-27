package gtcloud.jobman.core.scheduler;

import gtcloud.jobman.core.scheduler.event.SubjobBaseEvent;

public interface JobPersister {

	/**
	 * 簿记子作业事件日志，方便调试。
	 * @param subjob 子作业对象
	 * @param event 事件对象
	 */
	void writeSubjobEventLog(SubjobEntry subjob, SubjobBaseEvent event);

	/**
	 * 在持久存储中写入子作业状态标志。
	 * @param subjob 子作业对象
	 * @param state 当前状态
	 */
	void writeSubjobFinishState(SubjobEntry subjob, JobState state);
	
	/**
	 * 在持久存储中写入作业状态标志。
	 * @param job 作业对象
	 * @param state 当前状态
	 */
	void writeJobFinishState(JobEntry job, JobState state);

	/**
	 * 从持久存储中读子作业的数据体。
	 * @param subjob 子作业对象
	 * @return 读子作业的数据体。
	 */
	byte[] readSubjobBodyBytes(SubjobEntry subjob);

	/**
	 * 从持久存储中读子作业的事件日志。
	 * @param subjob 子作业对象;
	 * @param consumer 有该对象对各日志行进行处理
	 */
	void readSubjobEventLogs(SubjobEntry subjob, LinesConsumer consumer);

}
