package platon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackedLibraryLoader {

    private static final Class<?> CURRENT_CLASS = PackedLibraryLoader.class;

    /**
     * ���ظ���������ѹ������һ��������̬��.
     *
     * @param archiveStemName ѹ��������, �� "placename_jni", �ڲ�ͬ��ƽ̨�Ͻ���չ�ɶ�Ӧ��ѹ�����ĵ��ļ���, �� "placename_jni-windows-x64.zip";
     * @param unpackDestDir ��ѹ���ĸ�Ŀ¼��, ��Ϊnull��ʹ����ʱĿ¼
     * @param libNamesToLoad ��Ҫ���ص�һ��������̬����, �� "gis_placenamewrapper4java", ����.dll/.so��׺, linux��Ҳ����"lib"ǰ׺.
     * 
     * @throws Exception ����ʧ�ܽ��׳��쳣
     */
    public static void unpackAndLoadLibrary(String archiveStemName,
                                            String unpackDestDir,
                                            String[] libNamesToLoad) throws Exception {
        // ȷ��ѹ������ȫ·��Ȼ������ļ�.
        // ����Լ��, ѹ����Ӧ���ڵ�ǰ���.class�ļ�����Ӧ��classpath��, ����.jar�ļ����ڵ�Ŀ¼��
        String f = getCurrentClassFSLocation() + "/" + getArchivePlatSpecificName(archiveStemName);
        final byte[] content = loadFile(f);

        if (unpackDestDir == null) {
            String md5sum = calcMd5Sum(content, 0, content.length);
            String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
            String p = tempFolder + "/" + md5sum + "-" + archiveStemName;
            File odir = new File(p);
            unpackDestDir = odir.getAbsolutePath();
            if (odir.exists()) {
                // Ŀ¼�Ѿ�����, ˵��֮ǰ��ѹ����, ��β���Ҫ�ٽ�ѹ
                ;
            }
            else {
                odir.mkdirs();
                unpack(content, unpackDestDir);
            }
        }
        else {
            // �����߸����˽�ѹĿ¼, ���ǽ��н�ѹ
            unpack(content, unpackDestDir);
        }

        // װ�ظ�����̬��
        for (String libName : libNamesToLoad) {
            String libFileName = System.mapLibraryName(libName);
            File of = new File(unpackDestDir + "/" + libFileName);
            String libFullPathName = of.getAbsolutePath();
            if (!of.exists()) {
                continue;
            }
            try {
                System.out.println("load " + libFullPathName);
                System.load(libFullPathName);
            }
            catch (Exception e) {
                System.err.println("Unable to load " + libFullPathName + ": " + e);
                throw e;
            }
        }
    }

    private static String getArchivePlatSpecificName(final String archiveStemName) {
        //
        // �鿴 System.getProperties().list(System.out)�����
        //
        // Windows XP : os.arch=x86 os.name=Windows XP sun.arch.data.model=32
        // Windows 2008 Server: os.arch=x86 os.name=Windows Server 2008 R2
        // sun.arch.data.model=32
        // x86-kyin32 : os.arch=i386 os.name=Linux sun.arch.data.model=32
        // x86-kyin64 : os.arch=amd64 os.name=Linux sun.arch.data.model=64
        // longxin-kylin64 : os.arch=mips64el os.name=Linux
        // sun.arch.data.model=64
        // longxin-kylin32 : os.arch=mipsel os.name=Linux sun.arch.data.model=32
        // shenwei-kylin64 : os.arch=sw os.name=Linux sun.arch.data.model=64
        //
        String osName = System.getProperties().getProperty("os.name").toLowerCase();
        String osArch = System.getProperties().getProperty("os.arch").toLowerCase();
        String bits = System.getProperties().getProperty("sun.arch.data.model");

        String osId = getOsId(osName);
        String archId = "";
        if (osId.equals("windows")) {
            archId = bits.equals("64") ? "x64" : "x86";
        } else {
            archId = osArch;
            if (bits.equals("64") && archId.indexOf(bits) < 0) {
                archId += bits;
            }
        }

        // xxx-windows-x64.zip
        final String archiveName = String.format("%s-%s-%s.zip", archiveStemName, osId, archId);
        return archiveName;
    }

    private static String getOsId(String lowerCaseOsName) {
        if (lowerCaseOsName.contains("windows")) {
            return "windows";
        }
        else if (lowerCaseOsName.contains("mac")) {
            return "mac";
        }
        else if (lowerCaseOsName.contains("linux")) {
            return "linux";
        }
        else {
            return lowerCaseOsName.replaceAll("\\W", "");
        }
    }

    // ȷ����ǰ���.class�ļ���.jar�ļ����ڵ�Ŀ¼
    private static String getCurrentClassFSLocation() {
    	return ClassUtil.getClassFileSystemLocation(CURRENT_CLASS);
    }

    private static byte[] loadFile(String fileName) throws IOException {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            final int fileSize = fis.available();
            byte[] buffer = new byte[fileSize];
            int offset = 0;

            int totalRead = 0;
            int nleft = fileSize;
            for (;;) {
                int nread = fis.read(buffer, offset , nleft);
                if (nread <= 0) {
                    break;
                }
                offset += nread;
                nleft -= nread;
                totalRead += nread;
            }

            if (totalRead != fileSize) {
                byte[] b = new byte[totalRead];
                System.arraycopy(buffer, 0, b, 0, totalRead);
                buffer = b;
            }

            return buffer;
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    private static String calcMd5Sum(byte[] data, int offset, int len) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data, offset, len);
            byte[] m = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<m.length; ++i) {
                int n = m[i] & 0xff;
                sb.append(String.format("%02x", n));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available: " + e);
        }
    }

    private static void unpack(byte[] content, String unpackDestDir) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(content);
        ZipInputStream zis = new ZipInputStream(in);
        for (;;) {
            ZipEntry ze = zis.getNextEntry();
            if (ze == null) {
                break;
            }
            if (ze.isDirectory()) {
                String path = unpackDestDir + "/" + ze.getName();
                File odir = new File(path);
                odir.mkdirs();
            }
            else {
                String fileName = unpackDestDir + "/" + ze.getName();
                unpackCurrentZipEntryToFile(zis, fileName);
            }
            zis.closeEntry();
        }
        zis.close();
    }

    private static void unpackCurrentZipEntryToFile(ZipInputStream zis, String fileName) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            byte[] chunk = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = zis.read(chunk)) != -1) {
                fos.write(chunk, 0, bytesRead);
            }
        }
        finally {
            if (fos != null) {
                fos.close();
            }
        }

        if (fileName.endsWith(".so") && !System.getProperty("os.name").contains("Windows")) {
            try {
                Runtime.getRuntime().exec(
                        new String[] { "chmod", "755", fileName }
                ).waitFor();
            }
            catch (Throwable e) {
                ;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        unpackAndLoadLibrary("placename_jni", null, new String[] {"gis_placenamewrapper4java"});
    }
}
