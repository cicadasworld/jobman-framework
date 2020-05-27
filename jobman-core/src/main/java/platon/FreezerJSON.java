package platon;


public interface FreezerJSON {

    // ����ǰ��Ϣ�����JSON��, ����JSON����ָ��
    JsonNode freezeToJSON() throws FreezeException;

    // �Ӹ���JSON���н��, ��������õ������ݸ��ǵ���ǰ��Ϣ�ϡ�
    void defreezeFromJSON(JsonNode inputJsonTree) throws DefreezeException;
}
