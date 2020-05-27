package gtcloud.jobman.core.scheduler;

import java.util.Comparator;

import gtcloud.jobman.core.pdo.SubjobControlBlockDO;

public class SubjobComparator implements Comparator<SubjobEntry> {

	public SubjobComparator() {
		;
	}
	
	@Override
	public int compare(SubjobEntry x, SubjobEntry y) {
		// ���ȱȽ����ȼ�
		final SubjobControlBlockDO scbX = x.getSubjobCB();
		final SubjobControlBlockDO scbY = y.getSubjobCB();		
		int priorityX = scbX.getJobPriority();
		int priorityY = scbY.getJobPriority();
		int priorityDiff = priorityX - priorityY;
		if (priorityDiff != 0) {
			// ֵԽС�����ȼ�Խ��
			return priorityDiff > 0 ? 1 : (-1); 
		}
		
		// ��αȽϴ���ʱ��
		long borntimeX = x.getBornTime();
		long borntimeY = y.getBornTime();
		long borntimeDiff = borntimeX - borntimeY;
		if (borntimeDiff != 0) {
			// ʱ��Խ�磬���ȼ�Խ��			
			return borntimeDiff > 0 ? 1 : (-1); 
		}
		
		// �ٱȽ�jobId
		String jobIdX = scbX.getJobId();
		String jobIdY = scbY.getJobId();
		int jobIdDiff = jobIdX.compareTo(jobIdY);
		if (jobIdDiff != 0) {
			return jobIdDiff > 0 ? 1 : (-1); 
		}
		
		// �ٱȽ�����ҵ���
		int seqnoX = scbX.getSubjobSeqNo();
		int seqnoY = scbY.getSubjobSeqNo();
		int seqnoDiff = seqnoX - seqnoY;
		if (seqnoDiff != 0) {
			// ���ԽС�����ȼ�Խ��			
			return seqnoDiff > 0 ? 1 : (-1); 
		}
		
		return 0;
	}

}
