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
 * �ýӿ����ڼ���֮ǰ��������ͻ��ˣ���job�����mission.
 */
@RestController
public class MissionBridgeController {

    /**
     * ��ȡ���й�������
     * @return ���������б�
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions")
    @CrossOrigin
    public RestResult getAllMissions(HttpServletRequest request) {
        AdminMissionListDO missions = JobSchedulerHolder.value.getAllJobsAsMission();
        return RestResult.ok(missions, request);
    }

    /**
     * ��������ID��ȡһ����������
     * @param missionId ����ID��ȡֵ��jobId��ͬ
     * @return һ����������
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions/{missionId}")
    @CrossOrigin
    public RestResult getMissionById(@PathVariable String missionId, HttpServletRequest request) {
        AdminMissionDO mission = JobSchedulerHolder.value.getJobAsMission(missionId);
        if (mission != null) {
        	return RestResult.ok(mission, request);
        }
       	String errMsg = String.format("�Ҳ���missionId=%s������", missionId);
       	return RestResult.error(-2, errMsg, null, request);
    }

    /**
     * �������ڶλ�ȡ��������
     * @param from ��ʼʱ�䣬����2018-01-01
     * @param to ����ʱ�䣬����2018-01-01
     * @param host �ύ���������IP��ַ
     * @return ���������б�
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
     * ����������ID��ȡһ��������
     * @param missioItemId ������ID(ע�ⲻ������ҵ��ID)
     * @return һ��������
     * @throws Exception
     */
    @GetMapping(value = "/geodata/v1/missions/items/{missioItemId}")
    @CrossOrigin
    public RestResult getMissionItemById(@PathVariable String missioItemId, HttpServletRequest request) {
    	AdminMissionItemDO missionItem = JobSchedulerHolder.value.getSubjobAsMissionItem(missioItemId);
        if (missionItem != null) {
        	return RestResult.ok(missionItem, request);
        }
       	String errMsg = String.format("�Ҳ���missioItemId=%s��������", missioItemId);
       	return RestResult.error(-2, errMsg, null, request);
    }

}
