package gtcloud.common.nsocket.netty;

import gtcloud.common.nsocket.NetIoMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import platon.ByteStream;

class NetIoMessageEncoder extends MessageToByteEncoder<NetIoMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NetIoMessage msg, ByteBuf buf) throws Exception {
        int bodyLen = 0;
        ByteStream body = msg.getBody();
        if (body != null) {
            bodyLen = body.length() - body.read_pos();
        }

        ByteStream prefix = new ByteStream(4 + 2 + 2);
        prefix.writeInt(bodyLen);
        prefix.writeByte(msg.getWireFormat());
        prefix.writeByte(msg.getDomainId());
        prefix.writeShort(msg.getFunctionId());

        buf.writeBytes(prefix.array());
        if (bodyLen > 0) {
        	buf.writeBytes(body.array(), body.read_pos(), bodyLen);
        }
    }
}
