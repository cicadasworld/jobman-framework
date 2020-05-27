package gtcloud.jobman.core.test;

import java.util.ArrayList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.scheduler.AbstractJobDispatcher;
import gtcloud.jobman.core.scheduler.JobDispatcherFactory;
import gtcloud.jobman.core.scheduler.Subjob;
import gtcloud.jobman.core.scheduler.SubjobProcessNode;
import platon.ByteSeq;
import platon.ByteStream;
import platon.PropSet;

public class DummyJobDispatcher extends AbstractJobDispatcher {

    public DummyJobDispatcher(String jobCategory, JobDispatcherFactory factory) {
		super(jobCategory, factory);
	}

	private static Logger LOG = LoggerFactory.getLogger(DummyJobDispatcher.class);

	@Override
	protected ArrayList<Subjob> splitJob(JobControlBlockDO jobCB,
			                             ByteSeq jobBody,
			                             ArrayList<SubjobProcessNode> nodeList) throws Exception {

        LOG.info("splitJob() called, jobId=" + jobCB.getJobId() + ", jobCate=" + jobCB.getJobCategory());

        // ���"��ҵ��"
        PropSet propset = new PropSet();
        {
            byte[] b = jobBody.getBuffer().array();
            ByteStream stream = new ByteStream(b);
            propset.defreeze(stream);
        }

        // ���ڵ�ƽ�����ɹ�������
        final int nodeCount = 10;        
        final String v = propset.get("workload");
        final int workloadTotal = Integer.parseInt(v);
        final int workloadPerNode = workloadTotal / nodeCount;

        ArrayList<Subjob> result = new ArrayList<Subjob>();
        int workloadRemain = workloadTotal;
        for (int i = 0; i < nodeCount; ++ i) {
            Subjob subjob = new Subjob();
            result.add(subjob);

            // ���ø�����ҵ�Ŀ��ƿ�
            SubjobControlBlockDO scb = subjob.getSubjobCB();
            scb.setJobCaption(jobCB.getJobCaption());
            scb.setJobCategory(jobCB.getJobCategory());
            scb.setJobId(jobCB.getJobId());
            scb.setJobPriority(jobCB.getJobPriority());
            scb.setSubjobCount(nodeCount);
            scb.setSubjobSeqNo(i);
            scb.getOptions().put(ConstKeys.SUBJOB_PROP_GeoEntityId, "GeoEntityId-" + UUID.randomUUID().toString().replace("-", ""));
            
            if (i == 0) {
            	//����Ĭ������һ����β��ҵ
            	scb.setIsReduce(true);
            }

            // ���ø�����ҵ�ľ�����ҵ����
            ByteStream body = new ByteStream();
            {
                int w = (i == nodeCount - 1) ? workloadRemain : workloadPerNode;
                workloadRemain -= w;
                PropSet p = new PropSet();
                p.put("workload", String.format("%d", w));
                p.freeze(body);
                subjob.setTotalWorkload(w);
            }
            subjob.setBodyBytes(body.array(), 0, body.length());
            Thread.sleep(10);
        }

        return result;
    }
}
