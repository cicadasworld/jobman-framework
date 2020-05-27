package gtcloud.jobman.proc;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import gtcloud.common.SharedOkHttpClient;
import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;
import gtcloud.jobman.core.processor.main.LocalProccessNodeHelper;
import gtcloud.springutils.SpringEnvUtils;
import okhttp3.OkHttpClient;
import platon.ByteStream;
import platon.Message;

public class LocalProccessNodeHelperImpl implements LocalProccessNodeHelper {

    private static Logger LOG = LoggerFactory.getLogger(LocalProccessNodeHelperImpl.class);

    private final Environment env;

    private final String schedulerBaseURL;

    private final String localServiceBaseURL;

    private final OkHttpClient httpClient;

    public LocalProccessNodeHelperImpl(Environment env) throws Exception {
        this.env = env;

        this.httpClient = SharedOkHttpClient.get().newBuilder()
                .connectTimeout(6*1000L, TimeUnit.MILLISECONDS)
                .readTimeout(30*1000L, TimeUnit.MILLISECONDS)
                .build();

        String name = "gtcloud.jobman.scheduler.baseURL";
        String url = this.env.getProperty(name);
        if (url != null && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url == null || url.isEmpty()) {
            String emsg = String.format("必需的配置参数缺失：%s", name);
            LOG.error(emsg);
            throw new Exception(emsg);
        }
        this.schedulerBaseURL = url;

        this.localServiceBaseURL = figureOutLocalBaseURL(env);

        // 本地的filegetserver.exe的端口号
        SpringEnvUtils.injectLocalFileGetServerPort(env);
    }

    private static String figureOutLocalBaseURL(Environment env) throws Exception {
        String name = "local.server.port";
        String port = env.getProperty(name);
        if (port == null) {
            String emsg = String.format("从全局环境中找不到必需属性：%s", name);
            LOG.error(emsg);
            throw new Exception(emsg);
        }

        String baseURL = String.format("http://{host}:%s", port);
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath != null) {
            if (!contextPath.startsWith("/")) {
                baseURL += "/";
            }
            baseURL += contextPath;
        }
        return baseURL;
    }

    @Override
    public String getLocalServiceBaseURL() {
        return this.localServiceBaseURL;
    }

    @Override
    public String getProperty(String name, String defaultVal) {
        String val = this.env.getProperty(name);
        return val != null ? val : defaultVal;
    }

    @Override
    public void sendLogonReq(LogonReqDO req) throws Exception {
        sendMessageToScheduler(req, "/admin/node/logon");
    }

    @Override
    public void sendLogoffReq(LogoffDO req) throws Exception {
        sendMessageToScheduler(req, "/admin/node/logoff");
    }

    @Override
    public void sendHeartbeatReportReq(HeartbeatReportReqDO req) throws Exception {
        sendMessageToScheduler(req, "/admin/node/heartbeat");
    }

    @Override
    public void sendSubjobStatusReport(SubjobStatusReportDO snapshot) throws Exception {
        sendMessageToScheduler(snapshot, "/admin/subjob/status");
    }

    @Override
    public void sendSubjobDispatchAck(SubjobDispatchAckDO ack) throws Exception {
        sendMessageToScheduler(ack, "/admin/subjob/dispatch/ack");
    }

    private void sendMessageToScheduler(Message msg, String relativeEndpoint) throws Exception {
        ByteStream blob = new ByteStream();
        msg.freeze(blob);

        final String endpoint = this.schedulerBaseURL + relativeEndpoint;
        if (LOG.isDebugEnabled()) {
            LOG.debug("post('{}') begin...", endpoint);
        }
        MiscUtils.doHttpPost(httpClient, endpoint, blob);
        if (LOG.isDebugEnabled()) {
            LOG.debug("post('{}') done.", endpoint);
        }
    }

}
