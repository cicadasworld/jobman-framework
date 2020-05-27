package gtcloud.common.nsocket;

import gtcloud.common.basetypes.Options;

// �׽��ֹ�����
public interface NetSocketManager {

    public TCP.AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            TCP.ServerSocketEventListener listener,
            Options options) throws NetSocketException;

    public TCP.AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            TCP.ServerSocketEventListener listener) throws NetSocketException;
    
    // auto_reconnect: �������������ӶϿ����Ƿ��Զ�����������
    public TCP.ClientSocket createTcpClientSocket(String server_addr,
            TCP.ClientSocketEventListener listener,
            boolean auto_reconnect,
            Options options) throws NetSocketException;
    
    // �Զ�����������
    public TCP.ClientSocket createTcpClientSocket(String server_addr,
            TCP.ClientSocketEventListener listener) throws NetSocketException;
    
    // ��ֻ���ʼ��һ�������ڷ��͵�udp�׽���, Ӧ����outgoingOnly==true
    public UDP.Socket createUdpSocket(String local_addr,
            UDP.SocketEventListener listener,
            boolean outgoingOnly,
            Options options) throws NetSocketException;

    public String getName();

    public void tearDown();
}
