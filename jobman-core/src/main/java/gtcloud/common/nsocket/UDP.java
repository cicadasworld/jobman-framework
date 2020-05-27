package gtcloud.common.nsocket;

// a name-space for UDP related class
public class UDP {

    public static final int MAX_MESSAGE_SIZE = 4096;

    /**
     * UDP套接字接口。
     */
    public interface Socket extends NetSocket {
        // 发送一个消息包
        public void writeOneMessage(String receiverAddress,
                                    NetIoMessage message) throws NetSocketException;
    }

    public interface SocketEventListener {

        public void afterUdpMessageReceived(Socket socket,
                                            String senderAddress,
                                            NetIoMessage message);
    }
}
