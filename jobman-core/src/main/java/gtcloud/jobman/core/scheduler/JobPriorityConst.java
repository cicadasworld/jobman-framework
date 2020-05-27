package gtcloud.jobman.core.scheduler;

public class JobPriorityConst {
    
	//最闲置时触发的作业
	public static final int JOB_PRIORITY_IDLE = 100;
	
	//通常的作业
	public static final int JOB_PRIORITY_NORMAL = 5;
	
	//要抓紧完成的作业
	public static final int JOB_PRIORITY_HIGH = 1;
	
	//必须要已最快速度完成的作业
	public static final int JOB_PRIORITY_EMERGENCY = 0;
}
