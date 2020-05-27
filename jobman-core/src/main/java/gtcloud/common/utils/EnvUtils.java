package gtcloud.common.utils;

import java.io.File;

public class EnvUtils {

    // 若不是生产模式，则将关键环境变量设置成默认值
    public static void injectKeySystemProperties(String appName) {
        //-----------------------------
        // 获得本地ip地址，放入系统属性中
        //-----------------------------
        String key = "gtcloud.local-ip";
        if (System.getProperty(key) == null) {
            String ip = NetUtils.getLocalIPv4();
            System.setProperty(key, ip);
        }

        //-------------------------------------------
        // 若当前为开发环境，则设置一些重要的系统属性
        //-------------------------------------------
        // (1)
        System.setProperty("GTCLOUD_APP_NAME", appName);

        // (2)
        String appBaseDirName = System.getProperty("GTCLOUD_APP_BASE");
        if (appBaseDirName == null) {
            appBaseDirName = System.getenv("GTCLOUD_APP_BASE");
            if (appBaseDirName == null) {
                appBaseDirName = "C:\\Temp\\app";
            }
            System.setProperty("GTCLOUD_APP_BASE", appBaseDirName);
        }

        // (3)
        String logsDirName = System.getProperty("GTCLOUD_LOG_DIR");
        if (logsDirName == null) {
            logsDirName = System.getenv("GTCLOUD_LOG_DIR");
            if (logsDirName == null) {
                logsDirName = appBaseDirName + "\\var\\logs";
            }
            System.setProperty("GTCLOUD_LOG_DIR", logsDirName);
        }
        File logsDir = new File(logsDirName);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
    }
}
