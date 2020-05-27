package gtcloud.jobman.core.scheduler;

import java.util.ArrayList;

import gtcloud.common.basetypes.Lifecycle;

public interface JobDispatcherFactory extends Lifecycle {
    /**
     * ���ص�ǰ����֧�ֵ���ҵ����б�
     * @param result Я����ǰ����֧�ֵ���ҵ����б���, ��[IMPORT_IMAGE_DATA]��
     */
    void getSupportedJobCategory(ArrayList<String> result);

    /**
     * ������ҵ��𴴽���ҵ������.
     *
     * @param jobCategory ��ҵ���, �� IMPORT_IMAGE_DATA
     *
     * @return ��ҵ����������.
     */
    JobDispatcher createJobDispatcher(String jobCategory);

    /**
     * ����������ҵ�Ƿ�֧������.
     *
     * @param jobCategory ��ҵ���, �� IMPORT_IMAGE_DATA
     *
     * @return ��֧�����Է���true.
     */
    boolean isSubjobRetriable(String jobCategory);

    /**
     * ���ظ���������ҵ�����ļ���������.
     * @param jobCategory ��ҵ���, �� IMPORT_IMAGE_DATA
     * @param result ���������������ҵ���趨��.
     */
    default void getStepInfoList(String jobCategory, ArrayList<StepInfo> result) {
        ;
    }
}
