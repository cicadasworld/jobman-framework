package platon;

/**
 * Longֵ��Holder�࣬�����洢FZDL�ӿڷ���������Ϊ"long"��"out"������
 *
 * <p>���FZDL�ӿڷ����н�һ��long��ǳ�"out"����������÷���ʱ���봫��һ��<code>LongHolder</code>
 * ʵ����Ϊ���������е���Ӧ�������������÷��غ�ͨ����<code>value</code>�ֶ��������Ӧ�����ֵ��
 */
public class LongHolder {

    /**
     * ����һ���µ�LongHolder���󣬽���value�ֶγ�ʼ��Ϊ0��
     */
    public LongHolder() {}

    /**
     * ����һ���µ�LongHolder���󣬲�ʹ�ø���longֵ��ʼ����value�ֶΡ�
     *
     * @param value ������ʼ���½�LongHolder�����value�ֶΡ�
     */
    public LongHolder(long value) {
        this.value = value;
    }

    /**
     * ��LongHolder���󱣴��longֵ��
     */
    public long value = 0;
}
