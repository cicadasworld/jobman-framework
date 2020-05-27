package platon;


public interface Freezable {

    // ����ǰ���������������ֽ����С�
    void freeze(ByteStream output) throws FreezeException;

    // �Ӹ����ֽ����н��, ��������õ������ݸ��ǵ���ǰ�����ϡ�
    void defreeze(ByteStream input) throws DefreezeException;

    // ��¡��ǰ����
    Freezable makeClone();

    // �Ӹ������󿽱����ݵ���ǰ����
    void copyFrom(Freezable from);
}
