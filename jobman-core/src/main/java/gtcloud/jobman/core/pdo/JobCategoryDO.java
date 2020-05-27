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

public class JobCategoryDO extends Message implements FreezerJSON
{
    public static final int FIELD_ID_JOBCATEGORY = 0;

    private String jobCategory = null;

    public JobCategoryDO() {
        addField(FIELD_ID_JOBCATEGORY);
    }

    public JobCategoryDO(JobCategoryDO other) {
        this.copyFrom(other);
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
        JobCategoryDO from = (JobCategoryDO)other;
        this.jobCategory = from.jobCategory;
    }

    // 克隆当前对象。
    @Override
    public Freezable makeClone() {
        JobCategoryDO o = new JobCategoryDO();
        o.copyFrom(this);
        return o;
    }

    public void clear() {
        this.jobCategory = null;
        this.clearFields();
    }

    // 将当前对象打包进给定的字节流中。
    @Override
    public void freeze(ByteStream output) throws FreezeException {
        int[] cookie = new int[2];
        beginFreeze(output, cookie);

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

        JsonNode val_c_jobCategory = inputJsonNode.get("jobCategory");
        if (val_c_jobCategory != null && !val_c_jobCategory.isNull()) {
            this.jobCategory = val_c_jobCategory.asString();
            addField(FIELD_ID_JOBCATEGORY);
        }
    }
}

