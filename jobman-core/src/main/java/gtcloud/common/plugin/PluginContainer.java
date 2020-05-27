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

    // ���ݸ�������Ķ������
    private final HashMap<String, Object> pluginOptions;

    /**
     * ����һ��PluginContainer����.
     * @param pluginDefinitionFile ��������ļ���
     * @param pluginCaption �������,������־�ļ�
     * @param filter ���������
     */
    public PluginContainer(String pluginDefinitionFile, String pluginCaption, PluginFilter filter) {
        this.pluginDefinitionFile = pluginDefinitionFile;
        this.pluginCaption = pluginCaption;
        this.filter = filter;
        this.pluginOptions = new HashMap<>();
    }

    /**
     * ����һ��PluginContainer����.
     * @param pluginDefinitionFile ��������ļ���
     * @param pluginCaption �������,������־�ļ�
     * @param filter ���������
     * @param pluginOptions ���ݸ�������Ĳ�����
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
                    LOG.warn("û�м��ص��κ�{}���", this.pluginCaption);
                }
            }
        }
        catch (Exception ex) {
            LOG.error("����{}���ʧ��: {}", this.pluginCaption, ex.getMessage());
        }
    }

    /**
     * ���ز�������ļ��������������
     * @param pluginDefnFile ��������ļ�
     * @param pluginClass ���������
     * @param pluginOptions ���ݸ�������Ĳ�����
     * @return �������
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
