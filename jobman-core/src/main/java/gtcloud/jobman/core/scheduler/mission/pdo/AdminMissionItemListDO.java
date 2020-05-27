// Java file generated automatically from `mission.fzdl'.
// $Timestamp: 2019-07-19 09:40:49
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package gtcloud.jobman.core.scheduler.mission.pdo;

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
public class AdminMissionItemListDO extends ArrayList<AdminMissionItemDO> implements Freezable, FreezerJSON
{
    private static final AdminMissionItemDO NULL_ELEM = new AdminMissionItemDO();

    public AdminMissionItemListDO() {
        // EMPTY
    }

    public AdminMissionItemListDO(Collection<AdminMissionItemDO> collection) {
        super(collection);
    }

    public AdminMissionItemListDO(int initialCapacity) {
        super(initialCapacity);
    }

    static private AdminMissionItemDO deepCopyElem(AdminMissionItemDO srcElem) {
        return (AdminMissionItemDO)srcElem.makeClone();
    }

    // 从给定对象拷贝数据到当前对象
    @Override
    public void copyFrom(Freezable other) {
        AdminMissionItemListDO from = (AdminMissionItemListDO)other;
        this.clear();
        for (AdminMissionItemDO src : from) {
            AdminMissionItemDO e = deepCopyElem(src);
            this.add(e);
        }
    }

    // 克隆当前对象。
    @Override
    public Freezable makeClone() {
        AdminMissionItemListDO obj = new AdminMissionItemListDO();
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
        for (AdminMissionItemDO elem : this) {
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
                AdminMissionItemDO elem = defreezeElem(input);
                this.add(elem);
            }
        }

        Message.endDefreeze(input);
    }

    static private void freezeElem(AdminMissionItemDO elem, ByteStream output) throws FreezeException {
        elem.freeze(output);
    }

    static private AdminMissionItemDO defreezeElem(ByteStream input) throws DefreezeException {
        AdminMissionItemDO elem = new AdminMissionItemDO();
        elem.defreeze(input);
        return elem;
    }

    // 将当前消息打包成JSON树, 返回JSON树的指针
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonArray();
        for (AdminMissionItemDO elem : this) {
            if (elem == null) {
                thisNode.append(JsonNode.NULL_NODE);
            } else {
                thisNode.append(elem.freezeToJSON());
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
            if (j.isNull()) {
                this.add(null);
            } else {
                AdminMissionItemDO elem = new AdminMissionItemDO();
                elem.defreezeFromJSON(j);
                this.add(elem);
            }
        }
    }
}

