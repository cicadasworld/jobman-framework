package gtcloud.jobman.core.processor;

import gtcloud.common.basetypes.PropertiesEx;

/**
 * "����ҵ"ʵ�����
 */
public interface SubjobEntity {
    /**
     * ����"����ҵ"�������ӡ�
     * @return "����ҵ"��������
     */
    SubjobHandle getHandle();

    /**
     * ����"����ҵ"�ı���, ��'Ӱ���Ʒ�������-20170427123022', ��Ҫ����UI��ʾ.
     * @return "����ҵ"�ı���
     */
    String getJobCaption();

    /**
     * �����ҵ�����ȼ���
     * @return ��ҵ�����ȼ���
     */
    int getPriority();
    
    /**
     * �����ҵ�Ƿ�Ϊ��β��ҵ��
     * @return �Ƿ�Ϊ��β��ҵ��
     */
    boolean isReduce();

    /**
     * ��������ҵ�Ŀ�ѡ���ԡ�
     * @return ����ҵ�Ŀ�ѡ����
     */
    PropertiesEx getOptions();

    /**
     * ��������ҵ�����壬�����ݵĽ��������ھ���ʵ�֡�
     * @return ����ҵ������
     */
    byte[] getSubjobBody();

    /**
     * �Ӹ������󿽱����ݵ���ǰ����
     * @param from
     * @param copyBody
     */
    void copyFrom(SubjobEntity from, boolean copyBody);
}
