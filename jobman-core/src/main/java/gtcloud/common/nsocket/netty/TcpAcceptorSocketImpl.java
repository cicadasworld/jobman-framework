package gtcloud.common.nsocket.netty;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.Options;
import gtcloud.common.utils.NetUtils;
import gtcloud.common.nsocket.NetIoMessage;
import gtcloud.common.nsocket.NetSocket;
import gtcloud.common.nsocket.NetSocketException;
import gtcloud.common.nsocket.TCP.AcceptorSocket;
import gtcloud.common.nsocket.TCP.ClientInfo;
import gtcloud.common.nsocket.TCP.ServerSocketEventListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import platon.ByteStream;

class TcpAcceptorSocketImpl implements AcceptorSocket {

    private static Logger LOG = LoggerFactory.getLogger(TcpAcceptorSocketImpl.class);

    private ServerSocketEventListener listener = null;
    private NetSocketManagerImpl sockman = null;
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DaemonThreadFactory());
    private volatile ServerSocketChannel acceptorChannel = null;

    private ConcurrentHashMap<Integer, TcpServerSocketImpl> clients =
        new ConcurrentHashMap<Integer, TcpServerSocketImpl>(7);

    private int socketId = 0;
    private String localAddress = "N/A";

    TcpAcceptorSocketImpl(NetSocketManagerImpl sockman,
                          String listen_addr,
                          ServerSocketEventListener listener,
                          Options options) throws NetSocketException {

        if (listener == null) {
            throw new IllegalArgumentException("server socket event listener cannot be null");
        }

        this.sockman = sockman;
        this.socketId = this.sockman.getNextSocketId();
        this.listener = listener;

        ServerBootstrap b = new ServerBootstrap();
        b.group(this.bossGroup, this.sockman.getNetIoWorkerEventLoopGroup());
        b.channel(NioServerSocketChannel.class);
        b.option(ChannelOption.SO_BACKLOG, 100);

        final TcpAcceptorSocketImpl self = this;
        b.childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new NetIoMessageDecoder());
                 p.addLast(new NetIoMessageEncoder());
                 p.addLast(new ServerChannelInboundHandler(self));
             }
         });

        // Start the server.
        NetUtils.AddressParam ap = NetUtils.AddressParam.parse(listen_addr);
        ChannelFuture f = b.bind(ap.host, ap.port);

        final CountDownLatch bindLatch = new CountDownLatch(1);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bindLatch.countDown();
            }
        });

        // µÈ´ýbind()Íê³É
        if (!f.isDone()) {
            try {
                bindLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (f.isDone() && f.cause() != null) {
            LOG.error("unable to create acceptor-socket: " + f.cause().getMessage());
            this.close();
            throw new NetSocketException(f.cause().getMessage());
        }

        this.acceptorChannel = (ServerSocketChannel)f.channel();
        this.localAddress = f.channel().localAddress().toString();
        LOG.info("waiting connection from client at: " + this.localAddress);
    }

    @Override
    public String getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public String getRemoteAddress() {
        return "N/A";
    }

    @Override
    public int getSocketId() {
        return this.socketId;
    }

    @Override
    public int getSocketType() {
        return NetSocket.TYPE_TCP_ACCEPTOR;
    }

    @Override
    public ArrayList<ClientInfo> getConnectedClients() {
        ArrayList<ClientInfo> vec = new ArrayList<ClientInfo>(this.clients.size());
        for (TcpServerSocketImpl sock : this.clients.values()) {
            ClientInfo ci = new ClientInfo();
            ci.addr = sock.getRemoteAddress();
            ci.sockId = sock.getSocketId();
            ci.stat = sock.getStat();
            vec.add(ci);
        }
        return vec;
    }

    @Override
    public synchronized void close() {
        if (this.acceptorChannel != null) {
            this.acceptorChannel.close();
            this.acceptorChannel = null;
        }

        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
            this.bossGroup = null;
        }
    }

    private void afterClientConnected(TcpServerSocketImpl sock) {
        LOG.info("accepted a client from " + sock.getRemoteAddress());
        this.clients.put(sock.getSocketId(), sock);
        this.listener.afterClientConnected(sock);
    }

    private void afterClientDisconnected(TcpServerSocketImpl sock) {
        LOG.info("disconnected a client from " + sock.getRemoteAddress());
        this.listener.afterClientDisconnected(sock);
        this.clients.remove(sock.getSocketId());
    }

    private void afterClientMessageReceived(TcpServerSocketImpl sock,
            NetIoMessage req, NetIoMessage ack) {
        this.listener.afterClientMessageReceived(sock, req, ack);
    }

    private static final AttributeKey<TcpServerSocketImpl> SOCK_KEY =
        AttributeKey.valueOf("SERVER_CHANNEL_OBJ");

    private static class ServerChannelInboundHandler extends ChannelInboundHandlerAdapter {

        TcpAcceptorSocketImpl acceptor;

        ServerChannelInboundHandler(TcpAcceptorSocketImpl acceptor) {
            this.acceptor = acceptor;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            SocketChannel ch = (SocketChannel)ctx.channel();
            assert ch.attr(SOCK_KEY).get() == null;
            final int socketId = this.acceptor.sockman.getNextSocketId();
            TcpServerSocketImpl sock = new TcpServerSocketImpl(socketId, ch);
            this.acceptor.afterClientConnected(sock);
            ch.attr(SOCK_KEY).set(sock);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            SocketChannel ch = (SocketChannel)ctx.channel();
            TcpServerSocketImpl sock = ch.attr(SOCK_KEY).get();
            assert sock != null;
            if (sock != null) {
                this.acceptor.afterClientDisconnected(sock);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            SocketChannel ch = (SocketChannel)ctx.channel();
            TcpServerSocketImpl sock = ch.attr(SOCK_KEY).get();
            assert sock != null;
            if (sock == null) {
                return;
            }

            NetIoMessage req = (NetIoMessage)msg;
            NetIoMessage ack = new NetIoMessage();
            sock.updateRecvStat(req);
            this.acceptor.afterClientMessageReceived(sock, req, ack);

            ByteStream body = ack.getBody();
            if (body != null && (body.length() - body.read_pos()) > 0) {
                ch.writeAndFlush(ack);
                sock.updateSendStat(ack);
            }
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
