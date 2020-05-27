package platon;

import java.io.ByteArrayInputStream;
import java.util.BitSet;

public class Message implements Freezable {

    public static final int TAG_TYPE_VARINT32     = 0; // boolean, byte, short, integer
    public static final int TAG_TYPE_FIXED32      = 1; // float
    public static final int TAG_TYPE_FIXED64      = 2; // long, double
    public static final int TAG_TYPE_LEN_PREFIXED = 3; // string: length(Uvarint32) + [utf8-bytes]
    public static final int TAG_TYPE_EMBEDDEDMSG  = 4; // embedded message: length(fixed32Int) + [message-bytes]

    private static final int TAG_TYPE_BITS = 3;
    private static final int TAG_TYPE_MAX  = (1 << TAG_TYPE_BITS) - 1;
    private static final int TAG_ID_BITS   = 28;
    private static final int TAG_ID_MAX    = (1 << TAG_ID_BITS) - 1;

    private BitSet _bits = new BitSet(8);

    @Override
    public void copyFrom(Freezable from) {
        Message m = (Message)from;
        this._bits = (BitSet)m._bits.clone();
    }

    // 获得消息中包含了哪些字段，返回字段ID列表;
    // 返回消息包含的字段个数
    public int[] getFieldList() {
        int[] result = new int[_bits.cardinality()];
        int j = 0;
        for (int i=0; i<_bits.length(); ++i) {
            if (_bits.get(i)) {
                result[j++] = i;
            }
        }
        return result;
    }


    // 判断给定的字段是否包含在消息中。
    public boolean hasField(int fieldId) {
        return _bits.get(fieldId);
    }

    // 设置当前消息包含的字段列表
    public void addFields(int[] fieldIdList)    {
        for (int fid : fieldIdList) {
            addField(fid);
        }
    }

    // 清空当前消息包含的所有字段
    public void clearFields()   {
        _bits.clear();
    }

    // 设置当前消息包含的单个字段
    public void addField(int fieldId) {
        _bits.set(fieldId);
    }

    // 从当前消息中清除给定的字段
    public void clearField(int fieldId) {
        if (_bits.get(fieldId)) {
            _bits.set(fieldId);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    // cookieOut: int[2]
    public static void beginFreeze(ByteStream output, int[] cookieOut)  {
        // write a length-placer-holder
        final int placer_pos = output.write_pos();
        output.writeInt(0);

        final int begin_pos = output.write_pos();
        cookieOut[0] = placer_pos;
        cookieOut[1] = begin_pos;
    }

    // cookieIn: int[2]
    public static void endFreeze(ByteStream output, int[] cookieIn) {
        final int placer_pos = cookieIn[0];
        final int begin_pos = cookieIn[1];
        final int current_pos = output.write_pos();
        final int messageLen = current_pos - begin_pos;
        if (messageLen > 0) {
            output.write_pos(placer_pos);
            output.writeInt(messageLen);
            output.write_pos(current_pos);
        }
    }

    // return bytesLength of the incoming message
    public static int beginDefreeze(ByteStream input) throws DefreezeException {
        return input.readInt();
    }

    public static void endDefreeze(ByteStream input) throws DefreezeException {
        // NOOP SO FAR
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static void writeTag(ByteStream output, int tagType, int tagId) throws FreezeException {
        assert(tagId >= 0  && tagId <= TAG_ID_MAX);
        assert(tagType >= 0  && tagType <= TAG_TYPE_MAX);

        int v = (tagId << TAG_TYPE_BITS) | (tagType);
        if (v < 0) {
            throw new FreezeException();
        }

        output.writeVarint32(v);
    }

    // return int[2], int[0] is tagType, int[1] is tagId
    public static int[] readTag(ByteStream input) throws DefreezeException {
        int v = input.readVarint32();
        int tagType = v & TAG_TYPE_MAX;
        int tagId = (v >> TAG_TYPE_BITS);
        assert(tagId >= 0  && tagId <= TAG_ID_MAX);
        assert(tagType >= 0  && tagType <= TAG_TYPE_MAX);
        return new int[] {tagType, tagId};
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static void writeFieldBool(ByteStream output, int fieldId, boolean b) throws FreezeException {
        int n = b ? 1 : 0;
        writeFieldInt(output, fieldId, n);
    }

    public static void writeFieldByte(ByteStream output, int fieldId, byte v) throws FreezeException {
        int n = v;
        writeFieldInt(output, fieldId, n);
    }

    public static void writeFieldShort(ByteStream output, int fieldId, short v) throws FreezeException {
        int n = v;
        writeFieldInt(output, fieldId, n);
    }

    public static void writeFieldInt(ByteStream output, int fieldId, int v) throws FreezeException {
        writeTag(output, TAG_TYPE_VARINT32, fieldId);
        output.writeVarint32(v);
    }

    public static void writeFieldLong(ByteStream output, int fieldId, long v) throws FreezeException {
        writeTag(output, TAG_TYPE_FIXED64, fieldId);
        output.writeLong(v);
    }

    public static void writeFieldFloat(ByteStream output, int fieldId, float v) throws FreezeException {
        writeTag(output, TAG_TYPE_FIXED32, fieldId);
        output.writeFloat(v);
    }

    public static void writeFieldDouble(ByteStream output, int fieldId, double v) throws FreezeException {
        writeTag(output, TAG_TYPE_FIXED64, fieldId);
        output.writeDouble(v);
    }

    public static void writeFieldString(ByteStream output, int fieldId, final String v) throws FreezeException {
        writeTag(output, TAG_TYPE_LEN_PREFIXED, fieldId);
        output.writeString(v);
    }

    public static void writeFieldObject(ByteStream output, int fieldId, final Freezable obj) throws FreezeException {
        writeTag(output, TAG_TYPE_EMBEDDEDMSG, fieldId);
        obj.freeze(output);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static boolean readFieldBool(ByteStream input) throws DefreezeException {
        int n = readFieldInt(input);
        return (n == 1);
    }

    public static byte readFieldByte(ByteStream input) throws DefreezeException {
        int n = readFieldInt(input);
        return (byte)n;
    }

    public static short readFieldShort(ByteStream input) throws DefreezeException {
        int n = readFieldInt(input);
        return (short)n;
    }

    public static int readFieldInt(ByteStream input) throws DefreezeException {
        return input.readVarint32();
    }

    public static long readFieldLong(ByteStream input) throws DefreezeException {
        return input.readLong();
    }

    public static float readFieldFloat(ByteStream input) throws DefreezeException {
        return input.readFloat();
    }

    public static double readFieldDouble(ByteStream input) throws DefreezeException {
        return input.readDouble();
    }

    public static String readFieldString(ByteStream input) throws DefreezeException {
        return input.readString();
    }

    public static void readFieldObject(ByteStream input, Freezable obj) throws DefreezeException {
        obj.defreeze(input);
    }

    ////////////////////////////////////////////////////////////////////////////////

    // 根据tagType丢弃输入流中对应的内容
    public static void discardBytes(ByteStream input, int tagType) throws DefreezeException {
        if (tagType == TAG_TYPE_VARINT32) {
            input.readVarint32();
            return;
        }

        int nlenToDiscard = 0;

        if (tagType == TAG_TYPE_FIXED32) {
            nlenToDiscard = 4;
        }
        else if (tagType == TAG_TYPE_FIXED64) {
            nlenToDiscard = 8;
        }
        else if (tagType == TAG_TYPE_LEN_PREFIXED) {
            nlenToDiscard = input.readVarint32();
        }
        else if (tagType == TAG_TYPE_EMBEDDEDMSG) {
            nlenToDiscard = input.readInt();
        }
        else {
            assert(false) : "unknown tag";
            throw new DefreezeException("unknown tag");
        }

        final int current_pos = input.read_pos();
        final int left = input.length() - current_pos;
        if (left < nlenToDiscard) {
            throw new DefreezeException("not enougth bytes");
        }
        input.read_pos(current_pos + nlenToDiscard);
    }

    @Override
    public void defreeze(ByteStream input) throws DefreezeException {
        // NOOP SO FAR
    }

    @Override
    public void freeze(ByteStream output) throws FreezeException {
        // NOOP SO FAR
    }

    @Override
    public Freezable makeClone() {
        Message m = new Message();
        m.copyFrom(this);
        return m;
    }

    public static boolean isTypeEqual(String typeName1, String typeName2) {
        if (null == typeName1 || null == typeName2) {
            return false;
        }
        if (typeName1.equals(typeName2)) {
            return true;
        }

        final int CPP_STYLE = 1;
        final int JAVA_STYLE = 2;

        int style1 = -1;
        style1 = typeName1.indexOf(':') > 0 ? CPP_STYLE : (-1);
        if (style1 < 0) {
            style1 = typeName1.indexOf('.') > 0 ? JAVA_STYLE : (-1);
        }

        int style2 = -1;
        style2 = typeName2.indexOf(':') > 0 ? CPP_STYLE : (-1);
        if (style2 < 0) {
            style2 = typeName2.indexOf('.') > 0 ? JAVA_STYLE : (-1);
        }

        if (style1 == style2) {
            return typeName1.equals(typeName2);
        }
        else if (style1 == CPP_STYLE && style2 == JAVA_STYLE) {
            String t1 = typeName1.replace("::", ".");
            return t1.equals(typeName2);
        }
        else {
            String t2 = typeName2.replace("::", ".");
            return typeName1.equals(t2);
        }
    }

    public static void formatToJsonText(FreezerJSON msg,
            ByteStream output,
            String charsetName,
            int indentFactor,
            int indent) throws Exception {
        final JsonNode jsonNode = msg.freezeToJSON();
        final String jsonText = jsonNode.toPrettyString(indentFactor, indent);
        if (charsetName == null) {
            byte[] vec = jsonText.getBytes();
            output.writeBytes(vec, 0, vec.length);
        } else {
            byte[] vec = jsonText.getBytes(charsetName);
            output.writeBytes(vec, 0, vec.length);
        }
    }

    public static void formatToJsonText(FreezerJSON msg,
            ByteStream output,
            String charsetName) throws Exception {
        formatToJsonText(msg, output, charsetName, 0, 0);
    }

    public static void parseFromJsonText(FreezerJSON msg,
            ByteStream input,
            String charsetName) throws Exception {

        // 在buf[begin..end)中搜寻JSON-Object或JSON-Array, 返回JSON-Object或JSON-Array
        // 对应的区间[position[0]..position[1]).
        byte[] buf = input.array();
        final int oldPos = input.read_pos();
        final int length = input.length() - oldPos;
        int[] position = new int[] {0, 0};
        JsonNode.scanJsonEntity(buf, oldPos, oldPos+length, position);

        final int jsonLen = position[1] - position[0];
        ByteArrayInputStream is = new ByteArrayInputStream(buf, position[0], jsonLen);
        JsonNode inputNode = JsonNode.parseJsonDoc(is, charsetName);

        final int cosumedLen = position[1] - oldPos;
        input.read_pos(oldPos + cosumedLen);

        msg.defreezeFromJSON(inputNode);
    }
}
