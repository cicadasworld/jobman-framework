package gtcloud.common.nsocket.netty;

import java.util.List;

import gtcloud.common.nsocket.NetIoMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import platon.ByteStream;

class NetIoMessageDecoder extends ByteToMessageDecoder {

    private final byte[] work_buffer = new byte[2048];

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
            List<Object> objs) throws Exception {

        final int PREFIX_LEN = 4 + 2 + 2;
        int avail = in.readableBytes();
        while (avail > PREFIX_LEN) {
            in.markReaderIndex();
            in.readBytes(work_buffer, 0, PREFIX_LEN);
            avail -= PREFIX_LEN;

            ByteStream bs = new ByteStream(work_buffer, 0, PREFIX_LEN);
            int bodyLen = bs.readInt();
            if (avail >= bodyLen) {
                NetIoMessage msg = new NetIoMessage();
                msg.setWireFormat(bs.readByte());
                msg.setDomainId(bs.readByte());
                msg.setFunctionId(bs.readShort());
                read_n(msg.getBody(), in, bodyLen);
                avail -= bodyLen;
                objs.add(msg);
            }
            else {
                // 不足一个报文
                in.resetReaderIndex();
                return;
            }
        }
    }

    private void read_n(ByteStream out, ByteBuf in, int n) {
        int nleft = n;
        while (nleft > 0) {
            int nread = Math.min(nleft, this.work_buffer.length);
            in.readBytes(this.work_buffer, 0, nread);
            out.writeBytes(this.work_buffer, 0, nread);
            nleft -= nread;
        }
    }
}
