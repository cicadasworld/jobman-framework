package gtcloud.common.basetypes;

/**
 * �ýӿ�����������Դ����js����Ƭ�ȣ������ԡ�
 */
public interface ResourceAttributes {

    /**
     * �����Դ��ETag��
     * @return ��Դ��ETag��
     */
    String getETag();

    /**
     * �����Դ������޸�ʱ�䡣
     * @return ��Դ������޸�ʱ�䡣
     */
    long getLastModified();
}
