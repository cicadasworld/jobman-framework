package gtcloud.common.web;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import gtcloud.common.basetypes.StatusCodeException;
import platon.FreezerJSON;

/**
 * �������ڰ�װREST������ú���Ľ����ʹ�÷��ظ������ߵ�JSON����ͳһ����ʽ��
 *
 *<pre>
 * (1) �ɹ�Ӧ��
 * {
 *     retcode: 0,
 *     retmsg: "ok",
 *     retdata: ���������ݶ���
 * }
 *
 * (2) ����Ӧ��
 * {
 *     retcode: �������,
 *     retmsg: ��������,
 *     url: ������ʹ�õ�URL
 * }
 *</pre>
 */
public class RestResult {

    public static final int OK = 0;

    private int code = OK;

    private String message = "ok";

    private FreezerJSON data = null;

    private String url = null;

    private static final RestResult OK_RESULT = new RestResult();

    private final HashMap<String, String> options = new HashMap<>();

    private RestResult() {
        ;
    }

    /**
     * ����һ���ɹ�Ӧ��������"����ֵ��void"�����Ρ�
     * @return
     */
    public static RestResult ok() {
        return OK_RESULT;
    }

    /**
     * ����һ���ɹ�Ӧ��������"����ֵ��ĳ�����ݶ���"�����Ρ�
     * @param data rest������õĽ�����ݶ���
     * @return
     */
    public static RestResult ok(FreezerJSON data) {
        RestResult r = new RestResult();
        r.data = data;
        return r;
    }

    public static RestResult ok(FreezerJSON data, HttpServletRequest request) {
        RestResult r = ok(data);
        extractExtraOptions(request, r);
        return r;
    }

    /**
     * ����һ������Ӧ��
     * @param errorCode
     * @param errorMessage
     * @return
     */
    public static RestResult error(int errorCode, String errorMessage) {
        RestResult r = new RestResult();
        r.code = errorCode;
        r.message = errorMessage;
        return r;
    }

    public static RestResult error(int errorCode, String errorMessage, HttpServletRequest request) {
        RestResult r = new RestResult();
        r.code = errorCode;
        r.message = errorMessage;
        extractExtraOptions(request, r);
        return r;
    }

    public static RestResult error(Exception ex) {
        if (ex instanceof StatusCodeException) {
            StatusCodeException e = (StatusCodeException)ex;
            return error(e.getCode(), ex.getMessage());
        } else {
            return error(-1, ex.getMessage());
        }
    }

    public static RestResult error(Exception ex, HttpServletRequest request) {
        if (ex instanceof StatusCodeException) {
            StatusCodeException e = (StatusCodeException)ex;
            return error(e.getCode(), e.getMessage(), request);
        } else {
            return error(-1, ex.getMessage(), request);
        }
    }

    /**
     * ����һ������Ӧ��
     * @param errorCode
     * @param errorMessage
     * @param url
     * @return
     */
    public static RestResult error(int errorCode, String errorMessage, String url) {
        RestResult r = error(errorCode, errorMessage);
        r.url = url;
        return r;
    }

    public static RestResult error(Exception ex, String url) {
        if (ex instanceof StatusCodeException) {
            StatusCodeException e = (StatusCodeException)ex;
            return error(e.getCode(), ex.getMessage(), url);
        } else {
            return error(-1, ex.getMessage(), url);
        }
    }

    public static RestResult error(int errorCode, String errorMessage, String url, HttpServletRequest request) {
        RestResult r = error(errorCode, errorMessage, url);
        extractExtraOptions(request, r);
        return r;
    }

    public static RestResult error(StatusCodeException ex, String url, HttpServletRequest request) {
        if (ex instanceof StatusCodeException) {
            StatusCodeException e = (StatusCodeException)ex;
            return error(e.getCode(), ex.getMessage(), url, request);
        } else {
            return error(-1, ex.getMessage(), url, request);
        }
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public FreezerJSON getData() {
        return data;
    }

    public String getUrl() {
        return url;
    }

    public boolean isOK() {
        return code == OK;
    }

    public RestResult setOption(String name, String value) {
        this.options.put(name, value);
        return this;
    }

    public String getOption(String name) {
        return this.options.get(name);
    }

    public boolean hasOption(String name) {
        return this.options.containsKey(name);
    }

    private static void extractExtraOptions(HttpServletRequest request, RestResult r) {
        if (request != null) {
            String debug = request.getParameter("debug");
            if (debug != null) {
                r.setOption("debug", debug);
            }
            String encoding = request.getParameter("encoding");
            if (encoding == null) {
                encoding = request.getParameter("encode");
            }
            if (encoding != null) {
                r.setOption("encoding", encoding);
            }
        }
    }

}
