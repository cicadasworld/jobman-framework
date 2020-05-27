package gtcloud.common.plugin;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.Lifecycle;
import gtcloud.common.basetypes.StatusCodeException;

public class PluginContainer {

    private static Logger LOG = LoggerFactory.getLogger(PluginContainer.class);

    private final ArrayList<Lifecycle> plugins = new ArrayList<>();

    private volatile boolean pluginsLoaded = false;

    private final String pluginDefinitionFile;
    private final String pluginCaption;

    private final PluginFilter filter;

    // 传递给各插件的额外参数
    private final HashMap<String, Object> pluginOptions;

    /**
     * 构造一个PluginContainer对象.
     * @param pluginDefinitionFile 插件定义文件名
     * @param pluginCaption 插件标题,用于日志文件
     * @param filter 插件过滤器
     */
    public PluginContainer(String pluginDefinitionFile, String pluginCaption, PluginFilter filter) {
        this.pluginDefinitionFile = pluginDefinitionFile;
        this.pluginCaption = pluginCaption;
        this.filter = filter;
        this.pluginOptions = new HashMap<>();
    }

    /**
     * 构造一个PluginContainer对象.
     * @param pluginDefinitionFile 插件定义文件名
     * @param pluginCaption 插件标题,用于日志文件
     * @param filter 插件过滤器
     * @param pluginOptions 传递给各插件的参数集
     */
    public PluginContainer(String pluginDefinitionFile, String pluginCaption, PluginFilter filter,
                           HashMap<String, Object> pluginOptions) {
        this.pluginDefinitionFile = pluginDefinitionFile;
        this.pluginCaption = pluginCaption;
        this.filter = filter;
        this.pluginOptions = pluginOptions;
    }

    public void cleanUp() {
        int n = this.plugins.size();
        for (int i = n - 1; i >= 0; -- i) {
            Lifecycle r = this.plugins.get(i);
            r.dispose();
        }
        this.plugins.clear();
    }

    public ArrayList<Lifecycle> getPlugins() {
        if (!this.pluginsLoaded) {
            loadPluginsIfNeccessary();
        }
        return this.plugins;
    }

    private void loadPluginsIfNeccessary() {
        synchronized (this) {
            if (!this.pluginsLoaded) {
                loadPlugins_i();
                this.pluginsLoaded = true;
            }
        }
    }

    private void loadPlugins_i() {
        try {
            String f = this.pluginDefinitionFile;
            ArrayList<Lifecycle> plugins = PluginLoader.loadAndInit(f, this.filter, this.pluginOptions);
            for (Lifecycle p : plugins) {
                this.plugins.add(p);
            }
            if (this.plugins.isEmpty()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("没有加载到任何{}插件", this.pluginCaption);
                }
            }
        }
        catch (Exception ex) {
            LOG.error("加载{}插件失败: {}", this.pluginCaption, ex.getMessage());
        }
    }

    /**
     * 加载插件配置文件，创建插件容器
     * @param pluginDefnFile 插件配置文件
     * @param pluginClass 插件的类型
     * @param pluginOptions 传递给各插件的参数集
     * @return 插件容器
     */
    public static PluginContainer createInstance(String pluginDefnFile, Class<?> pluginClass,
                                                 HashMap<String, Object> pluginOptions) {
        String clazzName = pluginClass.getName();
        int pos = clazzName.lastIndexOf('.');
        final String pluginTag = pos > 0 ? clazzName.substring(pos + 1) : clazzName;
        final String pluginCaption = pluginTag;

        return new PluginContainer(pluginDefnFile, pluginCaption, new PluginFilter() {
            @Override
            public boolean supportPluginTag(String tag) {
                return pluginTag.equalsIgnoreCase(tag);
            }

            @Override
            public boolean supportPluginObject(Object obj) {
                return pluginClass.isInstance(obj);
            }
        }, pluginOptions);
    }

    public static PluginContainer createInstance(String pluginDefnFile, Class<?> pluginClass) {
        return createInstance(pluginDefnFile, pluginClass, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFirstPlugin(PluginContainer container, Class<T> clazz) throws StatusCodeException {
        ArrayList<Lifecycle> plugins = null;
        if (container != null) {
            plugins = container.getPlugins();
        }
        if (plugins == null || plugins.isEmpty()) {
            throw new StatusCodeException(-1, "Could not find any " + clazz.getName());
        }
        return (T)plugins.get(0);
    }
}
