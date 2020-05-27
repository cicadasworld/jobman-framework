package gtcloud.common.web;

import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.basetypes.ResourceAttributes;
import gtcloud.common.utils.StreamUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import platon.FreezerJSON;
import platon.JsonNode;

public class SimpleUtils {

    /**
     * �²��������ʹ�ú��ֱ��뷽ʽ�������ı����б��롣
     *
     * @param req http����
     * @param defaultEnc Ĭ�ϱ��롣
     *
     * @return �����������ʹ�õ��ַ����롣
     */
    public static String getRequestEncoding(HttpServletRequest req, String defaultEnc) {
        return getBrowserEncoding(req, "reqenc", defaultEnc);
    }

    /**
     * �²��������������������ʹ�ú��ֱ��뷽ʽ��Ӧ���ı����б��롣
     *
     * @param req http����
     * @param defaultEnc Ĭ�ϱ��롣
     *
     * @return ��������������ַ����롣
     */
    public static String getResponseEncoding(HttpServletRequest req, String defaultEnc) {
        String encode = req.getParameter("encode");
        if (encode == null) {
            encode = req.getParameter("encoding");
        }
        if (encode == null) {
            encode = defaultEnc;
        }
        if (encode == null) {
            encode = "gbk";
        }
        return encode;
    }

    public static String getResponseEncoding(HttpServletRequest req) {
        return getResponseEncoding(req, null);
    }

    /**
     * �²�������ı����ַ�����
     *
     * @param req ����http����
     * @param encParamName �����и����ַ�����Ĳ������֣�
     * @param defaultEnc Ĭ�ϱ��롣
     *
     * @return ������ı����ַ�����
     */
    private static String getBrowserEncoding(HttpServletRequest req,
                                            String encParamName,
                                            String defaultEnc) {
        String enc = req.getParameter(encParamName);
        if (enc != null && enc.length() > 0) {
            return enc;
        }

        // ���������ȷָ�����ַ������򷵻�֮
        String charset = req.getCharacterEncoding();
        if (charset != null && charset.length() > 0) {
            return charset;
        }

        return defaultEnc != null ? defaultEnc : "gbk";
    }

    /**
     * �������POST�ϵ�����
     * @param req
     * @return
     * @throws IOException
     */
    public static ByteArray readPostBody(HttpServletRequest req) throws IOException {
        InputStream fis = req.getInputStream();
        return StreamUtils.readInputStream(fis);
    }

    public static String readPostBodyAsString(HttpServletRequest req, String encoding) throws IOException {
        ByteArray ba = readPostBody(req);
        return new String(ba.array, ba.offset, ba.length, encoding);
    }

    /**
     * ���ͻ���POST���������ݽ�����Json�ڵ㡣
     * @param request http����
     * @return json�ڵ�
     * @throws Exception
     */
    public static JsonNode readJsonFromPostBody(HttpServletRequest request) throws Exception {
        String reqenc = request.getParameter("reqenc");
        JsonNode jsonNode = null;
        if ("uriencodedjson".equals(reqenc)) {
            ByteArray ba = readPostBody(request);
            String s = new String(ba.array, ba.offset, ba.length);

            if (s.indexOf('%') >= 0) {
                s = URLDecoder.decode(s, "utf-8");
            }
            jsonNode = JsonNode.parseJsonDoc(s);
        }
        else {
            String charset = reqenc != null ? reqenc : request.getCharacterEncoding();
            if (charset != null && charset.length() > 0) {
                jsonNode = JsonNode.parseJsonDoc(request.getInputStream(), charset);
            } else {
                jsonNode = JsonNode.parseJsonDoc(request.getInputStream());
            }
        }

        return jsonNode;
    }

    /**
     * ���ַ�����ΪӦ�𷢸��������
     *
     * @param req  ���ε�http�������
     * @param resp ���ε�httpӦ�����
     * @param respText ��Ӧ�ı���
     * @param encoding �������Ҫ���ַ����룻
     * @param mimeType MIME���ͣ���Ϊnull��Ĭ��Ϊ"text/plain".
     *
     * @throws IOException
     */
    public static void sendStringAsResponse(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String respText,
                                            String encoding,
                                            String mimeType) throws IOException {

        if (respText == null || respText.length() == 0) {
            return;
        }

        if (mimeType == null) {
            mimeType = "text/plain";
        }

        if (encoding == null) {
            encoding = getResponseEncoding(req);
        }

        resp.setContentType(String.format("%s; charset=%s", mimeType, encoding));

        OutputStream os = resp.getOutputStream();
        byte[] b = respText.getBytes(encoding);

        if (b.length > 10 && isGzipSupported(req)) {
            resp.setHeader("Content-Encoding", "gzip");
            GZIPOutputStream gzip_os = new GZIPOutputStream(os);
            gzip_os.write(b);
            gzip_os.close();
        }
        else {
            resp.setContentLength(b.length);
            os.write(b);
        }
    }

    /**
     * ��blob��ΪӦ�𷢸��������
     *
     * @param req  ���ε�http�������
     * @param resp ���ε�httpӦ�����
     * @param blob Ӧ�����ݣ�
     * @param offset Ӧ��������������ƫ��λ�ã�
     * @param len Ӧ�����ݵ��ֽڳ��ȣ�
     * @param mimeType MIME���ͣ���Ϊnull��Ĭ��Ϊ"application/octet-stream".
     *
     * @throws IOException
     */
    public static void sendBlobAsResponse(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          byte[] blob, int offset, int len,
                                          String mimeType) throws IOException {
        if (blob == null || len <= 0) {
            return;
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        resp.setContentType(mimeType);
        resp.setContentLength(len);
        OutputStream os = resp.getOutputStream();
        os.write(blob, offset, len);
        os.flush();
    }

    public static void sendHtmlAsResponse(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          ByteArrayOutputStream os) throws IOException {
        byte[] data = os.toByteArray();
        String contentType = "text/html; charset=" + Charset.defaultCharset().name();
        sendBlobAsResponse(req, resp, data, 0, data.length, contentType);
    }

    public static void sendJsonAsResponse(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          FreezerJSON nodes) throws Exception {
        boolean isDebugging = false;
        String debug = req.getParameter("debug");
        if (debug != null) {
            isDebugging = debug.equals("1") || debug.equalsIgnoreCase("true");
        }

        // ����������𻺴������õ���Ӧ
        sugguectBrowserNotCache(resp);

        final String jsonText = isDebugging ?
                                nodes.freezeToJSON().toPrettyString(4, 0) :
                                nodes.freezeToJSON().toCompactString();
        sendStringAsResponse(req, resp, jsonText, null, "application/json");
    }

    public static boolean isGzipSupported(HttpServletRequest req) {
        String encodings = req.getHeader("Accept-Encoding");
        return encodings != null && encodings.indexOf("gzip") >= 0;
    }


    public static void sugguectBrowserNotCache(HttpServletResponse resp) {
        final long t0 = System.currentTimeMillis();
        final long maxAgeMillis = 0;
        resp.setDateHeader("Date", t0);
        resp.setDateHeader("Expires", t0 + maxAgeMillis);
        resp.setHeader("Cache-Control", "public, max-age="
                + (maxAgeMillis / 1000));
    }

    /**
     * ����HTTP��Ӧ�뻺�������ص�header��
     *
     * @param resp
     * @param attrib
     * @param maxAgeMillis
     */
    public static void setCacheControlHeaders(HttpServletResponse resp, ResourceAttributes attrib, long maxAgeMillis) {
        final long t0 = System.currentTimeMillis();
        if (attrib != null) {
            resp.setHeader("ETag", attrib.getETag());
            resp.setDateHeader("Last-Modified", attrib.getLastModified());
        }
        resp.setDateHeader("Date", t0);
        resp.setDateHeader("Expires", t0 + maxAgeMillis);
        resp.setHeader("Cache-Control", "public, max-age=" + (maxAgeMillis/1000));
    }

    public static String escapeHttpURL(String httpUrl) {
        boolean needEscape = false;
        if (true) {
            byte[] bytes = httpUrl.getBytes();
            for (int i = 0; i < bytes.length; ++ i) {
                int n = bytes[i];
                if (n < 0) {
                    needEscape = true;
                }
            }
        }
        if (!needEscape) {
            return httpUrl;
        }

        final byte[] hexTable = new byte[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        try {
            byte[] utf8_bytes = httpUrl.getBytes("utf-8");
            byte[] buf = new byte[utf8_bytes.length * 3];
            int c = 0;
            for (int i = 0; i < utf8_bytes.length; ++ i) {
                int n = utf8_bytes[i];
                if (n >= 0) {
                    buf[c++] = (byte)n;
                }
                else {
                    //%E5%85%A8%E7%90%83%E7%9F%A2%E9%87%8F
                    int hi = (n >> 4) & 0x0f;
                    int lo = (n >> 0) & 0x0f;
                    buf[c++] = (byte)'%';
                    buf[c++] = (byte)hexTable[hi];
                    buf[c++] = (byte)hexTable[lo];
                }
            }
            httpUrl = new String(buf, 0, c);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return httpUrl;
    }

    // Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0
    private static final byte[] _CAP_BYTES = new byte[] {
        (byte)0x4d, (byte)0x6f, (byte)0x7a, (byte)0x69, (byte)0x6c, (byte)0x6c, (byte)0x61, (byte)0x2f,
        (byte)0x35, (byte)0x2e, (byte)0x30, (byte)0x20, (byte)0x28, (byte)0x57, (byte)0x69, (byte)0x6e,
        (byte)0x64, (byte)0x6f, (byte)0x77, (byte)0x73, (byte)0x20, (byte)0x4e, (byte)0x54, (byte)0x20,
        (byte)0x36, (byte)0x2e, (byte)0x31, (byte)0x3b, (byte)0x20, (byte)0x57, (byte)0x4f, (byte)0x57,
        (byte)0x36, (byte)0x34, (byte)0x3b, (byte)0x20, (byte)0x72, (byte)0x76, (byte)0x3a, (byte)0x31,
        (byte)0x39, (byte)0x2e, (byte)0x30, (byte)0x29, (byte)0x20, (byte)0x47, (byte)0x65, (byte)0x63,
        (byte)0x6b, (byte)0x6f, (byte)0x2f, (byte)0x32, (byte)0x30, (byte)0x31, (byte)0x30, (byte)0x30,
        (byte)0x31, (byte)0x30, (byte)0x31, (byte)0x20, (byte)0x46, (byte)0x69, (byte)0x72, (byte)0x65,
        (byte)0x66, (byte)0x6f, (byte)0x78, (byte)0x2f, (byte)0x31, (byte)0x39, (byte)0x2e, (byte)0x30
    };
    private static final String _CAP_VALUE = new String(_CAP_BYTES);

    // User-Agent
    private static final String _CAP_NAME = new String (new byte[] {
        (byte)0x55, (byte)0x73, (byte)0x65, (byte)0x72, (byte)0x2d, (byte)0x41, (byte)0x67, (byte)0x65,
        (byte)0x6e, (byte)0x74
    });

}
