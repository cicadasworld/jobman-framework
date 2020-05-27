package gtcloud.common.nsocket.netty;

import java.util.concurrent.atomic.AtomicInteger;

import gtcloud.common.basetypes.Options;
import gtcloud.common.nsocket.NetSocketException;
import gtcloud.common.nsocket.NetSocketManager;
import gtcloud.common.nsocket.TCP.AcceptorSocket;
import gtcloud.common.nsocket.TCP.ClientSocket;
import gtcloud.common.nsocket.TCP.ClientSocketEventListener;
import gtcloud.common.nsocket.TCP.ServerSocketEventListener;
import gtcloud.common.nsocket.UDP.Socket;
import gtcloud.common.nsocket.UDP.SocketEventListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

class NetSocketManagerImpl implements NetSocketManager {

    private String name;
    private final int netioThreadCount;
    EventLoopGroup workerGroup = null;

    private AtomicInteger nextSocketId = new AtomicInteger();

    public NetSocketManagerImpl(String name,
                                int netioThreadCount,
                                Options options) {
        this.name = name;
        this.netioThreadCount = netioThreadCount;
        if (options != null) {
            // TODO
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            ServerSocketEventListener listener, Options options)
            throws NetSocketException {
        return new TcpAcceptorSocketImpl(this, listen_addr, listener, options);
    }

    @Override
    public AcceptorSocket createTcpAcceptorSocket(String listen_addr,
            ServerSocketEventListener listener)
            throws NetSocketException {
        return new TcpAcceptorSocketImpl(this, listen_addr, listener, null);
    }

    @Override
    public ClientSocket createTcpClientSocket(String server_addr,
            ClientSocketEventListener listener, boolean auto_reconnect,
            Options options) throws NetSocketException {
        return new TcpClientSocketImpl(this, server_addr, listener, auto_reconnect, options);
    }

    @Override
    public ClientSocket createTcpClientSocket(String server_addr,
            ClientSocketEventListener listener) throws NetSocketException {
        return new TcpClientSocketImpl(this, server_addr, listener, true, null);
    }

    @Override
    public Socket createUdpSocket(String local_addr,
            SocketEventListener listener, boolean outgoingOnly,
            Options options) throws NetSocketException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized void tearDown() {
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
    }

    protected int getNextSocketId() {
        return this.nextSocketId.incrementAndGet();
    }

    protected synchronized EventLoopGroup getNetIoWorkerEventLoopGroup() {
        if (this.workerGroup == null) {
            int threadN = Math.max(this.netioThreadCount, 0);
            this.workerGroup = new NioEventLoopGroup(threadN, new DaemonThreadFactory());
        }
        return this.workerGroup;
    }
}
