package gtcloud.common.nsocket;

// 套接字IO统计数据
public class NetSocketStat {

    // 迄今为之发送的消息包数/字节数, 最近发送消息的时刻
    public long msgsSent;
    public long bytesSent;
    public long lastSendTick;

    // 迄今为之接收的消息包数/字节数, 最近收到消息的时刻
    public long msgsReceived;
    public long bytesReceived;
    public long lastReceiveTick;
}
