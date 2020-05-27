package gtcloud.common.utils;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.basetypes.StatusCodeException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import platon.PropSet;

public class PathUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PathUtils.class);

    private static final String SYSDATA_DIR = "sysdata";

    public static void dump() throws StatusCodeException {
        System.out.format("GTCLOUD_APP_NAME=%s%n", MiscUtils.expandSymbol("GTCLOUD_APP_NAME"));
        System.out.format("GTCLOUD_APP_BASE=%s%n", getAppBaseDir());
        System.out.format("GTCLOUD_LOG_DIR=%s%n", getLogDir());
        System.out.format("GTCLOUD_ETC_DIR=%s%n", getEtcDir());
        System.out.format("GTCLOUD_VAR_DIR=%s%n", getVarDir());
        System.out.format("GTCLOUD_TMP_DIR=%s%n", getTempDir());
        System.out.format("GTCLOUD_SYSDATA_DIR=%s%n", getSysDataDir());
    }

    public static void putTo(PropSet props) throws StatusCodeException {
        props.put("GTCLOUD_APP_NAME", MiscUtils.expandSymbol("GTCLOUD_APP_NAME"));
        props.put("GTCLOUD_APP_BASE", getAppBaseDir());
        props.put("GTCLOUD_LOG_DIR", getLogDir());
        props.put("GTCLOUD_ETC_DIR", getEtcDir());
        props.put("GTCLOUD_VAR_DIR", getVarDir());
        props.put("GTCLOUD_TMP_DIR", getTempDir());
        props.put("GTCLOUD_SYSDATA_DIR", getSysDataDir());
    }

    public static String getAppBaseDir() throws StatusCodeException {
        String dirSymbol = "GTCLOUD_APP_BASE";
        String appBaseDir = getWellknownDir(dirSymbol, null);
        if (appBaseDir == null) {
            String msg = "cannot get env-var: " + dirSymbol;
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            throw new StatusCodeException(-1, msg);
        }
        return getCanonicalPath(appBaseDir);
    }

    public static String getLogDir() throws StatusCodeException {
        String dirSymbol = "GTCLOUD_LOG_DIR";
        String logDir = System.getProperty(dirSymbol);
        if (logDir == null) {
            String msg = "cannot get -D" + dirSymbol;
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            throw new StatusCodeException(-1, msg);
        }
        return getCanonicalPath(logDir);
    }

    public static String getEtcDir() throws StatusCodeException {
        String dirSymbol = "GTCLOUD_ETC_DIR";
        String etcDir = getWellknownDir(dirSymbol, null);
        if (etcDir != null) {
            return getCanonicalPath(etcDir);
        }
        return getAppBaseDir() + File.separator + "webetc";
    }

    public static String getVarDir() {
        String dirSymbol = "GTCLOUD_VAR_DIR";
        String varDir = getWellknownDir(dirSymbol, null);
        if (varDir != null) {
            return getCanonicalPath(varDir);
        }
        try {
            return getAppBaseDir() + File.separator + "var";
        } catch (Exception e) {
            return MiscUtils.isWindows() ? System.getenv("TEMP") : "/tmp";
        }
    }

    public static String getTempDir() {
        String dirSymbol = "GTCLOUD_TMP_DIR";
        String tempDir = getWellknownDir(dirSymbol, null);
        if (tempDir != null) {
            return getCanonicalPath(tempDir);
        }
        return getVarDir() + File.separator + "temp";
    }

    public static String getXappsDir() throws StatusCodeException {
        String dirSymbol = "GTCLOUD_XAPPS_DIR";
        String xappsDir = getWellknownDir(dirSymbol, null);
        if (xappsDir != null) {
            return getCanonicalPath(xappsDir);
        }
        return getAppBaseDir() + File.separator + "xapps";
    }

    public static String getSysDataDir() throws StatusCodeException {
        //
        // 若存在系统变量GTCLOUD_SYSDATA_DIR，则使用该变量指定的目录；
        // 否则，放置在与GTCLOUD_APP_BASE目录平级的位置，
        //
        String dirSymbol = "GTCLOUD_SYSDATA_DIR";
        String sysDataDir = getWellknownDir(dirSymbol, null);
        if (sysDataDir != null) {
            return getCanonicalPath(sysDataDir);
        }

        String appBaseDir = getAppBaseDir();
        String p = appBaseDir.replace('\\', '/');
        int pos = p.lastIndexOf('/');
        File dirObj = new File(appBaseDir.substring(0, pos), SYSDATA_DIR);
        if (!dirObj.exists()) {
            dirObj.mkdirs();
        }
        return dirObj.getAbsolutePath();
    }

    public static String getDataStoreConfigDir() throws StatusCodeException {
        String dirSymbol = "GTCLOUD_DATASTORE_CONFIG_DIR";
        String dataStoreConfigDir = getWellknownDir(dirSymbol, null);
        if (dataStoreConfigDir != null) {
            return getCanonicalPath(dataStoreConfigDir);
        }
        return getSysDataDir() + File.separator + "datastore";
    }

    private static String getWellknownDir(String dirSymbol, String defaultDir) {
        String dirName = PropertiesEx.GLOBAL.getProperty(dirSymbol);
        if (dirName == null) {
            dirName = System.getProperty(dirSymbol);
        }
        if (dirName == null) {
            dirName = System.getenv(dirSymbol);
        }
        if (dirName != null) {
            return dirName;
        } else {
            return defaultDir;
        }
    }

    private static String getCanonicalPath(String path) {
        try {
            return (new File(path)).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    public static ArrayList<String> getSatelliteFileNames(String fileName) {
        String dirName = null;
        String name = null;
        String nameStem = null;
        String ext = "";
        if (true) {
            File of = new File(fileName);
            dirName = of.getAbsolutePath();
            nameStem = name = of.getName();
            int pos = name.lastIndexOf('.');
            if (pos > 0) {
                nameStem = name.substring(0, pos);
                ext = name.substring(pos); // .xml
            }
            of = null;
        }

        ArrayList<String> result = new ArrayList<String>();

        File[] children = null;
        if (true) {
            int pos = dirName.lastIndexOf(File.separatorChar);
            if (pos > 0) {
                dirName = dirName.substring(0, pos);
            }

            File odir = new File(dirName);
            children = odir.listFiles();
            if (children == null) {
                return result;
            }
        }

        final String pattern = nameStem + "_$";
        for (File of : children) {
            if (!of.isFile()) {
                continue;
            }

            String p = of.getAbsolutePath();
            String fname = of.getName();
            if (fname.equals(name)) {
                result.add(p);
                continue;
            }

            if (ext.length() > 0) {
                if (fname.startsWith(pattern) && fname.endsWith(ext)) {
                    result.add(p);
                }
            } else {
                if (fname.startsWith(pattern) && fname.lastIndexOf('.') < 0) {
                    result.add(p);
                }
            }
        }

        if (result.size() < 2) {
            return result;
        }

        // 将文件名升序排序
        final boolean isAscending = true;
        Collections.sort(result, new Comparator<String>() {
            public int compare(String a, String b) {
                int n = a.compareTo(b);
                n = isAscending ? n : (0 - n);
                return (n == 0) ? 0 : ( n > 0 ? 1 : (-1));
            }
        });

        return result;
    }

    // 判断给定的文件名是否是一个片段文件的名称.
    // fileName: global-image_$20560707.tly
    public static boolean isSatelliteFileName(String fileName) {
        return fileName.contains("_$");
    }

    // 由片段文件名获得对应的主文件名.
    // 形如, 若 satelliteFileName 为 "global-image_$20560707.tly", 则主文件名为 "global-image.tly"
    public static String getMainFileName(String satelliteFileName) {
        int pos1 = satelliteFileName.indexOf("_$");
        if (pos1 <= 0) {
            return satelliteFileName;
        }

        String stemName = satelliteFileName.substring(0, pos1);
        String fileExtName = "";

        final int fromIndex = pos1 + 2;
        int pos2 = satelliteFileName.indexOf('.', fromIndex);
        if (pos2 > 0) {
            fileExtName = satelliteFileName.substring(pos2);
        }

        return stemName + fileExtName;
    }

    /**
     * 将路径中包含的符号扩展成具体的值，如"${GTCLOUD_APP_BASE}/1.exe"扩展成"C:\GTCloud-2.0\mapserver\1.exe"
     * @param path
     * @return
     * @throws StatusCodeException
     */
    public static String expandPath(String path) throws StatusCodeException {
        int startPos = path.indexOf("${");
        if (startPos < 0) {
            return path;
        }

        int endPos = path.indexOf("}", startPos+2);
        if (endPos < 0) {
            return path;
        }

        String symbol = path.substring(startPos+2, endPos);
        String v = null;
        switch (symbol.toUpperCase()) {
            case "GTCLOUD_APP_BASE": v = getAppBaseDir(); break;
            case "GTCLOUD_LOG_DIR": v = getLogDir(); break;
            case "GTCLOUD_ETC_DIR": v = getEtcDir(); break;
            case "GTCLOUD_VAR_DIR": v = getVarDir(); break;
            case "GTCLOUD_TMP_DIR": v = getTempDir(); break;
            case "GTCLOUD_XAPPS_DIR": v = getXappsDir(); break;
            case "GTCLOUD_SYSDATA_DIR": v = getSysDataDir(); break;
            case "GTCLOUD_DATASTORE_CONFIG_DIR": v = getDataStoreConfigDir(); break;
        }
        if (v == null) {
            v = PropertiesEx.GLOBAL.getProperty(symbol);
            if (v == null) {
                v = System.getProperty(symbol);
            }
            if (v == null) {
                v = System.getenv(symbol);
            }
        }
        if (v == null) {
            v = "";
        }

        String prefix = startPos == 0 ? "" : path.substring(0, startPos);
        String suffix = path.substring(endPos + 1);
        String p = prefix + v + suffix;

         //继续替换剩余的符号
        return expandPath(p);
    }

}
