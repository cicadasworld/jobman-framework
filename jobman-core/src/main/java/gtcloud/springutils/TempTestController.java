package gtcloud.springutils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gtcloud.common.utils.MiscUtils;
import gtcloud.common.web.SimpleUtils;
import platon.PropSet;

/**
 * 该类仅用于临时性测试。
 */
@RestController
//@ConditionalOnProperty(name="GTCLOUD_TEMP_TEST", havingValue="1", matchIfMissing=false)
public class TempTestController {

    private static final Logger LOG = LoggerFactory.getLogger(TempTestController.class);

    // for debug
    @GetMapping("/gmx/v1/temptest/{className}/{methodName}")
    @ResponseBody
    @CrossOrigin
    public void invokeTestMethod(HttpServletRequest request,
                                 HttpServletResponse response,
                                 @PathVariable String className,
                                 @PathVariable String methodName) throws Exception {
        // 获得测试类名
        PropSet ack = new PropSet();
        try {
            Object arg = null;
            MiscUtils.invokeMethod(className, methodName, arg);
            ack.put("code", "0");
            ack.put("message", "ok");
        } catch(Throwable ex) {
            LOG.error("invokeMethod() error", ex);
            ack.put("code", "-1");
            ack.put("message", ex + "");
        }
        SimpleUtils.sendJsonAsResponse(request, response, ack);
    }
}
