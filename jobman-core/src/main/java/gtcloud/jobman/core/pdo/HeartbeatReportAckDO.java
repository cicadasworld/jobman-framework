// Java file generated automatically from `schedpdo.fzdl'.
// $Timestamp: 2018-04-18 15:58:28
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package gtcloud.jobman.core.pdo;

import platon.ByteStream;
import platon.DefreezeException;
import platon.Freezable;
import platon.FreezerJSON;
import platon.FreezeException;
import platon.JsonNode;
import platon.Message;

public class HeartbeatReportAckDO extends Message implements FreezerJSON
{
    public static final int FIELD_ID_SEQNO = 0;
    public static final int FIELD_ID_SCHEDULERID = 1;
    public static final int FIELD_ID_OPTIONS = 2;

    private String seqNo = null;
    private String schedulerId = null;
    private platon.PropSet options = null;

    public HeartbeatReportAckDO() {
        addField(FIELD_ID_SEQNO);
        addField(FIELD_ID_SCHEDULERID);
        if (this.options != null) {
            addField(FIELD_ID_OPTIONS);
        }
    }

    public HeartbeatReportAckDO(HeartbeatReportAckDO other) {
        this.copyFrom(other);
    }

    public String getSeqNo() {
        return this.seqNo;
    }

    public void setSeqNo(String val) {
        this.seqNo = val;
    }

    public String getSchedulerId() {
        return this.schedulerId;
    }

    public void setSchedulerId(String val) {
        this.schedulerId = val;
    }

    public platon.PropSet getOptions() {
        if (this.options == null) {
            this.options = new platon.PropSet();
            this.addField(FIELD_ID_OPTIONS);
        }
        return this.options;
    }


    // �Ӹ������󿽱����ݵ���ǰ����
    @Override
    public void copyFrom(Freezable other) {
        super.copyFrom(other);
        HeartbeatReportAckDO from = (HeartbeatReportAckDO)other;
        this.seqNo = from.seqNo;
        this.schedulerId = from.schedulerId;
        if (this.options == null) {
            this.options = new platon.PropSet();
        }
        this.options.copyFrom(from.getOptions());
    }

    // ��¡��ǰ����
    @Override
    public Freezable makeClone() {
        HeartbeatReportAckDO o = new HeartbeatReportAckDO();
        o.copyFrom(this);
        return o;
    }

    public void clear() {
        this.seqNo = null;
        this.schedulerId = null;
        this.options = null;
        this.clearFields();
    }

    // ����ǰ���������������ֽ����С�
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        beginFreeze(output, cookie);

        if (hasField(FIELD_ID_SEQNO)) {
            writeFieldString(output, FIELD_ID_SEQNO, this.seqNo);
        }

        if (hasField(FIELD_ID_SCHEDULERID)) {
            writeFieldString(output, FIELD_ID_SCHEDULERID, this.schedulerId);
        }

        if (hasField(FIELD_ID_OPTIONS)) {
            writeFieldObject(output, FIELD_ID_OPTIONS, this.options);
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
            case FIELD_ID_SEQNO:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.seqNo = readFieldString(input);
                    addField(FIELD_ID_SEQNO);
                    consumed = true;
                }
                break;

            case FIELD_ID_SCHEDULERID:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.schedulerId = readFieldString(input);
                    addField(FIELD_ID_SCHEDULERID);
                    consumed = true;
                }
                break;

            case FIELD_ID_OPTIONS:
                if (tagType == TAG_TYPE_EMBEDDEDMSG) {
                    if (this.options == null) {
                        this.options = new platon.PropSet();
                    }
                    readFieldObject(input, this.options);
                    addField(FIELD_ID_OPTIONS);
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

        if (hasField(FIELD_ID_SEQNO) && this.seqNo != null) {
            JsonNode val_c_seqNo = new JsonNode(this.seqNo);
            thisNode.put("seqNo", val_c_seqNo);
        }

        if (hasField(FIELD_ID_SCHEDULERID) && this.schedulerId != null) {
            JsonNode val_c_schedulerId = new JsonNode(this.schedulerId);
            thisNode.put("schedulerId", val_c_schedulerId);
        }

        if (hasField(FIELD_ID_OPTIONS) && this.options != null) {
            JsonNode val_c_options = this.options.freezeToJSON();
            thisNode.put("options", val_c_options);
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

        JsonNode val_c_seqNo = inputJsonNode.get("seqNo");
        if (val_c_seqNo != null && !val_c_seqNo.isNull()) {
            this.seqNo = val_c_seqNo.asString();
            addField(FIELD_ID_SEQNO);
        }

        JsonNode val_c_schedulerId = inputJsonNode.get("schedulerId");
        if (val_c_schedulerId != null && !val_c_schedulerId.isNull()) {
            this.schedulerId = val_c_schedulerId.asString();
            addField(FIELD_ID_SCHEDULERID);
        }

        JsonNode val_c_options = inputJsonNode.get("options");
        if (val_c_options != null && !val_c_options.isNull()) {
            this.getOptions().defreezeFromJSON(val_c_options);
            addField(FIELD_ID_OPTIONS);
        }
    }
}
