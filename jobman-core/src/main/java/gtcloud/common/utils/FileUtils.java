package gtcloud.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class FileUtils {

    public static byte[] loadFile(String fileName) throws IOException {

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

    public static void copyFile(String srcFileName, String destFileName, int chunkSize) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(srcFileName);
            copyFile(fis, destFileName, chunkSize);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception ex) {}
            }
        }
    }

    public static void copyFile(InputStream srcInputStream, String destFileName, int chunkSize) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destFileName);
            byte[] chunk = new byte[chunkSize];

            for (;;) {
                int n = srcInputStream.read(chunk);
                if (n <= 0) {
                    break;
                }
                fos.write(chunk, 0, n);
            }
        }
        finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception ex) {}
            }
        }
    }

    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     * Closes both streams when done.
     * @param in the stream to copy from
     * @param out the stream to copy to
     * @param chunkSize size of read buffer
     * @return the number of bytes copied
     * @throws IOException in case of I/O errors
     */
    public static int copy(InputStream in, OutputStream out, int chunkSize) throws IOException {
        if(in == null) throw new IllegalArgumentException("No InputStream specified");
        if(out == null) throw new IllegalArgumentException("No OutputStream specified");
        try {
            int byteCount = 0;
            byte[] buffer = new byte[chunkSize];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ex) {
            }
            try {
                out.close();
            }
            catch (IOException ex) {
            }
        }
    }

    /**
     * 删除给定的目录。
     * @param odir 目录对象。
     */
    public static void removeDirectory(File odir, boolean removeStartDir) {
        File[] child = odir.listFiles();
        if (child != null) {
            for (File of : child) {
                if (of.isFile()) {
                    of.delete();
                } else if (of.isDirectory()) {
                    removeDirectory(of, true);
                }
            }
        }
        if (removeStartDir) {
            odir.delete();
        }
    }

    public static void copyDirectory(File fromDir, File toDir) throws Exception {
        if (!fromDir.exists()) {
            throw new Exception("dir " + fromDir + " not exist");
        }

        if (!toDir.exists()) {
            toDir.mkdirs();
        }
        toDir.setLastModified(fromDir.lastModified());

        File[] children = fromDir.listFiles();
        if (children == null) {
            children = new File[0];
        }
        for (File fromChild : children) {
            if (fromChild.isFile()) {
                File fromFile = fromChild;
                File toFile = new File(toDir, fromFile.getName());

                String fromFileName = fromFile.getAbsolutePath();
                String toFileName = toFile.getAbsolutePath();
                int chunkSize = fromFile.length() < (128*1024) ? 1024 : (2*1024*1024);

                copyFile(fromFileName, toFileName, chunkSize);
                toFile.setLastModified(fromFile.lastModified());
            }
            else if (fromChild.isDirectory()) {
                File toChildDir = new File(toDir, fromChild.getName());
                copyDirectory(fromChild, toChildDir);
            }
        }
    }

    public static class MountPointInfo {
        // 挂载点路径, 如 F:\ 或  /home
        private final String mountPointPath;

        // 文件系统类型, 如 cifs/ext4/ntfs等
        private final String fsType;

        // 挂载点所在分区的总空间及剩余空间(以字节为单位)
        private final long totalSpace;
        private final long freeSpace;

        // 是否只读
        private final boolean readOnly;

        private MountPointInfo(String path, String fsType, long total, long free, boolean readOnly) {
            this.mountPointPath = path;
            this.fsType = fsType;
            this.totalSpace = total;
            this.freeSpace = free;
            this.readOnly = readOnly;
        }

        public String getMountPointPath() {
            return mountPointPath;
        }

        public String getFileSystemType() {
            return fsType;
        }

        public long getTotalSpace() {
            return totalSpace;
        }

        public long getFreeSpace() {
            return freeSpace;
        }

        public boolean isReadOnly() {
            return this.readOnly;
        }
    }

    // 返回当前系统中的所有磁盘分区的挂载点信息.
    // 返回  挂载点路径 --> MountPointInfo 的映射表
    public static Map<String, MountPointInfo> getDiskMountPoints() throws Exception {
        HashMap<String, MountPointInfo> result = new HashMap<String, MountPointInfo>();
        if (MiscUtils.isWindows()) {
            getDiskMountPointsWindows(result);
        } else {
            getDiskMountPointsLinux(result);
        }

        return result;
    }

    private static void getDiskMountPointsWindows(HashMap<String, MountPointInfo> result) {
        File[] fsRoots = File.listRoots();
        if (fsRoots == null || fsRoots.length == 0) {
            return;
        }

        for (File fsRoot : fsRoots) {
            final long total = fsRoot.getTotalSpace();
            if (total == 0) {
                // subst等工具虚拟出来的"假盘"
                continue;
            }

            final long free = fsRoot.getFreeSpace();
            final String mountPoint = fsRoot.getAbsolutePath();
            String fsType = "NTFS";
            boolean readOnly = false;
            try {
                Path p = Paths.get(mountPoint);
                FileStore store = Files.getFileStore(p);
                fsType = store.type();
                readOnly = store.isReadOnly();
            } catch (Exception e) {
                e.printStackTrace();
            }
            MountPointInfo mpi = new MountPointInfo(mountPoint, fsType, total, free, readOnly);
            result.put(mountPoint, mpi);
        }
    }

    private final static HashSet<String> LINUX_FS_TYPES_TO_IGNORE = new HashSet<String>();
    static {
        LINUX_FS_TYPES_TO_IGNORE.add("autofs");
        LINUX_FS_TYPES_TO_IGNORE.add("binfmt_misc");
        LINUX_FS_TYPES_TO_IGNORE.add("cgroup");
        LINUX_FS_TYPES_TO_IGNORE.add("configfs");
        LINUX_FS_TYPES_TO_IGNORE.add("debugfs");
        LINUX_FS_TYPES_TO_IGNORE.add("devpts");
        LINUX_FS_TYPES_TO_IGNORE.add("devtmpfs");
        LINUX_FS_TYPES_TO_IGNORE.add("fuse.gvfsd-fuse");
        LINUX_FS_TYPES_TO_IGNORE.add("fusectl");
        LINUX_FS_TYPES_TO_IGNORE.add("hugetlbfs");
        LINUX_FS_TYPES_TO_IGNORE.add("mqueue");
        LINUX_FS_TYPES_TO_IGNORE.add("nfsd");
        LINUX_FS_TYPES_TO_IGNORE.add("proc");
        LINUX_FS_TYPES_TO_IGNORE.add("pstore");
        LINUX_FS_TYPES_TO_IGNORE.add("rootfs");
        LINUX_FS_TYPES_TO_IGNORE.add("rpc_pipefs");
        LINUX_FS_TYPES_TO_IGNORE.add("securityfs");
        LINUX_FS_TYPES_TO_IGNORE.add("selinuxfs");
        LINUX_FS_TYPES_TO_IGNORE.add("sysfs");
        LINUX_FS_TYPES_TO_IGNORE.add("tmpfs");
        LINUX_FS_TYPES_TO_IGNORE.add("vmhgfs");
    }

    private static void getDiskMountPointsLinux(HashMap<String, MountPointInfo> result) throws Exception {
        FileSystem fs = FileSystems.getDefault();
        for (FileStore store: fs.getFileStores()) {
            final long total = store.getTotalSpace();
            if (total == 0) {
                continue;
            }

            String storeType = store.type(); // ext4, xfs ...
            if (LINUX_FS_TYPES_TO_IGNORE.contains(storeType)) {
                continue;
            }

            // / (/dev/sda3)
            String storeString = store.toString(); // "/ (/dev/sda3)", "/run (tmpfs)"
            int pos = storeString.indexOf('(');
            if (pos < 1) {
                continue;
            }
            String mountPoint = storeString.substring(0, pos).trim();
            if (mountPoint.isEmpty() || "/boot".equals(mountPoint)) {
                continue;
            }

            final long free = store.getUsableSpace();
            final boolean readOnly = store.isReadOnly();
            MountPointInfo mpi = new MountPointInfo(mountPoint, storeType, total, free, readOnly);
            result.put(mountPoint, mpi);
        }
    }

    /**
     * 将全路径形式文件名拆解长路径与文件名部分
     * @param fullFileName
     * @return
     */
    public static String[] splitFullFileName(String fullFileName) {
        int index = fullFileName.lastIndexOf('\\');
        if (index < 0) {
            index = fullFileName.lastIndexOf('/');
        }
        if (index < 0) {
            return new String[] {"", fullFileName};
        }
        String path = fullFileName.substring(0, index);
        String f = fullFileName.substring(index + 1);
        return new String[] {path, f};
    }

    /**
     * 在系统临时目录里产生一个随机名的目录
     * @return
     */
    public static File getTempDir() {
        File tempDirRoot = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(tempDirRoot, UUID.randomUUID().toString());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }
}
