package gtcloud.common.nsocket;

import platon.ByteStream;

public class NetIoMessage {

    // 服务调用请求/应答的格式
    public static final byte WIRE_FORMAT_BLOB = 1;
    public static final byte WIRE_FORMAT_JSON = 2;
    
    static final int NETIO_MSG_LENGTH_FIELD_LEN     = 4;
    static final int NETIO_MSG_WIREFORMAT_FIELD_LEN = 1;
    static final int NETIO_MSG_DOMAINID_FIELD_LEN   = 1;    
    static final int NETIO_MSG_FUNCID_FIELD_LEN     = 2;
    static final int NETIO_MSG_HEADER_LEN           = 8; // 上述4个之和

    // 打包格式
    private byte wireFormat = 0;
    
    // 功能域标识
    private byte domainId = 0;

    // 功能标识
    private short functionId = 0;

    // 消息正文
    private ByteStream body = null;

    public NetIoMessage() {}
    
    public NetIoMessage(byte domainId, short functionId, ByteStream body) {
    	this(WIRE_FORMAT_BLOB, domainId, functionId, body);
    }

    public NetIoMessage(byte wireFormat, byte domainId, short functionId, ByteStream body) {
    	this.wireFormat = wireFormat;
        this.domainId = domainId;
        this.functionId = functionId;
        this.body = body;
    }
    
	public byte getDomainId() {
		return this.domainId;
	}

	public void setDomainId(byte domainId) {
		this.domainId = domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = (byte)domainId;
	}
	
	public short getFunctionId() {
		return this.functionId;
	}

	public void setFunctionId(short functionId) {
		this.functionId = functionId;
	}

	public void setFunctionId(int functionId) {
		this.functionId = (short)functionId;
	}
	
	public ByteStream getBody() {
		if (this.body == null) {
			this.body = new ByteStream();
		}
		return this.body;
	}

	public void setBody(ByteStream body) {
		this.body = body;
	}

	public byte getWireFormat() {
		return wireFormat;
	}

	public void setWireFormat(byte wireFormat) {
		this.wireFormat = wireFormat;
	}
}
