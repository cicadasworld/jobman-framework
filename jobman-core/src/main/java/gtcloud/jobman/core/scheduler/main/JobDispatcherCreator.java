package gtcloud.jobman.core.scheduler.main;

import gtcloud.common.basetypes.Lifecycle;
import gtcloud.common.plugin.PluginContainer;
import gtcloud.common.utils.PathUtils;
import gtcloud.jobman.core.scheduler.JobDispatcher;
import gtcloud.jobman.core.scheduler.JobDispatcherFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JobDispatcherCreator {

    private static Logger LOG = LoggerFactory.getLogger(JobDispatcherCreator.class);

    private PluginContainer pluginContainer = null;

    // jobCategory --> instanceOf factory
    private HashMap<String, JobDispatcherFactory> factoryLookup = new HashMap<>();

    // jobCategory --> instanceOf plugin
    private ConcurrentHashMap<String, JobDispatcher> pluginLookup = new ConcurrentHashMap<>();

    void init() throws Exception {
        String etcDir = PathUtils.getEtcDir();
        String f = etcDir + "/jobman/scheduler/jobdispatcher.xml";
        this.pluginContainer = PluginContainer.createInstance(f, JobDispatcherFactory.class);

        ArrayList<Lifecycle> plugins = this.pluginContainer.getPlugins();
        for (Lifecycle p : plugins) {
            JobDispatcherFactory factory = (JobDispatcherFactory)p;
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

    void fini() {
        if (this.pluginContainer != null) {
            this.pluginContainer.cleanUp();
        }
    }

    public Collection<String> getJobCategoryList() {
        return this.factoryLookup.keySet();
    }

    public JobDispatcherFactory getJobDispatcherFactory(String jobCategory) {
        return factoryLookup.get(jobCategory);
    }

    public JobDispatcher getJobDispatcher(String jobCategory) {
        final String key = jobCategory;
        JobDispatcher old = this.pluginLookup.get(key);
        if (old != null) {
            return old;
        }

        JobDispatcherFactory factory = factoryLookup.get(jobCategory);
        if (factory == null) {
            return null;
        }

        JobDispatcher plugin = factory.createJobDispatcher(jobCategory);
        if (plugin != null) {
            old = this.pluginLookup.putIfAbsent(key, plugin);
            if (old != null) {
                return old;
            }
        }

        return plugin;
    }
}
