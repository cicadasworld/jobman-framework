package gtcloud.common.nsocket;

// a name-space for UDP related class
public class UDP {

    public static final int MAX_MESSAGE_SIZE = 4096;

    /**
     * UDP�׽��ֽӿڡ�
     */
    public interface Socket extends NetSocket {
        // ����һ����Ϣ��
        public void writeOneMessage(String receiverAddress,
                                    NetIoMessage message) throws NetSocketException;
    }

    public interface SocketEventListener {

        public void afterUdpMessageReceived(Socket socket,
                                            String senderAddress,
                                            NetIoMessage message);
    }
}
