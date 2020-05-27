package gtcloud.jobman.sched;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import gtcloud.common.SharedOkHttpClient;
import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.scheduler.main.JobSchedulerHelper;
import gtcloud.springutils.SpringEnvUtils;
import okhttp3.OkHttpClient;
import platon.ByteStream;

public class JobSchedulerHelperImpl implements JobSchedulerHelper {

    private static Logger LOG = LoggerFactory.getLogger(JobSchedulerHelperImpl.class);

    private final Environment env;

    private final OkHttpClient httpClient;
    
    public JobSchedulerHelperImpl(Environment env) {
        this.env = env;
        
        this.httpClient = SharedOkHttpClient.get().newBuilder()
                .connectTimeout(6*1000L, TimeUnit.MILLISECONDS)
                .readTimeout(30*1000L, TimeUnit.MILLISECONDS)
                .build();
        
        // 本地的filegetserver.exe的端口号
        SpringEnvUtils.injectLocalFileGetServerPort(env); 
    }

    @Override
    public String getProperty(String name, String defaultVal) {
        String val = this.env.getProperty(name);
        return val != null ? val : defaultVal;
    }

    @Override
    public void sendLogonAck(String nodeBaseURL, LogonAckDO ack) throws Exception {
        ByteStream blob = new ByteStream();
        ack.freeze(blob);    	
        sendBlobToProcessorNode(blob, nodeBaseURL, "/admin/node/logon/ack");
    }

    @Override
    public void sendHeartbeatAck(String nodeBaseURL, HeartbeatReportAckDO ack) throws Exception {
        ByteStream blob = new ByteStream();
        ack.freeze(blob);    	
        sendBlobToProcessorNode(blob, nodeBaseURL, "/admin/node/heartbeat/ack");
    }

    @Override
    public void sendSubjobReq(String nodeBaseURL, SubjobControlBlockDO subjobCB, byte[] body) throws Exception {
        ByteStream stream = new ByteStream();
        subjobCB.freeze(stream);
        stream.writeBlob(body, 0, body.length);
        sendBlobToProcessorNode(stream, nodeBaseURL, "/admin/subjob/dispatch");
    }

    /**
     * 发送消息给处理器节点。
     * @param blob
     * @param nodeBaseURL
     * @param uri
     * @throws Exception
     */
    private void sendBlobToProcessorNode(ByteStream blob, String nodeBaseURL, String uri) throws Exception {
        final String endpoint = nodeBaseURL + uri;
        if (LOG.isDebugEnabled()) {
            LOG.debug("post('{}') begin...", endpoint);
        }
        MiscUtils.doHttpPost(this.httpClient, endpoint, blob);
        if (LOG.isDebugEnabled()) {
            LOG.debug("post('{}') done.", endpoint);
        }
    }
}
