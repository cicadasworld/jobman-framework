package platon;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class ClassUtil {

    public static String getClassURL(Class<?> clazz) {
        String name = clazz.getName().replace('.', '/') + ".class";
        URL url = clazz.getClassLoader().getResource(name);
        if (url == null)
            return null;
        String classURL = url.toString();
        try {
            return URLDecoder.decode(classURL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return classURL;
        }
    }

    // ȷ����ǰ���.class�ļ���.jar�ļ����ڵ�Ŀ¼
    public static String getClassFileSystemLocation(Class<?> clazz) {
        final String className = clazz.getName().replace('.', '/') + ".class";
        final String classURL = getClassURL(clazz);
        String classPath = null;
        if (classURL.startsWith("file:/")) {
            // ���ļ�Ϊչ����ʽ
            // file:/W:/gt4wss/build/classes/platon/ClassUtil.class
            //                              ^
            String path = classURL.substring(6);
            int pos = path.indexOf(className);
            classPath = path.substring(0, pos);
        }
        else if (classURL.startsWith("jar:file:/")) {
            // ���ļ�Ϊjar�����ʽ
            // jar:file:/C:/Temp/extern/platon.jar!/platon/ClassUtil.class
            //                         ^
            String path = classURL.substring(10);
            int pos = path.indexOf("!/" + className);
            path = path.substring(0, pos);
            pos = path.lastIndexOf('/');
            classPath = path.substring(0, pos + 1);
        }
        else {
            classPath = "";
        }

        return classPath;
    }
}
