package gtcloud.common.nsocket;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

// a name-space for TCP related class
public class TCP {

    public static class ClientInfo {

        public String addr;

        public int sockId;

        public NetSocketStat stat = new NetSocketStat();
    }

    public interface AcceptorSocket extends NetSocket {
        //
        // ��õ�ǰ���ӵĿͻ����б�.
        //
        public ArrayList<ClientInfo> getConnectedClients();
    }

    // TCP�����������ݴ������׽���, ÿ�����Ӷ����Ŀͻ��˶�Ӧһ��
    public interface ServerSocket extends NetSocket {
        //
        // ��ͻ��˷���һ����Ϣ��
        //
        public void writeOneMessage(NetIoMessage message) throws NetSocketException;

        // ���ͳ������
        public NetSocketStat getStat();
    }

    public interface ServerSocketEventListener {

        public void afterClientConnected(ServerSocket socket);

        public void afterClientDisconnected(ServerSocket socket);

        // ���û�������ڴ���afterClientMessageReceived()�¼�����������Ӧ����, �������
        // ackMessage����, ackMessage.body.length() > 0, ���ܻὫ��Ӧ���ݷ��͸��ͻ��ˡ�
        public void afterClientMessageReceived(ServerSocket socket,
                                               NetIoMessage reqMessage,
                                               NetIoMessage ackMessage);
    }

    // TCP�ͻ����׽���
    public interface ClientSocket extends NetSocket {
        //
        // �ȴ���ֱ�����������������ӡ�
        //
        // @param timeoutMillis ��ʱʱ�䣨�Ժ���Ϊ��λ������Ϊ-1��ʾ
        // һֱ�ȴ�ֱ�����˵����ӣ������ʾ���ȴ�������ʱ�䡣
        //
        public void waitTillServerConnected(long timeoutMillis)
            throws TimeoutException, NetSocketException;

        // ��������д��һ�����ݰ���
        public void writeOneMessage(NetIoMessage message, long timeoutMillis)
            throws TimeoutException, NetSocketException;
    }

    public interface ClientSocketEventListener {

        public void afterServerConnected(ClientSocket socket);

        public void afterServerDisconnected(ClientSocket socket);

        public void afterServerMessageReceived(ClientSocket socket, NetIoMessage message);
    }
}
