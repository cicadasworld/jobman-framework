package gtcloud.common.nsocket.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.Options;
import gtcloud.common.utils.NetUtils;
import gtcloud.common.nsocket.NetIoMessage;
import gtcloud.common.nsocket.NetSocket;
import gtcloud.common.nsocket.NetSocketException;
import gtcloud.common.nsocket.TCP.ClientSocket;
import gtcloud.common.nsocket.TCP.ClientSocketEventListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TcpClientSocketImpl implements ClientSocket {

    private static Logger LOG = LoggerFactory.getLogger(TcpClientSocketImpl.class);

    private ClientSocketEventListener listener = null;
    private NetSocketManagerImpl sockman = null;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new DaemonThreadFactory());
    private volatile ChannelFuture connectFuture = null;
    private volatile SocketChannel socketChannel = null;

    private int socketId = 0;
    private String serverIp;
    private int serverPort;
    private String remoteAddress = "N/A";
    private String localAddress = "N/A";
    private boolean autoReconnect;

    // "到服务器的连接是否建立状态判断"相关字段
    private final Lock connReadyLock = new ReentrantLock();
    private final Condition connReadyCond  = connReadyLock.newCondition();
    private AtomicBoolean connReadyFlag = new AtomicBoolean(false);

    // 连接是否已经被关闭
    private AtomicBoolean closedFlag = new AtomicBoolean(false);

    protected TcpClientSocketImpl(
            NetSocketManagerImpl sockman,
            String server_addr,
            ClientSocketEventListener listener,
            boolean auto_reconnect,
            Options options) throws NetSocketException {

        if (listener == null) {
            throw new IllegalArgumentException("client socket event listener cannot be null");
        }

        this.sockman = sockman;
        this.socketId = this.sockman.getNextSocketId();
        this.listener = listener;
        this.autoReconnect = auto_reconnect;

        NetUtils.AddressParam ap = NetUtils.AddressParam.parse(server_addr);
        this.serverIp = ap.host;
        this.serverPort = ap.port;

        // 发起到server的连接
        this.tryConnectToServer();
    }

    @Override
    public synchronized String getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public synchronized String getRemoteAddress() {
        return this.remoteAddress;
    }

    @Override
    public int getSocketId() {
        return socketId;
    }

    @Override
    public int getSocketType() {
        return NetSocket.TYPE_TCP_STREAM_CLIENT;
    }

    @Override
    public void waitTillServerConnected(long timeoutMillis)
            throws TimeoutException, NetSocketException {

        if (this.closedFlag.get()) {
            throw new NetSocketException("socket has been closed");
        }

        if (this.connReadyFlag.get()) {
            // connection established already
            return;
        }

        try {
            this.connReadyLock.lock();
            if (timeoutMillis < 0) {
                waitForConnReady1();
            }
            else {
                waitForConnReady2(timeoutMillis);
            }
        }
        finally {
            this.connReadyLock.unlock();
        }
    }

    private void waitForConnReady1() {
        while (!this.connReadyFlag.get()) {
            this.connReadyCond.awaitUninterruptibly();
        }
    }

    private void waitForConnReady2(long timeoutMillis) throws TimeoutException {
        long left = timeoutMillis;
        while (!this.connReadyFlag.get()) {
            final long t0 = System.currentTimeMillis();
            boolean ok = waitConditionUninterruptibly(this.connReadyCond, left);
            if (ok) {
                return;
            }
            final long t1 = System.currentTimeMillis();
            left -= (t1 - t0);
            if (left <= 0) {
                throw new TimeoutException();
            }
        }
    }

    private void signalConnReady(boolean b) {
        this.connReadyLock.lock();
        try {
            this.connReadyFlag.set(b);
            this.connReadyCond.signalAll();
        }
        finally {
            this.connReadyLock.unlock();
        }
    }

    private static boolean waitConditionUninterruptibly(Condition c, long timeoutMillis) {
        try {
            return c.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public void writeOneMessage(NetIoMessage message, long timeoutMillis)
            throws TimeoutException, NetSocketException {
        waitTillServerConnected(timeoutMillis);

        SocketChannel ch;
        synchronized (this) {
            ch = this.socketChannel;
        }
        if (ch != null) {
            ch.writeAndFlush(message);
        }
    }

    @Override
    public synchronized void close() {

        this.closedFlag.set(true);

        if (this.socketChannel != null) {
            this.socketChannel.close();
            this.socketChannel = null;
        }

        if (this.connectFuture != null) {
            this.connectFuture.cancel(true);
            try {
                this.connectFuture.await(1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            this.connectFuture = null;
        }

        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully();
            this.eventLoopGroup = null;
        }
    }

    private synchronized void tryConnectToServer() {
        Bootstrap b = new Bootstrap();
        b.group(this.eventLoopGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

        final TcpClientSocketImpl self = this;
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                self.initSocketChannel(ch);
            }
        });

        LOG.info("Try connect to " + this.serverIp + ":" + this.serverPort + "...");
        ChannelFuture f = b.connect(this.serverIp, this.serverPort);

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone() && future.cause() != null) {
                    LOG.error("connect to server failed: " + future.cause().getMessage());
                    if (self.autoReconnect) {
                        synchronized(self) {
                            self.socketChannel = null;
                            self.connectFuture = null;
                            self.reconnectToServer_i();
                        }
                    }
                }
            }
        });

        this.connectFuture = f;
    }

    private void initSocketChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new NetIoMessageDecoder());
        p.addLast(new NetIoMessageEncoder());
        p.addLast(new ClientChannelInboundHandler(this));
    }

    private void reconnectToServer_i() {
        if (this.closedFlag.get()) {
            // close() called
            return;
        }
        final TcpClientSocketImpl self = this;
        this.eventLoopGroup.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(5);
                    if (!self.closedFlag.get()) {
                        self.tryConnectToServer();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private synchronized void afterServerConnected(SocketChannel ch) {
        this.remoteAddress = ch.remoteAddress().toString();
        this.localAddress = ch.localAddress().toString();
        this.socketChannel = ch;

        LOG.info("connection to server " + this.remoteAddress + " established.");
        signalConnReady(true);
        this.listener.afterServerConnected(this);
    }

    private synchronized void afterServerDisconnected() {
        LOG.info("connection to server " + this.remoteAddress + " lost.");
        signalConnReady(false);
        this.listener.afterServerDisconnected(this);

        this.socketChannel = null;
        this.connectFuture = null;
        if (this.autoReconnect) {
            this.reconnectToServer_i();
        }
    }

    private static class ClientChannelInboundHandler extends ChannelInboundHandlerAdapter {

        TcpClientSocketImpl self;

        ClientChannelInboundHandler(TcpClientSocketImpl self) {
            this.self = self;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.self.afterServerConnected((SocketChannel)ctx.channel());
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            this.self.afterServerDisconnected();
            ctx.fireChannelInactive();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NetIoMessage m = (NetIoMessage)msg;
            self.listener.afterServerMessageReceived(self, m);
            //ctx.fireChannelRead(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            LOG.error("exceptionCaught(): " + cause);
            ctx.close();
            //ctx.fireExceptionCaught(cause);
        }
    }
}
