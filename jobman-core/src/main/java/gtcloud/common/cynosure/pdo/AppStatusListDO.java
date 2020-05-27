// Java file generated automatically from `autogen.fzdl'.
// $Timestamp: 2019-04-29 09:36:31
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package gtcloud.common.cynosure.pdo;

import java.util.ArrayList;
import java.util.Collection;
import platon.ByteStream;
import platon.DefreezeException;
import platon.Freezable;
import platon.FreezerJSON;
import platon.FreezeException;
import platon.JsonNode;
import platon.Message;

@SuppressWarnings("serial")
public class AppStatusListDO extends ArrayList<AppStatusDO> implements Freezable, FreezerJSON
{
    private static final AppStatusDO NULL_ELEM = new AppStatusDO();

    public AppStatusListDO() {
        // EMPTY
    }

    public AppStatusListDO(Collection<AppStatusDO> collection) {
        super(collection);
    }

    public AppStatusListDO(int initialCapacity) {
        super(initialCapacity);
    }

    static private AppStatusDO deepCopyElem(AppStatusDO srcElem) {
        return (AppStatusDO)srcElem.makeClone();
    }

    // �Ӹ������󿽱����ݵ���ǰ����
    @Override
    public void copyFrom(Freezable other) {
        AppStatusListDO from = (AppStatusListDO)other;
        this.clear();
        for (AppStatusDO src : from) {
            AppStatusDO e = deepCopyElem(src);
            this.add(e);
        }
    }

    // ��¡��ǰ����
    @Override
    public Freezable makeClone() {
        AppStatusListDO obj = new AppStatusListDO();
        obj.copyFrom(this);
        return obj;
    }

    // ����ǰ���������������ֽ����С�
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        Message.beginFreeze(output, cookie);

        final int sz = this.size();
        output.writeSize(sz);
        for (AppStatusDO elem : this) {
            if (elem == null) {
                elem = NULL_ELEM;
            }
            freezeElem(elem, output);
        } //for

        Message.endFreeze(output, cookie);
    }

    // �Ӹ����ֽ����н��, ��������õ������ݸ��ǵ���ǰ�����ϡ�
    @Override
    public void defreeze(ByteStream input) throws DefreezeException {
        this.clear();
        Message.beginDefreeze(input);

        int sz = input.readSize();
        assert(sz >= 0);
        if (sz > 0) {
            this.ensureCapacity(sz);
            for (int i = 0; i < sz; i++) {
                AppStatusDO elem = defreezeElem(input);
                this.add(elem);
            }
        }

        Message.endDefreeze(input);
    }

    static private void freezeElem(AppStatusDO elem, ByteStream output) throws FreezeException {
        elem.freeze(output);
    }

    static private AppStatusDO defreezeElem(ByteStream input) throws DefreezeException {
        AppStatusDO elem = new AppStatusDO();
        elem.defreeze(input);
        return elem;
    }

    // ����ǰ��Ϣ�����JSON��, ����JSON����ָ��
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonArray();
        for (AppStatusDO elem : this) {
            if (elem == null) {
                thisNode.append(JsonNode.NULL_NODE);
            } else {
                thisNode.append(elem.freezeToJSON());
            }
        }
        return thisNode;
    }

    // �Ӹ���JSON���н��, ��������õ������ݸ��ǵ���ǰ��Ϣ�ϡ�
    @Override
    public void defreezeFromJSON(JsonNode inputJsonNode) throws DefreezeException {
        if (!inputJsonNode.isArray()) {
            throw new DefreezeException("input json is not array");
        }
        this.clear();
        final int nlen = inputJsonNode.size();
        for (int i=0; i<nlen; ++i) {
            JsonNode j = inputJsonNode.get(i);
            if (j.isNull()) {
                this.add(null);
            } else {
                AppStatusDO elem = new AppStatusDO();
                elem.defreezeFromJSON(j);
                this.add(elem);
            }
        }
    }
}
