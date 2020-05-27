package gtcloud.common.nsocket;

public interface NetSocket {

    public static final int TYPE_TCP_ACCEPTOR = 1;
    public static final int TYPE_TCP_STREAM_SERVER = 2;
    public static final int TYPE_TCP_STREAM_CLIENT = 3;
    public static final int TYPE_UDP = 4;
    public static final int TYPE_HTTP_ACCEPTOR = 5;
    public static final int TYPE_HTTP_STREAM_SERVER = 6;
    public static final int TYPE_HTTP_STREAM_CLIENT = 7;

    public int getSocketType();

    public int getSocketId();

    // ���ر��ص�ַ��
    public String getLocalAddress();

    // ����Զ�˵�ַ��
    public String getRemoteAddress();

    public void close();
}
