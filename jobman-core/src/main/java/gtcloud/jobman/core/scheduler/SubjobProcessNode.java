package gtcloud.jobman.core.scheduler;

// ����ҵ����ڵ�
public interface SubjobProcessNode {
    /**
     * ���ؽڵ�Id.
     * @return �ڵ�Id
     */
    String getNodeId();

    /**
     * ���ؽڵ����.
     * @return �ڵ����
     */
    int getNodeSeqNo();

    /**
     * �жϵ�ǰ�ڵ��Ƿ��ڻ״̬.
     * @return
     */
    boolean isAlive();

}
