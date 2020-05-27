package gtcloud.springutils;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import gtcloud.common.web.RestResult;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class SimpleErrorController implements ErrorController{

    @Value("${server.error.path:${error.path:/error}}")
    private final String path = "/error";

    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes(true);

    @Override
    public String getErrorPath() {
        return this.path;
    }

    @RequestMapping(produces = {
        "application/xml",
        "text/xml",
        "application/json",
        "application/*+xml",
        "application/*+json"
    })
    @ResponseBody
    public RestResult errorStructured(HttpServletRequest request,
                                      HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        response.setStatus(status.value());

        Map<String, Object> body = getErrorAttributes(request);
        Integer code = (Integer)body.get("status");
        String err = (String)body.get("error");
        String msg = (String)body.get("message");
        if (err != null) {
            msg = (msg == null) ? err : (err + ": " + msg);
        }
        String url = (String)body.get("path");

        return RestResult.error(code.intValue(), msg, url, request);
    }

    @RequestMapping(produces = "text/html")
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        response.setStatus(status.value());
        Map<String, Object> model = Collections.unmodifiableMap(getErrorAttributes(request));
        return new ModelAndView("error", model);
    }

    private Map<String, Object> getErrorAttributes(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        boolean includeStackTrace = false;
        return this.errorAttributes.getErrorAttributes(webRequest, includeStackTrace);
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        }
        catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

}
