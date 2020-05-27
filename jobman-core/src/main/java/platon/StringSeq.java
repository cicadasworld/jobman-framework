// Java file generated automatically from `stringseq.fzdl'.
// $Timestamp: 2016-06-20 21:49:14
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package platon;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("serial")
public class StringSeq extends ArrayList<String> implements Freezable, FreezerJSON
{
    private static final String NULL_ELEM = new String();

    public StringSeq() {
        // EMPTY
    }

    public StringSeq(Collection<String> collection) {
        super(collection);
    }

    public StringSeq(int initialCapacity) {
        super(initialCapacity);
    }

    static private String deepCopyElem(String srcElem) {
        return srcElem;
    }

    // 从给定对象拷贝数据到当前对象
    @Override
    public void copyFrom(Freezable other) {
        StringSeq from = (StringSeq)other;
        this.clear();
        for (String src : from) {
            String e = deepCopyElem(src);
            this.add(e);
        }
    }

    // 克隆当前对象。
    @Override
    public Freezable makeClone() {
        StringSeq obj = new StringSeq();
        obj.copyFrom(this);
        return obj;
    }

    // 将当前对象打包进给定的字节流中。
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        Message.beginFreeze(output, cookie);

        final int sz = this.size();
        output.writeSize(sz);
        for (String elem : this) {
            if (elem == null) {
                elem = NULL_ELEM;
            }
            freezeElem(elem, output);
        } //for

        Message.endFreeze(output, cookie);
    }

    // 从给定字节流中解包, 并将解包得到的数据覆盖到当前对象上。
    @Override
    public void defreeze(ByteStream input) throws DefreezeException {
        this.clear();
        Message.beginDefreeze(input);

        int sz = input.readSize();
        assert(sz >= 0);
        if (sz > 0) {
            this.ensureCapacity(sz);
            for (int i = 0; i < sz; i++) {
                String elem = defreezeElem(input);
                this.add(elem);
            }
        }

        Message.endDefreeze(input);
    }

    static private void freezeElem(String elem, ByteStream output) throws FreezeException {
        output.writeString(elem);
    }

    static private String defreezeElem(ByteStream input) throws DefreezeException {
        return input.readString();
    }

    // 将当前消息打包成JSON树, 返回JSON树的指针
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonArray();
        for (String elem : this) {
            if (elem == null) {
                thisNode.append(JsonNode.NULL_NODE);
            } else {
                thisNode.append(new JsonNode(elem));
            }
        }
        return thisNode;
    }

    // 从给定JSON树中解包, 并将解包得到的数据覆盖到当前消息上。
    @Override
    public void defreezeFromJSON(JsonNode inputJsonNode) throws DefreezeException {
        if (!inputJsonNode.isArray()) {
            throw new DefreezeException("input json is not array");
        }
        this.clear();
        final int nlen = inputJsonNode.size();
        for (int i=0; i<nlen; ++i) {
            JsonNode j = inputJsonNode.get(i);
            String elem = j.isNull() ? null : j.asString();
            this.add(elem);
        }
    }
}

