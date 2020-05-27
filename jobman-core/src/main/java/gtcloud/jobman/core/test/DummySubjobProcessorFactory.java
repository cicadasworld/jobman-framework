package gtcloud.jobman.core.test;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.jobman.core.processor.SubjobProcessor;
import gtcloud.jobman.core.processor.SubjobProcessorFactory;

public class DummySubjobProcessorFactory implements SubjobProcessorFactory {

    private static Logger LOG = LoggerFactory.getLogger(DummySubjobProcessorFactory.class);

    @Override
    public void initialize(PropertiesEx params, HashMap<String, Object> options) throws Exception {
        LOG.info(this.getClass() + ".initialize() called.");
    }

    @Override
    public void dispose() {
        LOG.info(this.getClass() + ".dispose() called.");
    }

    @Override
    public void getSupportedJobCategory(ArrayList<String> result) {
        result.add(DummyConstants.DUMMY_JOB_CATEGORY);
    }

    @Override
    public SubjobProcessor createSubjobProcessor(String jobCategory) {
        if (DummyConstants.DUMMY_JOB_CATEGORY.equals(jobCategory)) {
            return new DummySubjobProcessor();
        }
        return null;
    }
}
