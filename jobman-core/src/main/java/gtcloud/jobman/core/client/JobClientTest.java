package gtcloud.jobman.core.client;

import java.util.UUID;

import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import platon.ByteStream;
import platon.PropSet;

public class JobClientTest {

	private static final String schedulerBaseURL = "http://127.0.0.1:44850/gtjobsched";
	
	public static void main(String[] args) throws Exception {
		
		//创建几个不同优先级的作业
		int priority = 1;
		int i = 0;
		for(; i < 1; i++, priority++) {
			String jobId = UUID.randomUUID().toString();
			
			JobControlBlockDO jobCB = new JobControlBlockDO();
			jobCB.setJobId(jobId);
			jobCB.setJobCategory("DUMMY_JOB_CATEGORY");
			jobCB.setJobCaption("Test-" + MiscUtils.formatDateTime(System.currentTimeMillis()));
			jobCB.getOptions().put(ConstKeys.JOB_PROP_SubmitterUserId, "liyanming");
			jobCB.getOptions().put(ConstKeys.JOB_PROP_SubmitterHost, "129.0.3.60");
			
			//设置Priority的偏移
			priority = priority == 3 ? 0 : priority;
			jobCB.setJobPriority(priority);

		    // 任务用一个属性集来表达
		    ByteStream stream = new ByteStream();
	        PropSet p = new PropSet();
	        p.put("workload", "6000");
	        p.freeze(stream);

	    	JobClient jc = new JobClient(schedulerBaseURL);
	    	jc.submitJob(jobCB, stream);
	    	System.out.println("submitJob() succeeded, jobId=" + jobId + "jobPriority=" + priority);
		}
	}
}
