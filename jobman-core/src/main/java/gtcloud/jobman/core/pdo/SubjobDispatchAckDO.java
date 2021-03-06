// Java file generated automatically from `schedpdo.fzdl'.
// $Timestamp: 2018-04-18 15:58:29
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

public class SubjobDispatchAckDO extends Message implements FreezerJSON
{
    public static final int FIELD_ID_NODEID = 0;
    public static final int FIELD_ID_SCHEDULERID = 1;
    public static final int FIELD_ID_JOBID = 2;
    public static final int FIELD_ID_SUBJOBSEQNO = 3;
    public static final int FIELD_ID_JOBCATEGORY = 4;

    private String nodeId = null;
    private String schedulerId = null;
    private String jobId = null;
    private int subjobSeqNo = 0;
    private String jobCategory = null;

    public SubjobDispatchAckDO() {
        addField(FIELD_ID_NODEID);
        addField(FIELD_ID_SCHEDULERID);
        addField(FIELD_ID_JOBID);
        addField(FIELD_ID_SUBJOBSEQNO);
        addField(FIELD_ID_JOBCATEGORY);
    }

    public SubjobDispatchAckDO(SubjobDispatchAckDO other) {
        this.copyFrom(other);
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(String val) {
        this.nodeId = val;
    }

    public String getSchedulerId() {
        return this.schedulerId;
    }

    public void setSchedulerId(String val) {
        this.schedulerId = val;
    }

    public String getJobId() {
        return this.jobId;
    }

    public void setJobId(String val) {
        this.jobId = val;
    }

    public int getSubjobSeqNo() {
        return this.subjobSeqNo;
    }

    public void setSubjobSeqNo(int val) {
        this.subjobSeqNo = val;
    }

    public String getJobCategory() {
        return this.jobCategory;
    }

    public void setJobCategory(String val) {
        this.jobCategory = val;
    }


    // 从给定对象拷贝数据到当前对象
    @Override
    public void copyFrom(Freezable other) {
        super.copyFrom(other);
        SubjobDispatchAckDO from = (SubjobDispatchAckDO)other;
        this.nodeId = from.nodeId;
        this.schedulerId = from.schedulerId;
        this.jobId = from.jobId;
        this.subjobSeqNo = from.subjobSeqNo;
        this.jobCategory = from.jobCategory;
    }

    // 克隆当前对象。
    @Override
    public Freezable makeClone() {
        SubjobDispatchAckDO o = new SubjobDispatchAckDO();
        o.copyFrom(this);
        return o;
    }

    public void clear() {
        this.nodeId = null;
        this.schedulerId = null;
        this.jobId = null;
        this.subjobSeqNo = 0;
        this.jobCategory = null;
        this.clearFields();
    }

    // 将当前对象打包进给定的字节流中。
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        beginFreeze(output, cookie);

        if (hasField(FIELD_ID_NODEID)) {
            writeFieldString(output, FIELD_ID_NODEID, this.nodeId);
        }

        if (hasField(FIELD_ID_SCHEDULERID)) {
            writeFieldString(output, FIELD_ID_SCHEDULERID, this.schedulerId);
        }

        if (hasField(FIELD_ID_JOBID)) {
            writeFieldString(output, FIELD_ID_JOBID, this.jobId);
        }

        if (hasField(FIELD_ID_SUBJOBSEQNO)) {
            writeFieldInt(output, FIELD_ID_SUBJOBSEQNO, this.subjobSeqNo);
        }

        if (hasField(FIELD_ID_JOBCATEGORY)) {
            writeFieldString(output, FIELD_ID_JOBCATEGORY, this.jobCategory);
        }

        endFreeze(output, cookie);
    }

    // 从给定字节流中解包, 并将解包得到的数据覆盖到当前对象上。
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
            case FIELD_ID_NODEID:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.nodeId = readFieldString(input);
                    addField(FIELD_ID_NODEID);
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

            case FIELD_ID_JOBID:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.jobId = readFieldString(input);
                    addField(FIELD_ID_JOBID);
                    consumed = true;
                }
                break;

            case FIELD_ID_SUBJOBSEQNO:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.subjobSeqNo = readFieldInt(input);
                    addField(FIELD_ID_SUBJOBSEQNO);
                    consumed = true;
                }
                break;

            case FIELD_ID_JOBCATEGORY:
                if (tagType == TAG_TYPE_LEN_PREFIXED) {
                    this.jobCategory = readFieldString(input);
                    addField(FIELD_ID_JOBCATEGORY);
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

    // 将当前消息打包成JSON树, 返回JSON树的指针
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonObject();

        if (hasField(FIELD_ID_NODEID) && this.nodeId != null) {
            JsonNode val_c_nodeId = new JsonNode(this.nodeId);
            thisNode.put("nodeId", val_c_nodeId);
        }

        if (hasField(FIELD_ID_SCHEDULERID) && this.schedulerId != null) {
            JsonNode val_c_schedulerId = new JsonNode(this.schedulerId);
            thisNode.put("schedulerId", val_c_schedulerId);
        }

        if (hasField(FIELD_ID_JOBID) && this.jobId != null) {
            JsonNode val_c_jobId = new JsonNode(this.jobId);
            thisNode.put("jobId", val_c_jobId);
        }

        if (hasField(FIELD_ID_SUBJOBSEQNO)) {
            JsonNode val_c_subjobSeqNo = new JsonNode(this.subjobSeqNo);
            thisNode.put("subjobSeqNo", val_c_subjobSeqNo);
        }

        if (hasField(FIELD_ID_JOBCATEGORY) && this.jobCategory != null) {
            JsonNode val_c_jobCategory = new JsonNode(this.jobCategory);
            thisNode.put("jobCategory", val_c_jobCategory);
        }
        return thisNode;
    }

    // 从给定JSON树中解包, 并将解包得到的数据覆盖到当前消息上。
    @Override
    public void defreezeFromJSON(JsonNode inputJsonNode) throws DefreezeException {
        if (!inputJsonNode.isObject()) {
            throw new DefreezeException("input json is not object");
        }
        clear();

        JsonNode val_c_nodeId = inputJsonNode.get("nodeId");
        if (val_c_nodeId != null && !val_c_nodeId.isNull()) {
            this.nodeId = val_c_nodeId.asString();
            addField(FIELD_ID_NODEID);
        }

        JsonNode val_c_schedulerId = inputJsonNode.get("schedulerId");
        if (val_c_schedulerId != null && !val_c_schedulerId.isNull()) {
            this.schedulerId = val_c_schedulerId.asString();
            addField(FIELD_ID_SCHEDULERID);
        }

        JsonNode val_c_jobId = inputJsonNode.get("jobId");
        if (val_c_jobId != null && !val_c_jobId.isNull()) {
            this.jobId = val_c_jobId.asString();
            addField(FIELD_ID_JOBID);
        }

        JsonNode val_c_subjobSeqNo = inputJsonNode.get("subjobSeqNo");
        if (val_c_subjobSeqNo != null && !val_c_subjobSeqNo.isNull()) {
            this.subjobSeqNo = (int)val_c_subjobSeqNo.asInt64();
            addField(FIELD_ID_SUBJOBSEQNO);
        }

        JsonNode val_c_jobCategory = inputJsonNode.get("jobCategory");
        if (val_c_jobCategory != null && !val_c_jobCategory.isNull()) {
            this.jobCategory = val_c_jobCategory.asString();
            addField(FIELD_ID_JOBCATEGORY);
        }
    }
}

