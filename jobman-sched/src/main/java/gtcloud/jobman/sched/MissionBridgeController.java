package gtcloud.jobman.sched;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gtcloud.common.web.RestResult;
import gtcloud.jobman.core.scheduler.mission.MissionQueryFilter;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionDO;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionItemDO;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionListDO;

/**
 * 该接口用于兼容之前的浏览器客户端，将job适配成mission.
 */
@RestController
public class MissionBridgeController {

    /**
     * 获取所有管理任务
     * @return 管理任务列表
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions")
    @CrossOrigin
    public RestResult getAllMissions(HttpServletRequest request) {
        AdminMissionListDO missions = JobSchedulerHolder.value.getAllJobsAsMission();
        return RestResult.ok(missions, request);
    }

    /**
     * 根据任务ID获取一条管理任务
     * @param missionId 任务ID，取值与jobId相同
     * @return 一条管理任务
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions/{missionId}")
    @CrossOrigin
    public RestResult getMissionById(@PathVariable String missionId, HttpServletRequest request) {
        AdminMissionDO mission = JobSchedulerHolder.value.getJobAsMission(missionId);
        if (mission != null) {
        	return RestResult.ok(mission, request);
        }
       	String errMsg = String.format("找不到missionId=%s的任务", missionId);
       	return RestResult.error(-2, errMsg, null, request);
    }

    /**
     * 根据日期段获取管理任务
     * @param from 起始时间，形如2018-01-01
     * @param to 结束时间，形如2018-01-01
     * @param host 提交任务机器的IP地址
     * @return 管理任务列表
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions/query")
    @CrossOrigin
    public RestResult queryMissionsByFilter(
            @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(value = "host", required = false) String host,
            HttpServletRequest request) {
        MissionQueryFilter filter = new MissionQueryFilter();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setHost(host);
        AdminMissionListDO missions = JobSchedulerHolder.value.getJobsAsMissionByFilter(filter);
        return RestResult.ok(missions, request);
    }

    /**
     * 根据任务项ID获取一条任务项
     * @param missioItemId 任务项ID(注意不是子作业的ID)
     * @return 一条任务项
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions/items/{missioItemId}")
    @CrossOrigin
    public RestResult getMissionItemById(@PathVariable String missioItemId, HttpServletRequest request) {
    	AdminMissionItemDO missionItem = JobSchedulerHolder.value.getSubjobAsMissionItem(missioItemId);
        if (missionItem != null) {
        	return RestResult.ok(missionItem, request);
        }
       	String errMsg = String.format("找不到missioItemId=%s的任务项", missioItemId);
       	return RestResult.error(-2, errMsg, null, request);
    }

}
