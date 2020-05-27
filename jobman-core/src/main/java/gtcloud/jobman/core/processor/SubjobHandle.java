package gtcloud.jobman.core.processor;

// "����ҵ"�����ӽӿ�
public interface SubjobHandle {

    // "����ҵ"�ı�ʶ��ȫ��Ψһ, ��'4ea6530716d149429edb5749460938ec'
    String getJobId();

    // "����ҵ"�����, ��'IMPORT_IMAGE_DATA'
    String getJobCategory();

    // ��ǰ"����ҵ"�����, ��0��ʼ
    int getSubjobSeqNo();

    // ��ǰ"����ҵ"���ڣ�������"����ҵ"��������������ҵ
    int getSiblingsCount();

    // ����"����ҵ"��Ψһ��key
    String getSubjobKey();

    // �Ӹ������󿽱����ݵ���ǰ����
    void copyFrom(SubjobHandle from);
}
