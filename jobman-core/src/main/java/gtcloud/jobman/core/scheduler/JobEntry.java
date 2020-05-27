package gtcloud.jobman.core.scheduler;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.JobStatusDO;
import gtcloud.jobman.core.pdo.SubjobDumpStateDO;
import gtcloud.jobman.core.pdo.SubjobDumpStateListDO;
import gtcloud.jobman.core.scheduler.event.SubjobStatusReportEvent;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionDO;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionItemDO;

/**
 * ��ʶһ����ҵ����
 */
public class JobEntry {

    // ��ҵ���ƿ�
    private final JobControlBlockDO jcb;

    // ������Щ����ҵ
    private final ArrayList<SubjobEntry> subjobs = new ArrayList<>();

    // ��ҵ�ĵ�ǰ״̬
    private JobState state = JobState.PENDING;

    private final long bornTime;

    private long finishedTime = 0;

    private final JobPersister persister;

    private Object persisterCookie = null;

    public JobEntry(JobPersister persister,
                    JobControlBlockDO jcb,
                    ArrayList<SubjobEntry> list,
                    long bornTime) {
        this.persister = persister;
        this.jcb = jcb;
        this.bornTime = bornTime;
        for (int i=0; i<list.size(); ++i) {
            this.subjobs.add(null);
        }

        for (SubjobEntry e : list) {
            final int i = e.getSubjobCB().getSubjobSeqNo();
            assert this.subjobs.get(i) == null;
            this.subjobs.set(i, e);
        }
    }

    public JobEntry(JobPersister persister,
                    JobControlBlockDO jcb,
                    ArrayList<SubjobEntry> list,
                    JobState state,
                    long bornTime) {
        this(persister, jcb, list, bornTime);
        this.state = state;
    }

    public JobControlBlockDO getControlBlock() {
        return this.jcb;
    }

    public ArrayList<SubjobEntry> getSubjobs() {
        return this.subjobs;
    }

    public JobState getState() {
        return state;
    }

    public boolean canReduce() {
        for (SubjobEntry subjob : this.subjobs) {
            if (subjob.getSubjobCB().getIsReduce()) {
                continue;
            }
            if (subjob.getState() == JobState.PENDING) {
                return false;
            }
        }
        return true;
    }

    public static JobState evaluateJobState(final JobEntry job) {
        assert job != null;
        if (job == null) {
            return JobState.PENDING; //��Ϊ��û���
        }

        int successN = 0;
        int subjobN = job.getSubjobs().size();
        for (SubjobEntry subjob : job.getSubjobs()) {
            JobState s = subjob.getState();
            if (s == JobState.PENDING) {
                return JobState.PENDING; //��û���
            }
            if (s == JobState.FINISHED_SUCCESS) {
                successN += 1;
            }
        }

        return (successN == subjobN) ? JobState.FINISHED_SUCCESS : JobState.FINISHED_FAILED;
    }

    public void getStatus(JobStatusDO status) {
        status.setJobId(this.jcb.getJobId());
        status.setJobCategory(this.jcb.getJobCategory());
        status.setJobCaption(this.jcb.getJobCaption());

        // ��ǰ"��ҵ"���ܹ�����
        double totalWorkload = 0;
        double completedWorkload = 0;
        int statusCode = 0;
        String statusMessage = null;
        for (SubjobEntry subjob : this.subjobs) {
            totalWorkload += subjob.getTotalWorkload();
            SubjobStatusReportEvent evt = subjob.getLastStatusReportEvent();
            if (evt == null) {
                continue;
            }
            completedWorkload += evt.getCompletedWorkload();

            if (statusCode == 0 && evt.getStatusCode() != 0) {
                // ȡ��һ��ʧ��״̬������ҵ��״̬
                statusCode = evt.getStatusCode();
                statusMessage = evt.getStatusMessage();
            }
        }

        status.setJobEpoch(this.bornTime);
        status.setTotalWorkload(totalWorkload);
        status.setCompletedWorkload(completedWorkload);
        status.setStatusCode(statusCode);
        status.setStatusMessage(statusMessage);
        status.getOptions().put("state", this.state + "");
        status.getOptions().put("subjobN", this.subjobs.size() + "");
        if (this.finishedTime > 0) {
            status.getOptions().put("finishedTime", this.finishedTime + "");        	
        }
    }

    public long getBornTime() {
        return bornTime;
    }

    public void setFailureFinished() {
        // �ڳ־ô洢�б�ǳ�"ʧ�����"
        this.state = JobState.FINISHED_FAILED;
        this.finishedTime = System.currentTimeMillis();
        this.persister.writeJobFinishState(this, this.state);
    }

    public void setSuccessFinished() {
        // �ڳ־ô洢�б�ǳ�"�ɹ����"
        this.state = JobState.FINISHED_SUCCESS;
        this.finishedTime = System.currentTimeMillis();
        this.persister.writeJobFinishState(this, this.state);
    }

    /**
     * ������ҵ���ʱ��, ��persistor����
     * @param timestamp
     */
    public void setFinishedTime(long timestamp) {
        this.finishedTime = timestamp;
    }

    public Object getPersisterCookie() {
        return persisterCookie;
    }

    public void setPersisterCookie(Object persisterCookie) {
        this.persisterCookie = persisterCookie;
    }

    public boolean filter(long fromBornTime, long toBornTime, String host) {
        // ʱ����������
        if (fromBornTime <= this.getBornTime() && this.bornTime <= toBornTime) {
            ;
        } else {
            return false;
        }

        // �ύ������������
        if (host != null) {
            String h = this.jcb.getOptions().get(ConstKeys.JOB_PROP_SubmitterHost);
            if (!host.equalsIgnoreCase(h)) {
                return false;
            }
        }

        return true;
    }

    public SubjobEntry getSubjobBySeqNo(int subjobSeqNo) {
        if (subjobSeqNo < 0 || subjobSeqNo >= this.subjobs.size()) {
            return null;
        }
        return this.subjobs.get(subjobSeqNo);
    }

    /**
     * ��õ�ǰ��ҵ�����ݣ����������mission�ĳ�����
     * @return
     */
    public AdminMissionDO asMission() {
        AdminMissionDO amd = new AdminMissionDO();
        amd.setId(jcb.getJobId());
        amd.setCaption(jcb.getJobCaption());
        amd.setMissionType(jcb.getJobCategory());
        amd.setMissionEpoch(this.bornTime);
        amd.setSubmitterUserId(jcb.getOptions().get(ConstKeys.JOB_PROP_SubmitterUserId));
        amd.setSubmitterHost(jcb.getOptions().get(ConstKeys.JOB_PROP_SubmitterHost));

        // mission����
        try {
            JobStatusDO s = new JobStatusDO();
            getStatus(s);
            double ratio = s.getCompletedWorkload() / s.getTotalWorkload();
            String progress = String.format("%.2f", ratio);
            amd.getOptions().put("progress", progress);
        } catch (Throwable ex) { //��ֹ��0��
            amd.getOptions().put("progress", "0.0");
        }

        // mission״̬
        String missionStatus = JobState.formatToMissionStatus(this.state);
        amd.getOptions().put("status", missionStatus);

        // ���ʱ��
        if (this.finishedTime > 0) {
        	amd.getOptions().put("finishedTime", this.finishedTime + "");
        }
        
        // ������
        for (SubjobEntry subjob : this.subjobs) {
            AdminMissionItemDO amid = subjob.asMissionItem();
            amd.getItems().add(amid);
        }

        return amd;
    }

	public void dumpAllSubjobsState(SubjobDumpStateListDO result) {
		for (SubjobEntry subjob : this.subjobs) {
			SubjobDumpStateDO statedo = new SubjobDumpStateDO();
			subjob.dumpState(statedo);
			result.add(statedo);
		}
	}

	public void dumpSubjobEventLogs(int subjobSeqNo, ByteArrayOutputStream os) {
		SubjobEntry subjob = getSubjobBySeqNo(subjobSeqNo);
		if (subjob != null) {
			assert subjob.getSubjobCB().getSubjobSeqNo() == subjobSeqNo;
			subjob.dumpEventLogs(os);
		}
	}

}

