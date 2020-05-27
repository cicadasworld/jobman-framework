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
        // 获得当前连接的客户端列表.
        //
        public ArrayList<ClientInfo> getConnectedClients();
    }

    // TCP服务器端数据传输用套接字, 每个连接而来的客户端对应一个
    public interface ServerSocket extends NetSocket {
        //
        // 向客户端发送一个消息包
        //
        public void writeOneMessage(NetIoMessage message) throws NetSocketException;

        // 获得统计数据
        public NetSocketStat getStat();
    }

    public interface ServerSocketEventListener {

        public void afterClientConnected(ServerSocket socket);

        public void afterClientDisconnected(ServerSocket socket);

        // 若用户层代码在处理afterClientMessageReceived()事件中生成了响应数据, 即填充了
        // ackMessage变量, ackMessage.body.length() > 0, 则框架会将响应数据发送给客户端。
        public void afterClientMessageReceived(ServerSocket socket,
                                               NetIoMessage reqMessage,
                                               NetIoMessage ackMessage);
    }

    // TCP客户端套接字
    public interface ClientSocket extends NetSocket {
        //
        // 等待，直到建立服务器的连接。
        //
        // @param timeoutMillis 超时时间（以毫秒为单位），若为-1表示
        // 一直等待直到建了的连接，否则表示最多等待给定的时间。
        //
        public void waitTillServerConnected(long timeoutMillis)
            throws TimeoutException, NetSocketException;

        // 向网络上写出一个数据包。
        public void writeOneMessage(NetIoMessage message, long timeoutMillis)
            throws TimeoutException, NetSocketException;
    }

    public interface ClientSocketEventListener {

        public void afterServerConnected(ClientSocket socket);

        public void afterServerDisconnected(ClientSocket socket);

        public void afterServerMessageReceived(ClientSocket socket, NetIoMessage message);
    }
}
