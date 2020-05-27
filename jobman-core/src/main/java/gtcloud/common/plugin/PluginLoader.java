package gtcloud.common.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.Lifecycle;
import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.basetypes.XmlNode;
import gtcloud.common.utils.PathUtils;

public class PluginLoader {

    private static Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    public static class PluginItem {
        // 插件对象
        private Object object;

        // 将传递给插件的初始化参数
        private PropertiesEx paramemter = new PropertiesEx();

        public Object getObject() {
            return object;
        }

        public PropertiesEx getParamemter() {
            return paramemter;
        }
    }

    /**
     * 从插件定义文件中加载插件项列表.
     * @param pluginDefnFileName 插件定义文件
     * @param filter 用来过滤不需要的插件
     * @return 插件项列表.
     */
    public static ArrayList<PluginItem> load(String pluginDefnFileName, PluginFilter filter) {
        ArrayList<String> defnFileNames = PathUtils.getSatelliteFileNames(pluginDefnFileName);
        ArrayList<PluginItem> result =  new ArrayList<>();
        if (defnFileNames.size() == 0) {
            LOG.error("cannot find file {}.", pluginDefnFileName);
            return result;
        }

        for (String defnFileName : defnFileNames) {
            loadPlugins(defnFileName, result, filter);
        }
        return result;
    }

    /**
     * 初始化插件列表
     * @param pluginItems 待初始化的插件项列表;
     * @param filter 用来过滤不需要的插件;
     * @param options 插件的可选参数.
     * @return 初始化后的插件列表.
     */
    public static ArrayList<Lifecycle> initPlugins(ArrayList<PluginItem> pluginItems,
    		                                       PluginFilter filter,
                                                   HashMap<String, Object> options) {
        ArrayList<Lifecycle> result = new ArrayList<>();
        for (PluginItem pi : pluginItems) {
            Object obj = pi.getObject();
            if (obj == null) {
                continue;
            }
            if (filter != null && !filter.supportPluginObject(obj)) {
                continue;
            }

            String className = obj.getClass().getName();
            if (!(obj instanceof Lifecycle)) {
                LOG.error("{} is not of type {}", className, Lifecycle.class.getName());
                continue;
            }

            Lifecycle p = (Lifecycle)obj;
            try {
                p.initialize(pi.getParamemter(), options);
            } catch (Exception e) {
                LOG.error("initialize() failed: {}", className);
                continue;
            }

            result.add(p);
        }

        return result;
    }

    public static ArrayList<Lifecycle> loadAndInit(String pluginDefnFileName,
    		                                       PluginFilter filter,
                                                   HashMap<String, Object> options) {
        ArrayList<PluginItem> vec = load(pluginDefnFileName, filter);
        return initPlugins(vec, filter, options);    	
    }
    
    private static void loadPlugins(String defnFileName, ArrayList<PluginItem> result, PluginFilter filter) {
        XmlNode rootNode = null;
        try {
            rootNode = XmlNode.parseXmlFile(defnFileName);
        } catch (Exception e) {
            LOG.error("load " + defnFileName + " failed.", e);
            return;
        }

        for (XmlNode ch : rootNode.getChildren()) {
            if (!ch.getName().equals("plugin")) {
                continue;
            }
            PluginItem pi = loadPluginItem(ch, filter);
            if (pi != null) {
                result.add(pi);
            }
        }
    }

    private static PluginItem loadPluginItem(XmlNode ch, PluginFilter filter) {
        //
        //<plugin>
        //<param name="enabled" value="true" />
        //<param name="tag" value="repoWriter" />
        //<param name="className" value="xxx" />
        //<param name="p1" value="v1" />
        //</plugin>
        //
        PluginItem pi = new PluginItem();
        PropertiesEx plugParams = pi.getParamemter();
        XmlNode.xmlParamsToProperties(ch, plugParams);

        String enabled = plugParams.getProperty("enabled");
        if (enabled != null && (enabled.equalsIgnoreCase("false") || enabled.equals("0"))) {
            return null;
        }

        String tag = plugParams.getProperty("tag");
        if (filter != null && !filter.supportPluginTag(tag)) {
            return null;
        }

        String className = plugParams.getProperty("className");
        if (className == null) {
            return null;
        }

        // 移除已经取过的属性
        plugParams.remove("enabled");
        plugParams.remove("tag");
        plugParams.remove("className");

        try {
            Class<?> clazz = Class.forName(className);
            pi.object = clazz.newInstance();
        } catch (Exception e) {
            LOG.error("loadPlugin() failed.", e);
            pi = null;
        }

        return pi;
    }

}
