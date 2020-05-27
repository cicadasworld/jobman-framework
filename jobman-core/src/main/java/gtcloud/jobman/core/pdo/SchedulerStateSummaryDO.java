// Java file generated automatically from `dumpstate.fzdl'.
// $Timestamp: 2019-07-21 10:55:38
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

public class SchedulerStateSummaryDO extends Message implements FreezerJSON
{
    public static final int FIELD_ID_TOTALFINISHEDJOBCOUNT = 0;
    public static final int FIELD_ID_FAILUREFINISHEDJOBCOUNT = 1;
    public static final int FIELD_ID_SUCCESSFINISHEDJOBCOUNT = 2;
    public static final int FIELD_ID_TOTALPENDINGJOBCOUNT = 3;
    public static final int FIELD_ID_WAITINGQUEUELENGTH = 6;
    public static final int FIELD_ID_SENDINGQUEUELENGTH = 7;
    public static final int FIELD_ID_DISPATCHEDQUEUELENGTH = 8;
    public static final int FIELD_ID_RETRYQUEUELENGTH = 9;
    public static final int FIELD_ID_PROCESSNODES = 10;
    public static final int FIELD_ID_OPTIONS = 11;

    private int totalFinishedJobCount = 0;
    private int failureFinishedJobCount = 0;
    private int successFinishedJobCount = 0;
    private int totalPendingJobCount = 0;
    private int waitingQueueLength = 0;
    private int sendingQueueLength = 0;
    private int dispatchedQueueLength = 0;
    private int retryQueueLength = 0;
    private SubjobProcessNodeListDO processNodes = null;
    private platon.PropSet options = null;

    public SchedulerStateSummaryDO() {
        addField(FIELD_ID_TOTALFINISHEDJOBCOUNT);
        addField(FIELD_ID_FAILUREFINISHEDJOBCOUNT);
        addField(FIELD_ID_SUCCESSFINISHEDJOBCOUNT);
        addField(FIELD_ID_TOTALPENDINGJOBCOUNT);
        addField(FIELD_ID_WAITINGQUEUELENGTH);
        addField(FIELD_ID_SENDINGQUEUELENGTH);
        addField(FIELD_ID_DISPATCHEDQUEUELENGTH);
        addField(FIELD_ID_RETRYQUEUELENGTH);
        if (this.processNodes != null) {
            addField(FIELD_ID_PROCESSNODES);
        }
        if (this.options != null) {
            addField(FIELD_ID_OPTIONS);
        }
    }

    public SchedulerStateSummaryDO(SchedulerStateSummaryDO other) {
        this.copyFrom(other);
    }

    public int getTotalFinishedJobCount() {
        return this.totalFinishedJobCount;
    }

    public void setTotalFinishedJobCount(int val) {
        this.totalFinishedJobCount = val;
    }

    public int getFailureFinishedJobCount() {
        return this.failureFinishedJobCount;
    }

    public void setFailureFinishedJobCount(int val) {
        this.failureFinishedJobCount = val;
    }

    public int getSuccessFinishedJobCount() {
        return this.successFinishedJobCount;
    }

    public void setSuccessFinishedJobCount(int val) {
        this.successFinishedJobCount = val;
    }

    public int getTotalPendingJobCount() {
        return this.totalPendingJobCount;
    }

    public void setTotalPendingJobCount(int val) {
        this.totalPendingJobCount = val;
    }

    public int getWaitingQueueLength() {
        return this.waitingQueueLength;
    }

    public void setWaitingQueueLength(int val) {
        this.waitingQueueLength = val;
    }

    public int getSendingQueueLength() {
        return this.sendingQueueLength;
    }

    public void setSendingQueueLength(int val) {
        this.sendingQueueLength = val;
    }

    public int getDispatchedQueueLength() {
        return this.dispatchedQueueLength;
    }

    public void setDispatchedQueueLength(int val) {
        this.dispatchedQueueLength = val;
    }

    public int getRetryQueueLength() {
        return this.retryQueueLength;
    }

    public void setRetryQueueLength(int val) {
        this.retryQueueLength = val;
    }

    public SubjobProcessNodeListDO getProcessNodes() {
        if (this.processNodes == null) {
            this.processNodes = new SubjobProcessNodeListDO();
            this.addField(FIELD_ID_PROCESSNODES);
        }
        return this.processNodes;
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
        SchedulerStateSummaryDO from = (SchedulerStateSummaryDO)other;
        this.totalFinishedJobCount = from.totalFinishedJobCount;
        this.failureFinishedJobCount = from.failureFinishedJobCount;
        this.successFinishedJobCount = from.successFinishedJobCount;
        this.totalPendingJobCount = from.totalPendingJobCount;
        this.waitingQueueLength = from.waitingQueueLength;
        this.sendingQueueLength = from.sendingQueueLength;
        this.dispatchedQueueLength = from.dispatchedQueueLength;
        this.retryQueueLength = from.retryQueueLength;
        if (this.processNodes == null) {
            this.processNodes = new SubjobProcessNodeListDO();
        }
        this.processNodes.copyFrom(from.getProcessNodes());
        if (this.options == null) {
            this.options = new platon.PropSet();
        }
        this.options.copyFrom(from.getOptions());
    }

    // ��¡��ǰ����
    @Override
    public Freezable makeClone() {
        SchedulerStateSummaryDO o = new SchedulerStateSummaryDO();
        o.copyFrom(this);
        return o;
    }

    public void clear() {
        this.totalFinishedJobCount = 0;
        this.failureFinishedJobCount = 0;
        this.successFinishedJobCount = 0;
        this.totalPendingJobCount = 0;
        this.waitingQueueLength = 0;
        this.sendingQueueLength = 0;
        this.dispatchedQueueLength = 0;
        this.retryQueueLength = 0;
        this.processNodes = null;
        this.options = null;
        this.clearFields();
    }

    // ����ǰ���������������ֽ����С�
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        beginFreeze(output, cookie);

        if (hasField(FIELD_ID_TOTALFINISHEDJOBCOUNT)) {
            writeFieldInt(output, FIELD_ID_TOTALFINISHEDJOBCOUNT, this.totalFinishedJobCount);
        }

        if (hasField(FIELD_ID_FAILUREFINISHEDJOBCOUNT)) {
            writeFieldInt(output, FIELD_ID_FAILUREFINISHEDJOBCOUNT, this.failureFinishedJobCount);
        }

        if (hasField(FIELD_ID_SUCCESSFINISHEDJOBCOUNT)) {
            writeFieldInt(output, FIELD_ID_SUCCESSFINISHEDJOBCOUNT, this.successFinishedJobCount);
        }

        if (hasField(FIELD_ID_TOTALPENDINGJOBCOUNT)) {
            writeFieldInt(output, FIELD_ID_TOTALPENDINGJOBCOUNT, this.totalPendingJobCount);
        }

        if (hasField(FIELD_ID_WAITINGQUEUELENGTH)) {
            writeFieldInt(output, FIELD_ID_WAITINGQUEUELENGTH, this.waitingQueueLength);
        }

        if (hasField(FIELD_ID_SENDINGQUEUELENGTH)) {
            writeFieldInt(output, FIELD_ID_SENDINGQUEUELENGTH, this.sendingQueueLength);
        }

        if (hasField(FIELD_ID_DISPATCHEDQUEUELENGTH)) {
            writeFieldInt(output, FIELD_ID_DISPATCHEDQUEUELENGTH, this.dispatchedQueueLength);
        }

        if (hasField(FIELD_ID_RETRYQUEUELENGTH)) {
            writeFieldInt(output, FIELD_ID_RETRYQUEUELENGTH, this.retryQueueLength);
        }

        if (hasField(FIELD_ID_PROCESSNODES)) {
            writeFieldObject(output, FIELD_ID_PROCESSNODES, this.processNodes);
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
            case FIELD_ID_TOTALFINISHEDJOBCOUNT:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.totalFinishedJobCount = readFieldInt(input);
                    addField(FIELD_ID_TOTALFINISHEDJOBCOUNT);
                    consumed = true;
                }
                break;

            case FIELD_ID_FAILUREFINISHEDJOBCOUNT:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.failureFinishedJobCount = readFieldInt(input);
                    addField(FIELD_ID_FAILUREFINISHEDJOBCOUNT);
                    consumed = true;
                }
                break;

            case FIELD_ID_SUCCESSFINISHEDJOBCOUNT:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.successFinishedJobCount = readFieldInt(input);
                    addField(FIELD_ID_SUCCESSFINISHEDJOBCOUNT);
                    consumed = true;
                }
                break;

            case FIELD_ID_TOTALPENDINGJOBCOUNT:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.totalPendingJobCount = readFieldInt(input);
                    addField(FIELD_ID_TOTALPENDINGJOBCOUNT);
                    consumed = true;
                }
                break;

            case FIELD_ID_WAITINGQUEUELENGTH:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.waitingQueueLength = readFieldInt(input);
                    addField(FIELD_ID_WAITINGQUEUELENGTH);
                    consumed = true;
                }
                break;

            case FIELD_ID_SENDINGQUEUELENGTH:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.sendingQueueLength = readFieldInt(input);
                    addField(FIELD_ID_SENDINGQUEUELENGTH);
                    consumed = true;
                }
                break;

            case FIELD_ID_DISPATCHEDQUEUELENGTH:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.dispatchedQueueLength = readFieldInt(input);
                    addField(FIELD_ID_DISPATCHEDQUEUELENGTH);
                    consumed = true;
                }
                break;

            case FIELD_ID_RETRYQUEUELENGTH:
                if (tagType == TAG_TYPE_VARINT32) {
                    this.retryQueueLength = readFieldInt(input);
                    addField(FIELD_ID_RETRYQUEUELENGTH);
                    consumed = true;
                }
                break;

            case FIELD_ID_PROCESSNODES:
                if (tagType == TAG_TYPE_EMBEDDEDMSG) {
                    if (this.processNodes == null) {
                        this.processNodes = new SubjobProcessNodeListDO();
                    }
                    readFieldObject(input, this.processNodes);
                    addField(FIELD_ID_PROCESSNODES);
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

        if (hasField(FIELD_ID_TOTALFINISHEDJOBCOUNT)) {
            JsonNode val_c_totalFinishedJobCount = new JsonNode(this.totalFinishedJobCount);
            thisNode.put("totalFinishedJobCount", val_c_totalFinishedJobCount);
        }

        if (hasField(FIELD_ID_FAILUREFINISHEDJOBCOUNT)) {
            JsonNode val_c_failureFinishedJobCount = new JsonNode(this.failureFinishedJobCount);
            thisNode.put("failureFinishedJobCount", val_c_failureFinishedJobCount);
        }

        if (hasField(FIELD_ID_SUCCESSFINISHEDJOBCOUNT)) {
            JsonNode val_c_successFinishedJobCount = new JsonNode(this.successFinishedJobCount);
            thisNode.put("successFinishedJobCount", val_c_successFinishedJobCount);
        }

        if (hasField(FIELD_ID_TOTALPENDINGJOBCOUNT)) {
            JsonNode val_c_totalPendingJobCount = new JsonNode(this.totalPendingJobCount);
            thisNode.put("totalPendingJobCount", val_c_totalPendingJobCount);
        }

        if (hasField(FIELD_ID_WAITINGQUEUELENGTH)) {
            JsonNode val_c_waitingQueueLength = new JsonNode(this.waitingQueueLength);
            thisNode.put("waitingQueueLength", val_c_waitingQueueLength);
        }

        if (hasField(FIELD_ID_SENDINGQUEUELENGTH)) {
            JsonNode val_c_sendingQueueLength = new JsonNode(this.sendingQueueLength);
            thisNode.put("sendingQueueLength", val_c_sendingQueueLength);
        }

        if (hasField(FIELD_ID_DISPATCHEDQUEUELENGTH)) {
            JsonNode val_c_dispatchedQueueLength = new JsonNode(this.dispatchedQueueLength);
            thisNode.put("dispatchedQueueLength", val_c_dispatchedQueueLength);
        }

        if (hasField(FIELD_ID_RETRYQUEUELENGTH)) {
            JsonNode val_c_retryQueueLength = new JsonNode(this.retryQueueLength);
            thisNode.put("retryQueueLength", val_c_retryQueueLength);
        }

        if (hasField(FIELD_ID_PROCESSNODES) && this.processNodes != null) {
            JsonNode val_c_processNodes = this.processNodes.freezeToJSON();
            thisNode.put("processNodes", val_c_processNodes);
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

        JsonNode val_c_totalFinishedJobCount = inputJsonNode.get("totalFinishedJobCount");
        if (val_c_totalFinishedJobCount != null && !val_c_totalFinishedJobCount.isNull()) {
            this.totalFinishedJobCount = (int)val_c_totalFinishedJobCount.asInt64();
            addField(FIELD_ID_TOTALFINISHEDJOBCOUNT);
        }

        JsonNode val_c_failureFinishedJobCount = inputJsonNode.get("failureFinishedJobCount");
        if (val_c_failureFinishedJobCount != null && !val_c_failureFinishedJobCount.isNull()) {
            this.failureFinishedJobCount = (int)val_c_failureFinishedJobCount.asInt64();
            addField(FIELD_ID_FAILUREFINISHEDJOBCOUNT);
        }

        JsonNode val_c_successFinishedJobCount = inputJsonNode.get("successFinishedJobCount");
        if (val_c_successFinishedJobCount != null && !val_c_successFinishedJobCount.isNull()) {
            this.successFinishedJobCount = (int)val_c_successFinishedJobCount.asInt64();
            addField(FIELD_ID_SUCCESSFINISHEDJOBCOUNT);
        }

        JsonNode val_c_totalPendingJobCount = inputJsonNode.get("totalPendingJobCount");
        if (val_c_totalPendingJobCount != null && !val_c_totalPendingJobCount.isNull()) {
            this.totalPendingJobCount = (int)val_c_totalPendingJobCount.asInt64();
            addField(FIELD_ID_TOTALPENDINGJOBCOUNT);
        }

        JsonNode val_c_waitingQueueLength = inputJsonNode.get("waitingQueueLength");
        if (val_c_waitingQueueLength != null && !val_c_waitingQueueLength.isNull()) {
            this.waitingQueueLength = (int)val_c_waitingQueueLength.asInt64();
            addField(FIELD_ID_WAITINGQUEUELENGTH);
        }

        JsonNode val_c_sendingQueueLength = inputJsonNode.get("sendingQueueLength");
        if (val_c_sendingQueueLength != null && !val_c_sendingQueueLength.isNull()) {
            this.sendingQueueLength = (int)val_c_sendingQueueLength.asInt64();
            addField(FIELD_ID_SENDINGQUEUELENGTH);
        }

        JsonNode val_c_dispatchedQueueLength = inputJsonNode.get("dispatchedQueueLength");
        if (val_c_dispatchedQueueLength != null && !val_c_dispatchedQueueLength.isNull()) {
            this.dispatchedQueueLength = (int)val_c_dispatchedQueueLength.asInt64();
            addField(FIELD_ID_DISPATCHEDQUEUELENGTH);
        }

        JsonNode val_c_retryQueueLength = inputJsonNode.get("retryQueueLength");
        if (val_c_retryQueueLength != null && !val_c_retryQueueLength.isNull()) {
            this.retryQueueLength = (int)val_c_retryQueueLength.asInt64();
            addField(FIELD_ID_RETRYQUEUELENGTH);
        }

        JsonNode val_c_processNodes = inputJsonNode.get("processNodes");
        if (val_c_processNodes != null && !val_c_processNodes.isNull()) {
            this.getProcessNodes().defreezeFromJSON(val_c_processNodes);
            addField(FIELD_ID_PROCESSNODES);
        }

        JsonNode val_c_options = inputJsonNode.get("options");
        if (val_c_options != null && !val_c_options.isNull()) {
            this.getOptions().defreezeFromJSON(val_c_options);
            addField(FIELD_ID_OPTIONS);
        }
    }
}
