package gtcloud.jobman.core.scheduler;

import gtcloud.jobman.core.scheduler.event.SubjobBaseEvent;

public interface JobPersister {

	/**
	 * ��������ҵ�¼���־��������ԡ�
	 * @param subjob ����ҵ����
	 * @param event �¼�����
	 */
	void writeSubjobEventLog(SubjobEntry subjob, SubjobBaseEvent event);

	/**
	 * �ڳ־ô洢��д������ҵ״̬��־��
	 * @param subjob ����ҵ����
	 * @param state ��ǰ״̬
	 */
	void writeSubjobFinishState(SubjobEntry subjob, JobState state);
	
	/**
	 * �ڳ־ô洢��д����ҵ״̬��־��
	 * @param job ��ҵ����
	 * @param state ��ǰ״̬
	 */
	void writeJobFinishState(JobEntry job, JobState state);

	/**
	 * �ӳ־ô洢�ж�����ҵ�������塣
	 * @param subjob ����ҵ����
	 * @return ������ҵ�������塣
	 */
	byte[] readSubjobBodyBytes(SubjobEntry subjob);

	/**
	 * �ӳ־ô洢�ж�����ҵ���¼���־��
	 * @param subjob ����ҵ����;
	 * @param consumer �иö���Ը���־�н��д���
	 */
	void readSubjobEventLogs(SubjobEntry subjob, LinesConsumer consumer);

}
