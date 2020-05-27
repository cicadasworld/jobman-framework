package gtcloud.jobman.core.processor.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.Lifecycle;
import gtcloud.common.plugin.PluginContainer;
import gtcloud.common.utils.PathUtils;
import gtcloud.jobman.core.processor.SubjobProcessor;
import gtcloud.jobman.core.processor.SubjobProcessorFactory;

class SubjobProcessorCreator {

    private static Logger LOG = LoggerFactory.getLogger(SubjobProcessorCreator.class);
    
	private PluginContainer pluginContainer = null;

    // jobCategory --> instanceOf factory
    private HashMap<String, SubjobProcessorFactory> factoryLookup = new HashMap<>();
    
    // jobCategory --> instanceOf plugin
    private ConcurrentHashMap<String, SubjobProcessor> pluginLookup = new ConcurrentHashMap<>();
    
    public void init() throws Exception {
		String etcDir = PathUtils.getEtcDir();
		String f = etcDir + "/jobman/processor/subjobproc.xml";		
		this.pluginContainer = PluginContainer.createInstance(f, SubjobProcessorFactory.class);
		
		ArrayList<Lifecycle> plugins = this.pluginContainer.getPlugins();
		for (Lifecycle p : plugins) {
			SubjobProcessorFactory factory = (SubjobProcessorFactory)p;
			ArrayList<String> jobCategorys = new ArrayList<String>();
			factory.getSupportedJobCategory(jobCategorys);
			for (String c : jobCategorys) {
				Object old = this.factoryLookup.get(c);
				if (old != null) {
					LOG.warn("jobcategory registered already, former class=" + old.getClass());
					continue;
				}
				this.factoryLookup.put(c, factory);
			}
		}
    }

    public void fini() {
    	if (this.pluginContainer != null) {
    		this.pluginContainer.cleanUp();
    	}
    }

    public SubjobProcessor getSubjobProcessor(String jobCategory) {
        final String key = jobCategory;
        SubjobProcessor old = this.pluginLookup.get(key);
        if (old != null) {
            return old;
        }

        SubjobProcessorFactory factory = factoryLookup.get(jobCategory);
        if (factory == null) {
            return null;
        }

        SubjobProcessor plugin = factory.createSubjobProcessor(jobCategory);
        if (plugin != null) {
            old = this.pluginLookup.putIfAbsent(key, plugin);
            if (old != null) {
                return old;
            }
        }

        return plugin;
    }

    public void getSupportedJobCategoryList(ArrayList<String> to) {
        Collection<String> list = this.factoryLookup.keySet();
        to.addAll(list);
    }
}
