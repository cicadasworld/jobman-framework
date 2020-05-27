package gtcloud.jobman.core.processor.main;

import java.util.Map.Entry;
import java.util.Set;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobStatusDO;
import gtcloud.jobman.core.processor.SubjobEntity;
import gtcloud.jobman.core.processor.SubjobHandle;

class DefaultSubjobEntity implements SubjobEntity {

    private DefaultSubjobHandle handle = new DefaultSubjobHandle(this);

    private PropertiesEx options = new PropertiesEx();

    private int priority = 0;
    private String jobCaption = "";
    private boolean isReduce = false;
    private byte[] body = new byte[0];

    // 当前的执行状态
    private final SubjobStatusDO statusDO = new SubjobStatusDO();
    
    @Override
    public SubjobHandle getHandle() {
        return this.handle;
    }

    @Override
    public String getJobCaption() {
        return this.jobCaption;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }
    
	@Override
	public boolean isReduce() {
		return this.isReduce;
	}

    @Override
    public PropertiesEx getOptions() {
        return this.options;
    }

    @Override
    public byte[] getSubjobBody() {
        return this.body;
    }

    @Override
    public void copyFrom(SubjobEntity from, boolean copyBody) {
        this.handle.copyFrom(from.getHandle());
        this.priority = from.getPriority();
        this.jobCaption = from.getJobCaption();
        this.isReduce = from.isReduce();
        this.options.clear();
        this.options.putAll(from.getOptions());
        if (copyBody) {
            this.body = from.getSubjobBody();
        }
        else if (this.body != null && this.body.length > 0) {
            this.body = new byte[0];
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("jobCategory: " + this.handle.getJobCategory() + "\n");
        sb.append("jobId: " + this.handle.getJobId() + "\n");
        sb.append("subjobSeqNo: " + this.handle.getSubjobSeqNo() + "\n");
        sb.append("jobCaption: " + this.jobCaption + "\n");
        sb.append("jobPriority: " + this.priority + "\n");
        sb.append("isReduce: " + this.isReduce + "\n");
        sb.append("options--------\n");
        for (String name : this.options.stringPropertyNames()) {
            final String value = this.options.getProperty(name);
            sb.append("  |" + name + "| = |" + value + "|\n");
        }

        sb.append("body: " + this.body.length + "(bytes)\n");

        // 打印出前32个字节
        int nprint = Math.min(32, this.body.length);
        for (int i = 0; i < nprint; ++ i) {
            byte ch = this.body[i];
            String hex = String.format("%02x", ch);
            if (i == 0) {
                sb.append("  ");
            }
            sb.append(hex);
        }

        return sb.toString();
    }

    public static DefaultSubjobEntity createInstance(SubjobControlBlockDO scb, byte[] body) {
        DefaultSubjobEntity e = new DefaultSubjobEntity();
        e.handle.setJobId(scb.getJobId());
        e.handle.setJobCategory(scb.getJobCategory());
        e.handle.setSubjobSeqNo(scb.getSubjobSeqNo());
        e.handle.setSiblingsCount(scb.getSubjobCount());
        e.isReduce = scb.getIsReduce();
        e.jobCaption = scb.getJobCaption();
        e.priority = scb.getJobPriority();

        Set<Entry<String, String>> set = scb.getOptions().entrySet();
        for (Entry<String, String> elem : set) {
            final String key = (String)elem.getKey();
            final String value = (String)elem.getValue();
            e.getOptions().put(key, value);
        }

        e.body = body;
        return e;
    }

	public SubjobStatusDO getStatusDO() {
		return statusDO;
	}
}
