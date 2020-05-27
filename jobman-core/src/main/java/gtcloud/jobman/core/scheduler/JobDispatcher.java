package gtcloud.jobman.core.scheduler;

import java.util.ArrayList;

import gtcloud.jobman.core.pdo.JobControlBlockDO;
import platon.ByteSeq;

public interface JobDispatcher {

    void onStart(JobDispatcherContext ctx) throws Exception;

    void onStop();

	ArrayList<Subjob> onSplitJob(JobControlBlockDO jobCB,
			                     ByteSeq jobBody,
			                     ArrayList<SubjobProcessNode> nodeList) throws Exception;
}
