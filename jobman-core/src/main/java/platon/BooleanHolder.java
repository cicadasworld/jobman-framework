package platon;

/**
 * Booleanֵ��Holder�࣬�����洢FZDL�ӿڷ���������Ϊ"bool"��"out"������
 *
 * <p>���FZDL�ӿڷ����н�һ��boolean��ǳ�"out"����������÷���ʱ���봫��һ��<code>BooleanHolder</code>
 * ʵ����Ϊ���������е���Ӧ�������������÷��غ�ͨ����<code>value</code>�ֶ��������Ӧ�����ֵ��
 */
public final class BooleanHolder
{
    /**
     * ����һ���µ�BooleanHolder���󣬽���value�ֶγ�ʼ��Ϊfalse��
     */
    public BooleanHolder() {}

    /**
     * ����һ���µ�BooleanHolder���󣬲�ʹ�ø���booleanֵ��ʼ����value�ֶΡ�
     *
     * @param value ������ʼ���½�BooleanHolder�����value�ֶΡ�
     */
    public BooleanHolder(boolean value) {
        this.value = value;
    }

    /**
     * ��BooleanHolder���󱣴��booleanֵ��
     */
    public boolean value = false;
}
