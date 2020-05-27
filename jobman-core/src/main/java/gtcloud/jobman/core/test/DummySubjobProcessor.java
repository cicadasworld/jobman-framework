package gtcloud.jobman.core.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.jobman.core.processor.SubjobContext;
import gtcloud.jobman.core.processor.SubjobEntity;
import gtcloud.jobman.core.processor.SubjobException;
import gtcloud.jobman.core.processor.SubjobProcessor;
import gtcloud.jobman.core.processor.SubjobRetriableException;
import platon.ByteStream;
import platon.PropSet;

public class DummySubjobProcessor implements SubjobProcessor {

    private static Logger LOG = LoggerFactory.getLogger(DummySubjobProcessor.class);

    @Override
    public void processSubjob(SubjobContext ctx, SubjobEntity subjob) throws SubjobException {
        if (LOG.isInfoEnabled()) {
            LOG.info("processSubjob() begin, subjobKey=" + subjob.getHandle().getSubjobKey()
                    + ", jobCate=" + subjob.getHandle().getJobCategory() + ", subJobPriority=" + subjob.getPriority());
        }

        try {
            processSubjob_i(ctx, subjob);
        } catch (SubjobException e) {
            throw e;
        } catch (Exception e) {
            throw new SubjobException(e);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("processSubjob() done, subjobKey=" + subjob.getHandle().getSubjobKey()
                    + ", jobCate=" + subjob.getHandle().getJobCategory() + ", subJobPriority=" + subjob.getPriority());
        }
    }

    @Override
    public void processReduce(SubjobContext ctx, SubjobEntity subjob) throws SubjobException {
        if (LOG.isInfoEnabled()) {
            LOG.info("processReduce() begin, subjobKey=" + subjob.getHandle().getSubjobKey()
                    + ", jobCate=" + subjob.getHandle().getJobCategory() + ", subJobPriority=" + subjob.getPriority());
        }

        try {
            processSubjob_i(ctx, subjob);
        } catch (Exception e) {
            throw new SubjobException(e);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("processReduce() done, subjobKey=" + subjob.getHandle().getSubjobKey()
                    + ", jobCate=" + subjob.getHandle().getJobCategory() + ", subJobPriority=" + subjob.getPriority());
        }
    }

    static private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processSubjob_i(SubjobContext ctx, SubjobEntity subjob) throws Exception {

        // ���"����ҵ��"
        PropSet propset = new PropSet();
        {
            byte[] body = subjob.getSubjobBody();
            ByteStream stream = new ByteStream(body);
            propset.defreeze(stream);
        }

        final String v = propset.get("workload");
        final int workloadTotal = Integer.parseInt(v);

        // ģ��ִ��3�����裬ÿ������3��
        ctx.reportStepBegin(subjob.getHandle(), "stepId-1");
        sleep(3*1000);
        ctx.reportProgress(subjob.getHandle(), workloadTotal*1/3);        
        ctx.reportStepEnd(subjob.getHandle(), "stepId-1");

        ctx.reportStepBegin(subjob.getHandle(), "stepId-2");
        sleep(3*1000);
        ctx.reportProgress(subjob.getHandle(), workloadTotal*2/3);        
        ctx.reportStepEnd(subjob.getHandle(), "stepId-2");        
        
        // ģ�⴦���г�����
        if (subjob.getHandle().getSubjobSeqNo() == 3) {
	        if (System.getProperty("forgeError") != null) {
	        	throw new SubjobException("����ʧ��:���������쳣");
	        }
        }
        else if (subjob.getHandle().getSubjobSeqNo() == 4) {
	        if (System.getProperty("forgeError") != null) {
	        	throw new SubjobRetriableException("����ʧ��:�������쳣");
	        }
        }
        
        ctx.reportStepBegin(subjob.getHandle(), "stepId-3");
        sleep(3*1000);
        ctx.reportProgress(subjob.getHandle(), workloadTotal*3/3);        
        ctx.reportStepEnd(subjob.getHandle(), "stepId-3");        
    }


}
