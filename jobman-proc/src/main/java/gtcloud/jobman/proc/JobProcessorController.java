package gtcloud.jobman.proc;

import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.web.SimpleUtils;
import gtcloud.jobman.core.pdo.HeartbeatReportAckDO;
import gtcloud.jobman.core.pdo.LogonAckDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import platon.ByteStream;

@RestController
public class JobProcessorController {

    private static Logger LOG = LoggerFactory.getLogger(JobProcessorController.class);

    /**
     * 处理调度器发送来登录响应。
     * @param request
     * @return
     */
    @PostMapping("/admin/node/logon/ack")
    public void handleLogonAck(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleLogonAck() called.");
        }
        if (LocalProccessNodeHolder.value == null ) {
            // 系统还未完全起来
            return;
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            LogonAckDO ack = new LogonAckDO();
            ack.defreeze(stream);
            LocalProccessNodeHolder.value.handleLogonAck(ack);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }

    /**
     * 处理调度器发送来心跳报告响应。
     * @param request
     * @return
     */
    @PostMapping("/admin/node/heartbeat/ack")
    public void handleHeartbeatAck(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleHeartbeatAck() called.");
        }
        if (LocalProccessNodeHolder.value == null ) {
            // 系统还未完全起来
            return;
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            HeartbeatReportAckDO ack = new HeartbeatReportAckDO();
            ack.defreeze(stream);
            LocalProccessNodeHolder.value.handleHeartbeatAck(ack);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }

    /**
     * 处理调度器分配来的子任务。
     * @param request
     * @return
     */
    @PostMapping("/admin/subjob/dispatch")
    public void handleSubjobReq(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleSubjobReq() called.");
        }
        if (LocalProccessNodeHolder.value == null ) {
            // 系统还未完全起来
            return;
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            SubjobControlBlockDO req = new SubjobControlBlockDO();
            req.defreeze(stream);
            byte[] body = stream.readBlob();
            LocalProccessNodeHolder.value.handleSubjobReq(req, body);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }
}
