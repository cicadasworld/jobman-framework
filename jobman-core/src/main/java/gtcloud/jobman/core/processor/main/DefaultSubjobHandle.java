package gtcloud.jobman.core.processor.main;

import gtcloud.jobman.core.common.Helper;
import gtcloud.jobman.core.processor.SubjobEntity;
import gtcloud.jobman.core.processor.SubjobHandle;

class DefaultSubjobHandle implements SubjobHandle {

    private String jobCategory;
    private String jobId;
    private int subjobSeqNo;
    private int siblingsCount;

    private final SubjobEntity subjobEntity;
    
    DefaultSubjobHandle(SubjobEntity subjobEntity) {
    	this.subjobEntity = subjobEntity;
    }
    
    @Override
    public String getJobId() {
        return this.jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getJobCategory() {
        return this.jobCategory;
    }

    public void setJobCategory(String jobCategory) {
        this.jobCategory = jobCategory;
    }

    @Override
    public int getSubjobSeqNo() {
        return this.subjobSeqNo;
    }

    public void setSubjobSeqNo(int subjobSeqNo) {
        this.subjobSeqNo = subjobSeqNo;
    }

    @Override
    public String getSubjobKey() {
        return Helper.makeSubjobKey(this.jobId, this.subjobSeqNo);
    }

    @Override
    public void copyFrom(SubjobHandle from) {
        this.jobCategory = from.getJobCategory();
        this.jobId = from.getJobId();
        this.subjobSeqNo = from.getSubjobSeqNo();
        this.siblingsCount = from.getSiblingsCount();
    }

    @Override
    public String toString() {
        return "DefaultSubjobHandle [jobCategory=" + jobCategory
                + ", jobId=" + jobId
                + ", subjobSeqNo=" + subjobSeqNo
                + "]";
    }

    @Override
    public int getSiblingsCount() {
        return siblingsCount;
    }

    public void setSiblingsCount(int siblingsCount) {
        this.siblingsCount = siblingsCount;
    }

	SubjobEntity getSubjobEntity() {
		return subjobEntity;
	}
}
