package gtcloud.common.nsocket.netty;

import gtcloud.common.nsocket.NetIoMessage;
import gtcloud.common.nsocket.NetSocket;
import gtcloud.common.nsocket.NetSocketException;
import gtcloud.common.nsocket.NetSocketStat;
import gtcloud.common.nsocket.TCP.ServerSocket;
import io.netty.channel.socket.SocketChannel;
import platon.ByteStream;

class TcpServerSocketImpl implements ServerSocket {

    private int socketId;
    private SocketChannel socketChannel;
    private String remoteAddress;
    private String localAddress;

    // 迄今为之发送的消息包数/字节数, 最近发送消息的时刻
    private long msgsSent;
    private long bytesSent;
    private long lastSendTick;

    // 迄今为之接收的消息包数/字节数, 最近收到消息的时刻
    private long msgsReceived;
    private long bytesReceived;
    private long lastReceiveTick;

    TcpServerSocketImpl(int socketId, SocketChannel ch) {
        this.socketId = socketId;
        this.socketChannel = ch;
        this.remoteAddress = ch.remoteAddress().toString();
        this.localAddress = ch.localAddress().toString();
    }

    @Override
    public String getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    @Override
    public int getSocketId() {
        return this.socketId;
    }

    @Override
    public int getSocketType() {
        return NetSocket.TYPE_TCP_STREAM_SERVER;
    }

    @Override
    public synchronized NetSocketStat getStat() {
        NetSocketStat stat = new NetSocketStat();
        stat.msgsSent = this.msgsSent;
        stat.bytesSent = this.bytesSent;
        stat.lastSendTick = this.lastSendTick;
        stat.msgsReceived = this.msgsReceived;
        stat.bytesReceived = this.bytesReceived;
        stat.lastReceiveTick = this.lastReceiveTick;
        return stat;
    }

    @Override
    public void writeOneMessage(NetIoMessage message) throws NetSocketException {
        SocketChannel ch;
        synchronized (this) {
            ch = this.socketChannel;
        }

        if (ch != null) {
            ch.writeAndFlush(message);
        } else {
            throw new NetSocketException("socket has been closed");
        }

        updateSendStat(message);
    }

    @Override
    public synchronized void close() {
        if (this.socketChannel != null) {
            this.socketChannel.close();
            this.socketChannel = null;
        }
    }

    synchronized void updateSendStat(NetIoMessage msg) {
        this.msgsSent += 1;
        
        ByteStream body = msg.getBody();
        if (body != null) {
            this.bytesSent += (body.length() - body.read_pos());
        }
        this.lastSendTick = System.currentTimeMillis();
    }

    synchronized void updateRecvStat(NetIoMessage msg) {
        this.msgsReceived += 1;
        
        ByteStream body = msg.getBody();        
        if (body != null) {
            this.bytesReceived += (body.length() - body.read_pos());
        }
        this.lastReceiveTick = System.currentTimeMillis();
    }
}
