package platon;

/**
 * Stringֵ��Holder�࣬�����洢FZDL�ӿڷ���������Ϊ"string"��"out"������
 *
 * <p>���FZDL�ӿڷ����н�һ��string��ǳ�"out"����������÷���ʱ���봫��һ��<code>StringHolder</code>
 * ʵ����Ϊ���������е���Ӧ�������������÷��غ�ͨ����<code>value</code>�ֶ��������Ӧ�����ֵ��
 */
public class StringHolder {

    /**
     * ����һ���µ�StringHolder���󣬽���value�ֶγ�ʼ��Ϊnull��
     */
    public StringHolder() {}

    /**
     * ����һ���µ�StringHolder���󣬲�ʹ�ø���Stringֵ��ʼ����value�ֶΡ�
     *
     * @param value ������ʼ���½�StringHolder�����value�ֶΡ�
     */
    public StringHolder(String value) {
        this.value = value;
    }

    /**
     * ��StringHolder���󱣴��Stringֵ��
     */
    public String value = null;
}
