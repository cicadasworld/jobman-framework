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
	 * 进行初始化处理.
	 * @param params 初始化属性参数
	 * @param options 可选参数
	 * @throws Exception 若初始化失败将抛出异常.
	 */
    @Override
    public void initialize(PropertiesEx params, HashMap<String, Object> options) throws Exception {
    	LOG.info(this.getClass() + ".initialize() called.");
	}
	
	/**
	 * 进行清理处理.
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
