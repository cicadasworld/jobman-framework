package gtcloud.jobman.core.scheduler;

import java.util.Comparator;

import gtcloud.jobman.core.pdo.SubjobControlBlockDO;

public class SubjobComparator implements Comparator<SubjobEntry> {

	public SubjobComparator() {
		;
	}
	
	@Override
	public int compare(SubjobEntry x, SubjobEntry y) {
		// 首先比较优先级
		final SubjobControlBlockDO scbX = x.getSubjobCB();
		final SubjobControlBlockDO scbY = y.getSubjobCB();		
		int priorityX = scbX.getJobPriority();
		int priorityY = scbY.getJobPriority();
		int priorityDiff = priorityX - priorityY;
		if (priorityDiff != 0) {
			// 值越小，优先级越高
			return priorityDiff > 0 ? 1 : (-1); 
		}
		
		// 其次比较创建时间
		long borntimeX = x.getBornTime();
		long borntimeY = y.getBornTime();
		long borntimeDiff = borntimeX - borntimeY;
		if (borntimeDiff != 0) {
			// 时间越早，优先级越高			
			return borntimeDiff > 0 ? 1 : (-1); 
		}
		
		// 再比较jobId
		String jobIdX = scbX.getJobId();
		String jobIdY = scbY.getJobId();
		int jobIdDiff = jobIdX.compareTo(jobIdY);
		if (jobIdDiff != 0) {
			return jobIdDiff > 0 ? 1 : (-1); 
		}
		
		// 再比较子作业序号
		int seqnoX = scbX.getSubjobSeqNo();
		int seqnoY = scbY.getSubjobSeqNo();
		int seqnoDiff = seqnoX - seqnoY;
		if (seqnoDiff != 0) {
			// 序号越小，优先级越高			
			return seqnoDiff > 0 ? 1 : (-1); 
		}
		
		return 0;
	}

}
