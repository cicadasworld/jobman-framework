package gtcloud.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.basetypes.StatusCodeException;
import gtcloud.common.cynosure.CynosureClient;
import gtcloud.common.utils.MiscUtils;

/**
 * 提供集群中元数据库URL、master节点URL的设置与获取的统一位置。
 */
public class ClusterNodeURL {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterNodeURL.class);

    // 发布到CynosureServer的"集群主节点URL"全局参数名
    public static final String GLOBAL_PARAM_NAME_MasterNodeURL = "gtcloud.masterNodeURL";

    // -D...
    public static final String SYS_PROP_NAME_MasterNodeURL = "GTCLOUD_MASTER_NODE_URL";

    private static CynosureClient cynosureClient = null;

    // 集群主节点URL
    private static volatile String masterNodeBaseURL = null;

    public static void injectCynosureClient(CynosureClient client) {
        cynosureClient = client;
    }

    public static String getMasterNodeBaseURL() throws StatusCodeException {
        if (masterNodeBaseURL != null) {
            return masterNodeBaseURL;
        }

        // 看系统属性中是否设置了该参数
        //-DGTCLOUD_MASTER_NODE_URL=http://127.0.0.1:58091
        String symbol = SYS_PROP_NAME_MasterNodeURL;
        String addr = MiscUtils.expandSymbol(symbol);
        if (addr != null) {
            masterNodeBaseURL = normalizeMasterNodeURL(addr);
            return masterNodeBaseURL;
        }

        // 从CynosureServer的全局参数中获取
        if (cynosureClient == null) {
            throw new StatusCodeException(-1, "-D" + SYS_PROP_NAME_MasterNodeURL + "未设置");
        }

        final String name = GLOBAL_PARAM_NAME_MasterNodeURL;
        for (int i = 0; i < 10; i ++) {
            if (LOG.isInfoEnabled()) {
                LOG.info("从CynosureServer获取配置参数[{}]...", name);
            }

            PropertiesEx params = cynosureClient.fetchGlobalParams();
            String url = params.getProperty(name);
            if (url != null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("从CynosureServer获取配置参数[{}]完成,value={}", name, url);
                }
                masterNodeBaseURL = normalizeMasterNodeURL(url);
                return masterNodeBaseURL;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                throw new StatusCodeException(-1, ex);
            }
        }

        if (LOG.isErrorEnabled()) {
            LOG.error("从CynosureServer获取配置参数[{}]失败.", name);
        }
        throw new StatusCodeException(-1, String.format("从CynosureServer获取配置参数[%s]失败.", name));
    }

    static private String normalizeMasterNodeURL(String serverURL) {
        serverURL = serverURL.trim();
        if (!serverURL.startsWith("http://") && !serverURL.startsWith("https://")) {
            serverURL = "http://" + serverURL;
        }
        while (serverURL.endsWith("/")) {
            int len = serverURL.length();
            serverURL = serverURL.substring(0, len-1);
        }
        return serverURL;
    }

}
