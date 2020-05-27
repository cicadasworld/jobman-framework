package gtcloud.jobman.core.scheduler;

import java.util.ArrayList;

import gtcloud.jobman.core.pdo.JobControlBlockDO;
import platon.ByteSeq;

public class AbstractJobDispatcher implements JobDispatcher {

    // ��ǰ����������Ե���ҵ���
    private final String jobCategory;

    // ��ǰ�������Ĺ�������
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
     * ������ҵ���.
     *
     * @param jobCB ����ֵĴ���ҵ���ƿ�;
     * @param jobBody ����ֵĴ���ҵ���ݲ���, ���������ھ������ҵ���;
     * @param nodeList ��ǰ���õĴ���ڵ��б�.
     *
     * @return ��ֺ������ҵ�б�.
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
