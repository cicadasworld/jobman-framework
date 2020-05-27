// Java file generated automatically from `autogen.fzdl'.
// $Timestamp: 2018-11-28 18:08:44
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package gtcloud.common.cynosure.pdo;

import platon.ByteStream;
import platon.DefreezeException;
import platon.Freezable;
import platon.FreezerJSON;
import platon.FreezeException;
import platon.JsonNode;
import platon.Message;

public class ParamDO extends Message implements FreezerJSON
{
    public static final int FIELD_ID_NAME = 0;
    public static final int FIELD_ID_VALUES = 1;
    public static final int FIELD_ID_UPDATESEQ = 2;

    private String name = null;
    private platon.StringSeq values = null;
    private int updateSeq = 0;

    public ParamDO() {
        addField(FIELD_ID_NAME);
        if (this.values != null) {
            addField(FIELD_ID_VALUES);
        }
        addField(FIELD_ID_UPDATESEQ);
    }

    public ParamDO(ParamDO other) {
        this.copyFrom(other);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String val) {
        this.name = val;
    }

    public platon.StringSeq getValues() {
        if (this.values == null) {
            this.values = new platon.StringSeq();
            this.addField(FIELD_ID_VALUES);
        }
        return this.values;
    }

    public int getUpdateSeq() {
        return this.updateSeq;
    }

    public void setUpdateSeq(int val) {
        this.updateSeq = val;
    }


    // �Ӹ������󿽱����ݵ���ǰ����
    @Override
    public void copyFrom(Freezable other) {
        super.copyFrom(other);
        ParamDO from = (ParamDO)other;
        this.name = from.name;
        if (this.values == null) {
            this.values = new platon.StringSeq();
        }
        this.values.copyFrom(from.getValues());
        this.updateSeq = from.updateSeq;
    }

    // ��¡��ǰ����
    @Override
    public Freezable makeClone() {
        ParamDO o = new ParamDO();
        o.copyFrom(this);
        return o;
    }

    public void clear() {
        this.name = null;
        this.values = null;
        this.updateSeq = 0;
        this.clearFields();
    }

    // ����ǰ���������������ֽ����С�
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        beginFreeze(output, cookie);

        if (hasField(FIELD_ID_NAME)) {
            writeFieldString(output, FIELD_ID_NAME, this.name);
        }

        if (hasField(FIELD_ID_VALUES)) {
            writeFieldObject(output, FIELD_ID_VALUES, this.values);
        }

        if (hasField(FIELD_ID_UPDATESEQ)) {
            writeFieldInt(output, FIELD_ID_UPDATESEQ, this.updateSeq);
        }

        endFreeze(output, cookie);
    }

    // �Ӹ����ֽ����н��, ��������õ������ݸ��ǵ���ǰ�����ϡ�
    @Override
    public void defreeze(ByteStream input) throws DefreezeException {
        clear();

        int messageLen = beginDefreeze(input);
        int lastPos = input.read_pos();
        while (messageLen > 0) {
            int[] tuple = readTag(input);
            int tagType = tuple[0];
            int fieldId = tuple[1];

            boolean consumed = false;
            switch (fieldId) {
            case FIELD_ID_NAME:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.name = readFieldString(input);
                    addField(FIELD_ID_NAME);
                    consumed = true;
                }
                break;

            case FIELD_ID_VALUES:
                if (tagType == TAG_TYPE_EMBEDDEDMSG) {
                    if (this.values == null) {
                        this.values = new platon.StringSeq();
                    }
                    readFieldObject(input, this.values);
                    addField(FIELD_ID_VALUES);
                    consumed = true;
                }
                break;

            case FIELD_ID_UPDATESEQ:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.updateSeq = readFieldInt(input);
                    addField(FIELD_ID_UPDATESEQ);
                    consumed = true;
                }
                break;
            }

            if (!consumed) {
                // unknown field, ignore it
                discardBytes(input, tagType);
            }

            final int currentPos = input.read_pos();
            messageLen -= (currentPos - lastPos);
            lastPos = currentPos;
        }

        endDefreeze(input);
    }

    // ����ǰ��Ϣ�����JSON��, ����JSON����ָ��
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonObject();

        if (hasField(FIELD_ID_NAME) && this.name != null) {
            JsonNode val_c_name = new JsonNode(this.name);
            thisNode.put("name", val_c_name);
        }

        if (hasField(FIELD_ID_VALUES) && this.values != null) {
            JsonNode val_c_values = this.values.freezeToJSON();
            thisNode.put("values", val_c_values);
        }

        if (hasField(FIELD_ID_UPDATESEQ)) {
            JsonNode val_c_updateSeq = new JsonNode(this.updateSeq);
            thisNode.put("updateSeq", val_c_updateSeq);
        }
        return thisNode;
    }

    // �Ӹ���JSON���н��, ��������õ������ݸ��ǵ���ǰ��Ϣ�ϡ�
    @Override
    public void defreezeFromJSON(JsonNode inputJsonNode) throws DefreezeException {
        if (!inputJsonNode.isObject()) {
            throw new DefreezeException("input json is not object");
        }
        clear();

        JsonNode val_c_name = inputJsonNode.get("name");
        if (val_c_name != null && !val_c_name.isNull()) {
            this.name = val_c_name.asString();
            addField(FIELD_ID_NAME);
        }

        JsonNode val_c_values = inputJsonNode.get("values");
        if (val_c_values != null && !val_c_values.isNull()) {
            this.getValues().defreezeFromJSON(val_c_values);
            addField(FIELD_ID_VALUES);
        }

        JsonNode val_c_updateSeq = inputJsonNode.get("updateSeq");
        if (val_c_updateSeq != null && !val_c_updateSeq.isNull()) {
            this.updateSeq = (int)val_c_updateSeq.asInt64();
            addField(FIELD_ID_UPDATESEQ);
        }
    }
}
