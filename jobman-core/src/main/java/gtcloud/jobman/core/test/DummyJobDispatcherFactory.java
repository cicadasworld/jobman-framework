package gtcloud.jobman.core.test;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.jobman.core.scheduler.JobDispatcher;
import gtcloud.jobman.core.scheduler.JobDispatcherFactory;

public class DummyJobDispatcherFactory implements JobDispatcherFactory {

    private static Logger LOG = LoggerFactory.getLogger(DummyJobDispatcherFactory.class);

	/**
	 * ���г�ʼ������.
	 * @param params ��ʼ�����Բ���
	 * @param options ��ѡ����
	 * @throws Exception ����ʼ��ʧ�ܽ��׳��쳣.
	 */
    @Override
    public void initialize(PropertiesEx params, HashMap<String, Object> options) throws Exception {
    	LOG.info(this.getClass() + ".initialize() called.");
	}
	
	/**
	 * ����������.
	 */
    @Override
    public void dispose() {
    	LOG.info(this.getClass() + ".dispose() called.");
	}
	
    @Override
    public void getSupportedJobCategory(ArrayList<String> result) {
        result.add(DummyConstants.DUMMY_JOB_CATEGORY);
    }

    @Override
    public JobDispatcher createJobDispatcher(String jobCategory) {
        if (DummyConstants.DUMMY_JOB_CATEGORY.equals(jobCategory)) {
            return new DummyJobDispatcher(jobCategory, this);
        }
        return null;
    }

    @Override
    public boolean isSubjobRetriable(String jobCategory) {
        if (DummyConstants.DUMMY_JOB_CATEGORY.equals(jobCategory)) {
            return true;
        }
        return false;
    }
}
