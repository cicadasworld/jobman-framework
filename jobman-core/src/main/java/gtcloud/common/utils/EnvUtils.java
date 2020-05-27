package gtcloud.common.utils;

import java.io.File;

public class EnvUtils {

    // ����������ģʽ���򽫹ؼ������������ó�Ĭ��ֵ
    public static void injectKeySystemProperties(String appName) {
        //-----------------------------
        // ��ñ���ip��ַ������ϵͳ������
        //-----------------------------
        String key = "gtcloud.local-ip";
        if (System.getProperty(key) == null) {
            String ip = NetUtils.getLocalIPv4();
            System.setProperty(key, ip);
        }

        //-------------------------------------------
        // ����ǰΪ����������������һЩ��Ҫ��ϵͳ����
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
