package gtcloud.jobman.sched;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.utils.MiscUtils;
import gtcloud.common.web.RestResult;
import gtcloud.common.web.SimpleUtils;
import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.HeartbeatReportReqDO;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.JobStatusDO;
import gtcloud.jobman.core.pdo.JobStatusListDO;
import gtcloud.jobman.core.pdo.LogoffDO;
import gtcloud.jobman.core.pdo.LogonReqDO;
import gtcloud.jobman.core.pdo.SchedulerStateSummaryDO;
import gtcloud.jobman.core.pdo.SubjobDispatchAckDO;
import gtcloud.jobman.core.pdo.SubjobDumpStateListDO;
import gtcloud.jobman.core.pdo.SubjobProcessNodeListDO;
import gtcloud.jobman.core.pdo.SubjobStatusReportDO;
import platon.ByteSeq;
import platon.ByteStream;
import platon.PropSet;
import platon.StringHolder;

@RestController
public class JobSchedulerController {
	
    private static Logger LOG = LoggerFactory.getLogger(JobSchedulerController.class);
    
    private static String getNodeBaseURL(PropSet options, HttpServletRequest request) {
    	// http://{host}:%s/gtjobproc
        String nodeBaseURL = options.get(ConstKeys.OPTION_PROCESSOR_BASE_URL);
        String ip = request.getRemoteAddr();
        nodeBaseURL = nodeBaseURL.replace("{host}", ip);
        return nodeBaseURL;
    }
    
	/**
	 * 处理执行节点发送的登录请求。
	 * @param request
	 * @return
	 */
    @PostMapping("/admin/node/logon")
    public void handleNodeLogonReq(HttpServletRequest request) {
    	
    	try {
    		ByteArray ba = SimpleUtils.readPostBody(request);
    		ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            LogonReqDO req = new LogonReqDO();
            req.defreeze(stream);
            String nodeBaseURL = getNodeBaseURL(req.getOptions(), request);
            
            if (LOG.isDebugEnabled()) {
            	LOG.debug("nodeLogonReq received, nodeId={}, nodeBaseURL={}", req.getNodeId(), nodeBaseURL);
            }
            
            JobSchedulerHolder.value.handleNodeLogonReq(nodeBaseURL, req);
    	}
    	catch (Exception ex) {
    		LOG.error("exception caught: {}", ex.getMessage());    		
    	}
    }

	/**
	 * 处理执行节点发送的登出请求。
	 * @param request
	 * @return
	 */
    @PostMapping("/admin/node/logoff")
    public void handleNodeLogoffReq(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("nodeLogoffReq received.");
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            LogoffDO req = new LogoffDO();
            req.defreeze(stream);
            JobSchedulerHolder.value.handleNodeLogoffReq(req);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }

	/**
	 * 处理执行节点发送的心跳报告。
	 * @param request
	 * @return
	 */    
    @PostMapping("/admin/node/heartbeat")
    public void handleNodeHearbeatReport(HttpServletRequest request) {
        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            HeartbeatReportReqDO req = new HeartbeatReportReqDO();
            req.defreeze(stream);
            String nodeBaseURL = getNodeBaseURL(req.getOptions(), request);            

            if (LOG.isDebugEnabled()) {
            	LOG.debug("nodeHearbeatReport received, nodeId={}, nodeBaseURL={}", req.getNodeId(), nodeBaseURL);
            }
            
            JobSchedulerHolder.value.handleNodeHeartbeatReq(nodeBaseURL, req);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }
    
	/**
	 * 处理执行节点发送的子作业状态报告。
	 * @param request
	 * @return
	 */    
    @PostMapping("/admin/subjob/status")
    public void handleSubjobStatusReport(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("subjobStatusReport received.");
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            SubjobStatusReportDO pdo = new SubjobStatusReportDO();
            pdo.defreeze(stream);
            JobSchedulerHolder.value.handleNodeStatusReport(pdo);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }
    }

	/**
	 * 处理执行节点发送的子作业已分派的应答。
	 * @param request
	 * @return
	 */    
    @PostMapping("/admin/subjob/dispatch/ack")
    public void handleSubjobDispatchAck(HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("subjobDispatchAck received.");
        }

        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            SubjobDispatchAckDO pdo = new SubjobDispatchAckDO();
            pdo.defreeze(stream);
            JobSchedulerHolder.value.handleSubjobDispatchAck(pdo);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
        }  	
    }
    
    /**
     * 处理请求者提交一个作业。
     * @param request
     * @return
     */       
    @PostMapping("/job")
    public void handleScheduleJobReq(HttpServletRequest request, HttpServletResponse response) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handleScheduleJobReq() called.");
        }

        PropSet ack = new PropSet();
        try {
            ByteArray ba = SimpleUtils.readPostBody(request);
            ByteStream stream = new ByteStream(ba.array, ba.offset, ba.length);
            JobControlBlockDO jobCB = new JobControlBlockDO();
            jobCB.defreeze(stream);
            ByteSeq jobBody = new ByteSeq(stream.readBlob());
            
            StringHolder s = new StringHolder();
            int rc = JobSchedulerHolder.value.handleJobScheduleRequest(jobCB, jobBody, s);
            buildScheduleJobAck(ack, rc, s.value);
        }
        catch (Exception ex) {
            LOG.error("exception caught: {}", ex.getMessage());
            buildScheduleJobAck(ack, -1, ex.getMessage());
        } 
        
        // 回送应答
        try {
			ByteStream stream  = new ByteStream();
			ack.freeze(stream);
			response.setContentType("application/octet-stream");
			response.setContentLength(stream.length());
			response.getOutputStream().write(stream.array(), 0, stream.length());
		} catch (Exception ex) {
            LOG.error("exception caught when sending ack: {}", ex.getMessage());
		}        
    }
    
    private static void buildScheduleJobAck(PropSet ack, int rc, String msg) {
    	ack.put("retcode", rc + "");
    	if (rc == 0) {
    		ack.put("retmsg", "0");
    	} else {
    		ack.put("retmsg", msg == null ? "unknown error" : msg);        		
    	}    	
    }
    
	/**
	 * 处理请求者要求获取所有执行节点的状态。
	 * @param request
	 * @return
	 */    
    @GetMapping("/node/status")
    public RestResult getAllProcessNodeStatus(HttpServletRequest request) {
    	SubjobProcessNodeListDO nodeList = new SubjobProcessNodeListDO();
        JobSchedulerHolder.value.getAllProcessNodeStatus(nodeList);
       	return RestResult.ok(nodeList, request);
    }
    
	/**
	 * 处理请求者要求获取所有作业的状态。
	 * @param request
	 * @return
	 */     
    @GetMapping("/job/status")
    public RestResult getAllJobStatus(HttpServletRequest request) {
    	JobStatusListDO jobStatusList = new JobStatusListDO();
        JobSchedulerHolder.value.getAllJobStatus(jobStatusList);
       	return RestResult.ok(jobStatusList, request);
    }

	/**
	 * 处理请求者要求获取指定作业的状态。
	 * @param request
	 * @return
	 */     
    @GetMapping("/job/status/{jobId}")
    public RestResult getJobStatus(@PathVariable String jobId, HttpServletRequest request) {
        StringHolder s = new StringHolder();
        JobStatusDO jobStatus = new JobStatusDO();
        int rc = JobSchedulerHolder.value.getJobStatus(jobId, jobStatus, s);
       	return rc == 0 ? RestResult.ok(jobStatus, request) : RestResult.error(rc, s.value, request);
    }
    
	/**
	 * 处理请求者要求获取scheduler的内部状态。
	 * @param request
	 * @return
	 */     
    @GetMapping("/scheduler/state/summary")
    public RestResult getSchedulerStateSummary(HttpServletRequest request) {
    	SchedulerStateSummaryDO summary = JobSchedulerHolder.value.getStateSummary();
       	return RestResult.ok(summary, request);
    }
    
	/**
	 * 处理请求者要求获取给定作业的所有子作业的内部状态。
	 * @param request
	 * @return
	 */     
    @GetMapping("/scheduler/state/allsubjobs/{jobId}")
    public RestResult dumpSubjobStateByJobId(@PathVariable String jobId, HttpServletRequest request) {
    	SubjobDumpStateListDO ack = JobSchedulerHolder.value.dumpAllSubjobsStateByJobId(jobId);
       	return RestResult.ok(ack, request);
    }

	/**
	 * 处理请求者要求dump给定子作业的事件日志。
	 * @param request
	 * @return
	 * @throws IOException 
	 */     
    @GetMapping("/scheduler/state/subjobeventlog/{jobId}/{subjobSeqNo}")
    public void dumpSubjobEventLogs(@PathVariable String jobId,
    		                        @PathVariable int subjobSeqNo,
    		                        HttpServletRequest request,
    		                        HttpServletResponse response) throws IOException {
    	ByteArrayOutputStream os = new ByteArrayOutputStream(); 
    	MiscUtils.emitString(os, "<html><body><pre>\n");
    	JobSchedulerHolder.value.dumpSubjobEventLogs(jobId, subjobSeqNo, os);
        MiscUtils.emitString(os, "</pre></body></html>\n");
        SimpleUtils.sendHtmlAsResponse(request, response, os);
    }

}
