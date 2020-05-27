package gtcloud.jobman.core.processor;

import java.util.ArrayList;

import gtcloud.common.basetypes.Lifecycle;

public interface SubjobProcessorFactory extends Lifecycle {
    /**
     * ���ص�ǰ����֧�ֵ���ҵ����б�
     * @param result Я����ǰ����֧�ֵ���ҵ����б���,��[IMPORT_IMAGE_DATA]��
     */
    void getSupportedJobCategory(ArrayList<String> result);

    /**
     * ������ҵ��𴴽�����ҵ����������.
     *
     * @param jobCategory ��ҵ���, �� IMPORT_IMAGE_DATA
     *
     * @return ����ҵ����������.
     */
    SubjobProcessor createSubjobProcessor(String jobCategory);
}
