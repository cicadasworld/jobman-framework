package gtcloud.common.nsocket;

import gtcloud.common.basetypes.Options;

// 套接字管理器
public interface NetSocketManager {

    public TCP.AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            TCP.ServerSocketEventListener listener,
            Options options) throws NetSocketException;

    public TCP.AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            TCP.ServerSocketEventListener listener) throws NetSocketException;
    
    // auto_reconnect: 到服务器的连接断开后是否自动重连服务器
    public TCP.ClientSocket createTcpClientSocket(String server_addr,
            TCP.ClientSocketEventListener listener,
            boolean auto_reconnect,
            Options options) throws NetSocketException;
    
    // 自动重连服务器
    public TCP.ClientSocket createTcpClientSocket(String server_addr,
            TCP.ClientSocketEventListener listener) throws NetSocketException;
    
    // 若只需初始化一个仅用于发送的udp套接字, 应传入outgoingOnly==true
    public UDP.Socket createUdpSocket(String local_addr,
            UDP.SocketEventListener listener,
            boolean outgoingOnly,
            Options options) throws NetSocketException;

    public String getName();

    public void tearDown();
}
