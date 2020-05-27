// Java file generated automatically from `jobpdo.fzdl'.
// $Timestamp: 2018-04-18 15:58:28
// DO NOT EDIT THIS FILE UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING.
//
package gtcloud.jobman.core.pdo;

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
public class JobStatusListDO extends ArrayList<JobStatusDO> implements Freezable, FreezerJSON
{
    private static final JobStatusDO NULL_ELEM = new JobStatusDO();

    public JobStatusListDO() {
        // EMPTY
    }

    public JobStatusListDO(Collection<JobStatusDO> collection) {
        super(collection);
    }

    public JobStatusListDO(int initialCapacity) {
        super(initialCapacity);
    }

    static private JobStatusDO deepCopyElem(JobStatusDO srcElem) {
        return (JobStatusDO)srcElem.makeClone();
    }

    // �Ӹ������󿽱����ݵ���ǰ����
    @Override
    public void copyFrom(Freezable other) {
        JobStatusListDO from = (JobStatusListDO)other;
        this.clear();
        for (JobStatusDO src : from) {
            JobStatusDO e = deepCopyElem(src);
            this.add(e);
        }
    }

    // ��¡��ǰ����
    @Override
    public Freezable makeClone() {
        JobStatusListDO obj = new JobStatusListDO();
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
        for (JobStatusDO elem : this) {
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
                JobStatusDO elem = defreezeElem(input);
                this.add(elem);
            }
        }

        Message.endDefreeze(input);
    }

    static private void freezeElem(JobStatusDO elem, ByteStream output) throws FreezeException {
        elem.freeze(output);
    }

    static private JobStatusDO defreezeElem(ByteStream input) throws DefreezeException {
        JobStatusDO elem = new JobStatusDO();
        elem.defreeze(input);
        return elem;
    }

    // ����ǰ��Ϣ�����JSON��, ����JSON����ָ��
    @Override
    public JsonNode freezeToJSON() throws FreezeException {
        JsonNode thisNode = JsonNode.createJsonArray();
        for (JobStatusDO elem : this) {
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
                JobStatusDO elem = new JobStatusDO();
                elem.defreezeFromJSON(j);
                this.add(elem);
            }
        }
    }
}
