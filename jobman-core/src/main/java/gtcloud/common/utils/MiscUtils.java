package gtcloud.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ThreadFactory;

import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.basetypes.StatusCodeException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import platon.ByteStream;
import platon.DefreezeException;
import platon.FreezeException;
import platon.FreezerJSON;
import platon.JsonNode;
import platon.Message;

public class MiscUtils {

    private static boolean IS_WINDOWS = false;

    static {
        String osName = System.getProperties().getProperty("os.name").toLowerCase();
        IS_WINDOWS = osName.contains("windows");
    }

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static void emitString(OutputStream os, String line) throws IOException {
        byte[] v = line.getBytes();
        os.write(v, 0, v.length);
    }

    public static byte[] freezeMessageToBlob(Message msg) throws FreezeException {
        ByteStream stream = new ByteStream();
        msg.freeze(stream);
        byte[] blob = new byte[stream.length()];
        System.arraycopy(stream.array(), 0, blob, 0, blob.length);
        return blob;
    }

    public static byte[] byteStreamToBytes(ByteStream stream) {
        byte[] bvec = stream.array();
        byte[] blob;
        int len = stream.length();
        if (len == bvec.length) {
            blob = bvec;
        } else {
            blob = new byte[len];
            System.arraycopy(bvec, 0, blob, 0, len);
        }
        return blob;
    }

    public static boolean isUTF8(byte[] bytes) {
        int single_byte_chars = 0;
        int two_byte_chars = 0;
        int three_byte_chars = 0;
        int four_byte_chars = 0;
        //int error_single_byte_chars = 0;

        // 按照UTF-8的规定，除了最高的一个字节外，其余的所有字节均以10开头;
        // 最高字节的开头，110表示连续2位，1110表示连续3位，11110表示连续4位。
        byte[] v = bytes;
        int nleft = bytes.length;
        int p = 0;
        while (nleft > 0) {
            if ((v[p+0] & 0x80) == 0) {
                single_byte_chars ++;
                nleft --;
                p ++;
                continue;
            }

            if ((v[p+0] & 0xe0) == 0xc0) {
                // 110xxxxx
                if (nleft >= 2 && is_u8c(v[p+1])) {
                    two_byte_chars ++;
                    nleft -= 2;
                    p += 2;
                    continue;
                }
            }

            else if ((v[p+0] & 0xf0) == 0xe0) {
                // 1110xxxx
                if (nleft >= 3 && is_u8c(v[p+1]) && is_u8c(v[p+2])) {
                    three_byte_chars ++;
                    nleft -= 3;
                    p += 3;
                    continue;
                }
            }

            else if ((v[p+0] & 0xf8) == 0xf0) {
                // 11110xxx
                if (nleft >= 4 && is_u8c(v[p+1]) && is_u8c(v[p+2]) && is_u8c(v[p+3])) {
                    four_byte_chars ++;
                    nleft -= 4;
                    p += 4;
                    continue;
                }
            }

            //error_single_byte_chars ++;
            nleft --;
            p ++;
        }

        return single_byte_chars +
               two_byte_chars*2 +
               three_byte_chars*3 +
               four_byte_chars*4 == bytes.length;
    }

    public static boolean isAscii(byte[] bytes) {
        for (int i=0; i<bytes.length; ++i) {
            if ((bytes[i] & 0x80) != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean is_u8c(byte b) {
        // 是否以10打头
        return (b & 0xc0) == 0x80;
    }

    public static String expandSymbol(String symbol) {
        String value = PropertiesEx.GLOBAL.getProperty(symbol);
        if (value == null) {
            value = System.getProperty(symbol);
        }
        if (value == null) {
            value = System.getenv(symbol);
        }
        return value;
    }

    public static int getpid() {
        try {
            // pid@hostname
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int pos = name.indexOf('@');
            if (pos > 0) {
                String pid = name.substring(0, pos);
                return Integer.parseInt(pid);
            }
        }
        catch (Throwable t) {
            // nothing
        }

        return 9999;
    }

    public static String formatDateTime(long millisec) {
        return formatDateTime(millisec, "%d/%02d/%02d %02d:%02d:%02d");
    }

    public static String formatDateTime(long millisec, String formatPattern) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisec);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        return String.format(formatPattern, year, month, day, hour, minute, sec);
    }

    public static String formatTimeSpan(long uptimeMillis) {
        long millisPerDay = 24*3600*1000L;
        long millisPerHour = 3600*1000L;
        long millisPerMinute = 60*1000L;

        long remain = uptimeMillis;
        long day = remain / millisPerDay;
        remain -= (day * millisPerDay);

        long hour = remain / millisPerHour;
        remain -= (hour * millisPerHour);

        long minute = remain / millisPerMinute;
        remain -= (minute * millisPerMinute);

        long second = remain / 1000;

        if (day > 0) {
            return String.format("%d天%d小时%d分%d秒", day, hour, minute, second);
        }
        if (hour > 0) {
            return String.format("%d小时%d分%d秒", hour, minute, second);
        }
        if (minute > 0) {
            return String.format("%d分%d秒", minute, second);
        }
        return String.format("%d秒", second);
    }

    public static byte[] readBlob(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available() + 1024);
        byte[] chunk = new byte[4096];
        int nread = 0;
        for (;;) {
            int n = inputStream.read(chunk);
            if (n <= 0) {
                break;
            }
            bos.write(chunk, 0, n);
            nread += n;
        }
        if (nread > 0) {
            return bos.toByteArray();
        }
        return null;
    }

    private static ThreadFactory _tf = null;

    public static ThreadFactory getThreadFactory() {
        synchronized (MiscUtils.class) {
            if (_tf == null) {
                _tf = new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        return t;
                    }
                };
            }
            return _tf;
        }
    }

    public static String formatSize(long fileSize) {
        final long ONE_KB = 1024;
        final long ONE_MB = 1024 * ONE_KB;
        final long ONE_GB = 1024 * ONE_MB;
        String size;
        if (fileSize >= ONE_GB) {
            double f = ((double)fileSize) / (ONE_GB * 1.0);
            size = String.format("%.2f(GB)", f);
        } else if (fileSize >= ONE_MB) {
            double f = ((double)fileSize) / (ONE_MB * 1.0);
            size = String.format("%.2f(MB)", f);
        } else if (fileSize >= ONE_KB) {
            double f = ((double)fileSize) / (ONE_KB * 1.0);
            size = String.format("%.2f(KB)", f);
        } else {
            size = String.format("%d(Bytes)", fileSize);
        }
        return size;
    }

    // 后缀是 KB、MB、GB或TB
    // 返回字节为单位的大小值
    public static long parseSize(String str) {
        if (str == null) {
            return 0;
        }

        str = str.toLowerCase();
        double ratio = 1;
        if (str.endsWith("kb") || str.endsWith("k")) {
            ratio = 1024;
        }
        else if (str.endsWith("mb") || str.endsWith("m")) {
            ratio = 1024 * 1024L;
        }
        else if (str.endsWith("gb") || str.endsWith("g")) {
            ratio = 1024 * 1024 * 1024L;
        }
        else if (str.endsWith("tb") || str.endsWith("t")) {
            ratio = 1024 * 1024 * 1024 * 1024L;
        }

        int pos = 0;
        int len = str.length();
        while (pos < len) {
            char c = str.charAt(pos);
            if (c == '.' || (c >= '0' && c <= '9')) {
                pos ++;
            } else {
                break;
            }
        }
        if (pos == 0) {
            return 0;
        }

        String s = str.substring(0, pos);
        double v = Double.parseDouble(s) * ratio;
        return (long)v;
    }

    /**
     * 将形如YYYYMMDDhhmmss的时间字符串解析成日历对象
     * @param str 时间字符串,如2016,20161223112005
     * @return 若解析成功返回日历对象，否则返回null.
     */
    public static Calendar parseYYYYMMDDhhmmss(String str) {
        if (str == null) {
            return null;
        }

        byte[] s = str.getBytes();
        int len = s.length;
        for (int i = 0; i < len; ++ i) {
            if (!(s[i] >= '0' && s[i] <= '9')) {
                return null;
            }
        }

        int YYYY=0, MM=0, DD=0, hh=0, mm=0, ss=0;

        switch (len) {
            // 到年: 2016
            case 4: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = 1;
                DD = 1;
            }
            break;

            // 到月: 201612
            case 6: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = (s[4] - '0') * 10 + (s[5] - '0');
                DD = 1;
            }
            break;

            // 到日: 20161231
            case 8: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = (s[4] - '0') * 10 + (s[5] - '0');
                DD = (s[6] - '0') * 10 + (s[7] - '0');
            }
            break;

            // 到时: 2016123113
            case 10: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = (s[4] - '0') * 10 + (s[5] - '0');
                DD = (s[6] - '0') * 10 + (s[7] - '0');
                hh = (s[8] - '0') * 10 + (s[9] - '0');
            }
            break;

            // 到分: 201612311325
            case 12: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = (s[4] - '0') * 10 + (s[5] - '0');
                DD = (s[6] - '0') * 10 + (s[7] - '0');
                hh = (s[8] - '0') * 10 + (s[9] - '0');
                mm = (s[10] - '0') * 10 + (s[11] - '0');
            }
            break;

            // 到秒: 20161231132526
            case 14: {
                YYYY = (s[0] - '0') * 1000 + (s[1] - '0') * 100 + (s[2] - '0') * 10 + (s[3] - '0');
                MM = (s[4] - '0') * 10 + (s[5] - '0');
                DD = (s[6] - '0') * 10 + (s[7] - '0');
                hh = (s[8] - '0') * 10 + (s[9] - '0');
                mm = (s[10] - '0') * 10 + (s[11] - '0');
                ss = (s[12] - '0') * 10 + (s[13] - '0');
            }
            break;
        }
        if (YYYY < 1900 || YYYY > 3000) {
            return null;
        }
        if (MM == 0) {
            MM = 1;
        }
        if (DD == 0) {
            DD = 1;
        }

        Calendar c = Calendar.getInstance();
        c.set(YYYY, MM-1, DD, hh, mm, ss);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static boolean isNullString(String v) {
        return v == null || v.isEmpty();
    }

    public static String generateFtpUserName(final String mp) {
        if (mp.startsWith("/")) {
            return "filesgetuser";
        } else {
            // C:\ --> user_a
            String name = "user_" + mp.substring(0, 1);
            return name.toLowerCase();
        }
    }

    public static String generateFtpUserKey(String userName) {
        return userName + "12345678";
    }

    public static PropertiesEx loadPropertiesFile(String fileName) throws Exception {
        return MiscUtils.loadPropertiesFile(fileName, Charset.defaultCharset().displayName());
    }

    public static PropertiesEx loadPropertiesFile(String fileName, String charsetName) throws Exception {
        PropertiesEx props = new PropertiesEx();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(fis, charsetName);
            props.load(isr);
            fis.close();
        } finally {
            StreamUtils.close(fis);
        }
        return props;
    }

    public static JsonNode doHttpGetForJson(OkHttpClient httpClient, String endpoint) throws Exception {
        ByteArray ba = doHttpGet(httpClient, endpoint);
        ByteArrayInputStream is = new ByteArrayInputStream(ba.array, ba.offset, ba.length);
        return JsonNode.parseJsonDoc(is, "gbk");
    }

    public static ByteArray doHttpGet(OkHttpClient httpClient, String endpoint) throws Exception {
        Request request = new Request.Builder()
                .url(endpoint)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String emsg = String.format("rest调用失败: endpoint=%s, httpcode=%d, httpmsg=%s",
                        endpoint, code, response.message());
                throw new StatusCodeException(code, emsg);
            }
            InputStream is = response.body().byteStream();
            return StreamUtils.readInputStream(is);
        }
    }

    public static ByteArray doHttpPost(OkHttpClient httpClient, String endpoint, ByteStream blobToPost) throws Exception {
        final MediaType BLOB = MediaType.parse("application/octet-stream");
        int len = blobToPost.length();
        RequestBody body = RequestBody.create(BLOB, blobToPost.array(), 0, len);
        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Length", String.valueOf(len))
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String emsg = String.format("rest调用失败: endpoint=%s, httpcode=%d, httpmsg=%s",
                        endpoint, code, response.message());
                throw new StatusCodeException(code, emsg);
            }
            InputStream is = response.body().byteStream();
            return StreamUtils.readInputStream(is);
        }
    }

    public static JsonNode doHttpPostForJson(OkHttpClient httpClient, String endpoint,
                                             String jsonText,
                                             HashMap<String, String> httpHeaders) throws Exception {
        final MediaType JSON = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(JSON, jsonText);
        Request.Builder builder = new Request.Builder()
                .url(endpoint)
                .post(body);
        if (httpHeaders != null) {
            for (Entry<String, String> e : httpHeaders.entrySet()) {
                builder.addHeader(e.getKey(), e.getValue());
            }
        }

        Request request = builder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String emsg = String.format("rest调用失败: endpoint=%s, httpcode=%d, httpmsg=%s",
                        endpoint, code, response.message());
                throw new StatusCodeException(code, emsg);
            }
            InputStream is = response.body().byteStream();
            return JsonNode.parseJsonDoc(is);
        }
    }

    public static void invokeMethod(String className, String methodName, Object arg) throws Exception {
        if (className == null) {
            throw new Exception("className不能为null");
        }
        if (className.indexOf('.') < 0) {
            // 这是一个符号名
            String symbolName = className;
            className = expandSymbol(symbolName);
            if (className == null) {
                throw new Exception("没找到系统属性: " + symbolName);
            }
        }

        // 调用方法
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, Object.class);
        Object obj = clazz.newInstance();
        method.invoke(obj, arg);
    }

    public static void parseAndCheckRestResult(ByteArray ba, FreezerJSON result) throws StatusCodeException {
        ByteArrayInputStream is = new ByteArrayInputStream(ba.array, ba.offset, ba.length);
        parseAndCheckRestResult(is, result);
    }

    public static void parseAndCheckRestResult(InputStream is, FreezerJSON result) throws StatusCodeException {
        JsonNode ack;
        try {
            ack = JsonNode.parseJsonDoc(is);
        } catch (Exception e) {
            throw new StatusCodeException(-2, e);
        }

        JsonNode codeNode = ack.get("retcode");
        if (codeNode == null) {
            throw new StatusCodeException(-1, "rest应答格式错误");
        }

        int ncode = (int)codeNode.asInt64(); //Integer.parseInt(codeNode.asString());
        if (ncode != 0) {
            JsonNode msgNode = ack.get("retmsg");
            String msg = msgNode != null ? msgNode.asString() : "原因未知";
            throw new StatusCodeException(ncode, msg);
        }

        if (result != null) {
            try {
                result.defreezeFromJSON(ack.get("retdata"));
            } catch (DefreezeException e) {
                throw new StatusCodeException(-2, e);
            }
        }
    }
}

