package gtcloud.springutils;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;

import gtcloud.common.web.RestResult;
import platon.FreezerJSON;

public class RestResultHttpMessageConverter extends AbstractHttpMessageConverter<RestResult> {

    private static final String LN = String.format("%n");

    @Override
    protected boolean canWrite(@Nullable MediaType mediaType) {
        return true;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RestResult.class.equals(clazz);
    }

    @Override
    protected RestResult readInternal(Class<? extends RestResult> clazz,
                                      HttpInputMessage input)
                                      throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(RestResult result,
                                 HttpOutputMessage output)
                                 throws IOException, HttpMessageNotWritableException {

        String encoding = result.getOption("encode");
        if (encoding == null) {
            encoding = result.getOption("encoding");
        }
        if (encoding == null) {
            encoding = "gbk";
        }

        boolean prettyJson = false;
        String debug = result.getOption("debug");
        if (debug != null) {
            prettyJson = debug.equals("1") || debug.equalsIgnoreCase("true");
        }

        StringBuilder sb = new StringBuilder();
        if (!result.isOK()) {
            // 错误应答
            formatErrorResult(result, sb, prettyJson);
        }
        else {
            // 成功应答
            formatSuccessResult(result, sb, prettyJson);
        }
        byte[] jsonBytes = sb.toString().getBytes(encoding);

        HttpHeaders h = output.getHeaders();
        h.add("Content-Type", String.format("application/json; charset=%s", encoding));
        h.setContentLength(jsonBytes.length);

        output.getBody().write(jsonBytes);
    }

    private static void formatErrorResult(RestResult result, StringBuilder sb, boolean prettyJson) {
        String msg = result.getMessage();
        String url = result.getUrl();
        appendOneLine(sb, "{", prettyJson);
        appendOneLine(sb, String.format("  \"retcode\": %d,", result.getCode()), prettyJson);
        appendOneLine(sb, String.format("  \"retmsg\": \"%s\",", msg != null ? msg : "null"), prettyJson);
        appendOneLine(sb, String.format("  \"url\": \"%s\"", url != null ? url : "null"), prettyJson);
        appendOneLine(sb, "}", prettyJson);
    }

    private static void formatSuccessResult(RestResult result, StringBuilder sb, boolean prettyJson) throws IOException {
        appendOneLine(sb, "{", prettyJson);
        appendOneLine(sb, "  \"retcode\": 0,", prettyJson);
        appendOneLine(sb, "  \"retmsg\": \"ok\",", prettyJson);
        String s = resultDataToJsonString(result, prettyJson);
        sb.append("  \"retdata\": ");
        appendOneLine(sb, s, prettyJson);
        appendOneLine(sb, "}", prettyJson);
    }

    private static void appendOneLine(StringBuilder sb, String line, boolean prettyJson) {
        sb.append(line);
        if (prettyJson) {
            sb.append(LN);
        }
    }

    private static String resultDataToJsonString(RestResult result, boolean prettyJson) throws IOException {
        FreezerJSON node = result.getData();
        if (node == null) {
            return "null";
        }
        try {
            return prettyJson ?
                node.freezeToJSON().toPrettyString(4, 0) :
                node.freezeToJSON().toCompactString();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
