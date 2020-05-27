package gtcloud.jobman.core.scheduler;

import java.util.ArrayList;

import gtcloud.jobman.core.pdo.JobControlBlockDO;
import platon.ByteSeq;

public class AbstractJobDispatcher implements JobDispatcher {

    // 当前分派器所针对的作业类别
    private final String jobCategory;

    // 当前分派器的工厂对象
    private final JobDispatcherFactory factory;
    
    protected AbstractJobDispatcher(String jobCategory, JobDispatcherFactory factory) {
        this.jobCategory = jobCategory;
        this.factory = factory;       
    }
    
	@Override
	public void onStart(JobDispatcherContext ctx) throws Exception {
		;
	}

	@Override
	public void onStop() {
		;
	}

	@Override
	public ArrayList<Subjob> onSplitJob(JobControlBlockDO jobCB,
			                            ByteSeq jobBody,
			                            ArrayList<SubjobProcessNode> nodeList) throws Exception {
		return splitJob(jobCB, jobBody, nodeList);
	}
	
    /**
     * 进行作业拆分.
     *
     * @param jobCB 待拆分的大作业控制块;
     * @param jobBody 待拆分的大作业数据部分, 内容依赖于具体的作业类别;
     * @param nodeList 当前可用的处理节点列表.
     *
     * @return 拆分后的子作业列表.
     *
     * @throws Exception
     */
    protected ArrayList<Subjob> splitJob(JobControlBlockDO jobCB,
                                         ByteSeq jobBody,
                                         ArrayList<SubjobProcessNode> nodeList) throws Exception {
        return new ArrayList<Subjob>();
    }

	public String getJobCategory() {
		return jobCategory;
	}

	public JobDispatcherFactory getFactory() {
		return factory;
	}	

}
