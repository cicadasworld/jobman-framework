package gtcloud.common.nsocket;

// �׽���IOͳ������
public class NetSocketStat {

    // ����Ϊ֮���͵���Ϣ����/�ֽ���, ���������Ϣ��ʱ��
    public long msgsSent;
    public long bytesSent;
    public long lastSendTick;

    // ����Ϊ֮���յ���Ϣ����/�ֽ���, ����յ���Ϣ��ʱ��
    public long msgsReceived;
    public long bytesReceived;
    public long lastReceiveTick;
}
