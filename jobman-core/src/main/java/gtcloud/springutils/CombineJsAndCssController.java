package gtcloud.springutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import gtcloud.common.web.SimpleUtils;

@Controller
@ConditionalOnProperty(name="GTCLOUD_PRODUCT_MODE", havingValue="0", matchIfMissing=true)
public class CombineJsAndCssController {

    @Autowired(required = true)
    private ResourceProperties resourceProperties;

    @Autowired(required = true)
    private ResourceLoader resourceLoader;

    private final ConcurrentHashMap<String, String> moduleToBigJs = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> moduleToBigCss = new ConcurrentHashMap<>();

    @GetMapping(value = "/jsapi/{moduleId}.js")
    public void combineJs1(@PathVariable String moduleId,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        String fullModuleId = moduleId;
        doCombileJs(fullModuleId, request, response);
    }

    @GetMapping(value = "/jsapi/{ns1}/{moduleId}.js")
    public void combineJs2(@PathVariable String ns1,
                           @PathVariable String moduleId,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        String fullModuleId = ns1 +"/" + moduleId;
        doCombileJs(fullModuleId, request, response);
    }

    @GetMapping(value = "/jsapi/{ns1}/{ns2}/{moduleId}.js")
    public void combineJs3(@PathVariable String ns1,
                           @PathVariable String ns2,
                           @PathVariable String moduleId,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        String fullModuleId = ns1 +"/" + ns2 + "/" + moduleId;
        doCombileJs(fullModuleId, request, response);
    }

    private void doCombileJs(String fullModuleId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 从缓存中找
        String cached = this.moduleToBigJs.get(fullModuleId);
        String jsText = cached;

        // 看是否存在 "jsapi/{moduleId}.js"这个资源
        if (jsText == null) {
            String[] resourceLocation = {null};
            String relativePath = String.format("jsapi/%s.js", fullModuleId);
            jsText = loadTextResourceByRelativePath(relativePath, resourceLocation);
        }

        // 读列表文件"{moduleId}/jsapi/list.js"
        if (jsText == null) {
            String relativePath = String.format("%s/jsapi/list.js", fullModuleId);
            String[] resourceLocation = {null};
            String jsFileList = loadTextResourceByRelativePath(relativePath, resourceLocation);
            if (jsFileList != null) {
                String pivotLocation = resourceLocation[0] + String.format("%s/jsapi/", fullModuleId);
                jsText = combineTextFiles(jsFileList, pivotLocation);
            }
        }

        if (jsText == null) {
            String msg = String.format("resource not found: /jsapi/%s.js", fullModuleId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }

        // 放入缓存
        if (cached == null) {
            this.moduleToBigJs.putIfAbsent(fullModuleId, jsText);
        }

        final String encoding = "gbk";
        final String mimeType = "text/javascript";
        SimpleUtils.sendStringAsResponse(request, response, jsText, encoding, mimeType);
    }

    @GetMapping(value = "/{moduleId}/css/single.css")
    public void combineCss1(@PathVariable String moduleId,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        String fullModuleId = moduleId;
        doCombileCss(fullModuleId, request, response);
    }

    @GetMapping(value = "/{ns1}/{moduleId}/css/single.css")
    public void combineCss2(@PathVariable String ns1,
                            @PathVariable String moduleId,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        String fullModuleId = ns1 + "/" + moduleId;
        doCombileCss(fullModuleId, request, response);
    }

    @GetMapping(value = "/{ns1}/{ns2}/{moduleId}/css/single.css")
    public void combineCss3(@PathVariable String ns1,
                            @PathVariable String ns2,
                            @PathVariable String moduleId,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        String fullModuleId = ns1 + "/" + ns2 + "/" + moduleId;
        doCombileCss(fullModuleId, request, response);
    }

    private void doCombileCss(String fullModuleId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 从缓存中找
        String cached = this.moduleToBigCss.get(fullModuleId);
        String cssText = cached;

        // 读列表文件"{moduleId}/css/list.txt"
        if (cssText == null) {
            String relativePath = String.format("%s/css/list.txt", fullModuleId);
            String[] resourceLocation = {null};
            String cssFileList = loadTextResourceByRelativePath(relativePath, resourceLocation);
            if (cssFileList != null) {
                String pivotLocation = resourceLocation[0] + String.format("%s/css/", fullModuleId);
                cssText = combineTextFiles(cssFileList, pivotLocation);
            }
        }

        if (cssText == null) {
            String msg = String.format("resource not found: /%s/css/single.css", fullModuleId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }

        // 放入缓存
        if (cached == null) {
            this.moduleToBigCss.putIfAbsent(fullModuleId, cssText);
        }

        final String encoding = "gbk";
        final String mimeType = "text/css";
        SimpleUtils.sendStringAsResponse(request, response, cssText, encoding, mimeType);
    }

    /**
     * 加载.js、.css等文本资源.
     *
     * @param relativePath 相对于 classpath的 路径，如"/jsapi/list.js"
     * @param outLocation 返回资源所在的位置
     * @return 若资源存在，返回资源文本，否则返回null.
     * @throws IOException
     */
    private String loadTextResourceByRelativePath(String relativePath, String[] outLocation) throws IOException {
        for (String location : this.resourceProperties.getStaticLocations()) {
            // location 形如: "classpath:/static/"
            String text = loadTextResourceByFullPath(location + relativePath);
            if (text != null) {
                outLocation[0] = location;
                return text;
            }
        }
        return null;
    }

    /**
     * 加载.js、.css等文本资源.
     *
     * @param fullPath  classpath的 路径，如"classpath:/static/commons/jsapi/list.js"
     * @return 若资源存在，返回资源文本，否则返回null.
     * @throws IOException
     */
    private String loadTextResourceByFullPath(String fullPath) throws IOException {
        // fullPath 形如: "classpath:/static/commons/jsapi/list.js"
        Resource resource = this.resourceLoader.getResource(fullPath);
        if (resource == null || !resource.exists()) {
            return null;
        }
        InputStream in = resource.getInputStream();
        if (in == null) {
            return null;
        }
        String text = StreamUtils.copyToString(in, Charset.defaultCharset());
        in.close();
        return text;
    }

    private String combineTextFiles(String textFileList, String pivotLocation) throws IOException {
        StringBuilder sb = new StringBuilder(2048);

        StringReader sr = new StringReader(textFileList);
        LineNumberReader reader = new LineNumberReader(sr);
        String line = null;
        while (null != (line = reader.readLine())) {
            String fileName = line.trim();
            if (fileName.length() == 0 || fileName.startsWith("#") || fileName.startsWith("//")) {
                continue;
            }
            String fullPath = pivotLocation + fileName;
            String jsLines = loadTextResourceByFullPath(fullPath);
            if (jsLines != null) {
                sb.append("/*" + fileName + "*/\n");
                sb.append(jsLines);
            }
        }
        reader.close();

        return sb.toString();
    }

}
