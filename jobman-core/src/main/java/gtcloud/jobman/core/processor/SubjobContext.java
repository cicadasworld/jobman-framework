package gtcloud.jobman.core.processor;

import java.util.concurrent.Future;

import platon.BooleanHolder;

/**
 * Ϊ"����ҵ"�����ṩ�����Ļ���
 */
public interface SubjobContext {
	/**
	 * ��������ҵ��������е�ĳ�����迪ʼ��
     * @param jh "����ҵ"������, ������������ĸ�"����ҵ"�Ĵ������; 
	 * @param stepId �����ʶ��
	 */
	void reportStepBegin(SubjobHandle jh, String stepId);
	
	/**
	 * ��������ҵ��������еĽ��ȡ�
     * @param jh "����ҵ"������, ������������ĸ�"����ҵ"�Ĵ������; 
	 * @param completedWorkload ������ɵĹ�������
	 */
	void reportProgress(SubjobHandle jh, double completedWorkload);

	/**
	 * ��������ҵ��������е�ĳ��������ɡ�
     * @param jh "����ҵ"������, ������������ĸ�"����ҵ"�Ĵ������; 
	 * @param stepId �����ʶ��
	 */
	void reportStepEnd(SubjobHandle jh, String stepId);
	
    /**
     * ʹ�ø������ֵ��̳߳���ִ��һ������.
     *
     * @param threadPoolName �̳߳ص�����, ��Ϊnull��ʹ��Ĭ������"general";
     * @param cmd Ҫִ�е���������;
     * @param isBusy �����Ƿ�æ ��
     */
    Future<?> executeCommand(String threadPoolName, Runnable cmd, BooleanHolder isBusy);

}
