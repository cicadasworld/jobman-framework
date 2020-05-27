package gtcloud.springutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import gtcloud.common.basetypes.PropertiesEx;

public class SpringEnvUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SpringEnvUtils.class);

    public static String figureOutLocalBaseURL(Environment env) throws Exception {
        String port;
        {
            String name = "local.server.port";
            port = env.getProperty(name);
            if (port == null) {
                String emsg = String.format("从全局环境中找不到必需属性：%s", name);
                LOG.error(emsg);
                throw new Exception(emsg);
            }
        }

        String ip;
        {
            String name = "gtcloud.local-ip";
            ip = env.getProperty(name);
            if (ip == null) {
                String emsg = String.format("从全局环境中找不到必需属性：%s", name);
                LOG.error(emsg);
                throw new Exception(emsg);
            }
        }

        String baseURL = String.format("http://%s:%s", ip, port);
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath != null) {
            if (!contextPath.startsWith("/")) {
                baseURL += "/";
            }
            baseURL += contextPath;
        }
        return baseURL;
    }

    public static void injectLocalFileGetServerPort(Environment env) {
        // 本地的filegetserver.exe的端口号
        String str = env.getProperty("gtcloud.geodata.localFileGetServerPort");
        if (str != null) {
            int port = Integer.parseInt(str);
            if (port > 0) {
                PropertiesEx.GLOBAL.setProperty_Int("GTCLOUD_LOCAL_FILEGETSERVER_PORT", port);
            }
        }
    }
}
