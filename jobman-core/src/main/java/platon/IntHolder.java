package platon;

/**
 * Integerֵ��Holder�࣬�����洢FZDL�ӿڷ���������Ϊ"int"��"out"������
 *
 * <p>���FZDL�ӿڷ����н�һ��int��ǳ�"out"����������÷���ʱ���봫��һ��<code>IntHolder</code>
 * ʵ����Ϊ���������е���Ӧ�������������÷��غ�ͨ����<code>value</code>�ֶ��������Ӧ�����ֵ��
 */
public class IntHolder {

    /**
     * ����һ���µ�IntHolder���󣬽���value�ֶγ�ʼ��Ϊ0��
     */
    public IntHolder() {}

    /**
     * ����һ���µ�IntHolder���󣬲�ʹ�ø���intֵ��ʼ����value�ֶΡ�
     *
     * @param value ������ʼ���½�IntHolder�����value�ֶΡ�
     */
    public IntHolder(int value) {
        this.value = value;
    }

    /**
     * ��IntHolder���󱣴��intֵ��
     */
    public int value = 0;
}

