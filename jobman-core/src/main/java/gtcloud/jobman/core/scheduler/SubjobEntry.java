package gtcloud.jobman.core.scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;

import gtcloud.jobman.core.common.ConstKeys;
import gtcloud.jobman.core.common.Helper;
import gtcloud.jobman.core.pdo.SubjobDumpStateDO;
import gtcloud.jobman.core.scheduler.event.SubjobBaseEvent;
import gtcloud.jobman.core.scheduler.event.SubjobDispatchAckReceivedEvent;
import gtcloud.jobman.core.scheduler.event.SubjobDispatchReqSentEvent;
import gtcloud.jobman.core.scheduler.event.SubjobStatusReportEvent;
import gtcloud.jobman.core.scheduler.mission.pdo.AdminMissionItemDO;

/**
 * ����ҵ����(���ڣ������ڲ�ʵ��)��
 */
public class SubjobEntry extends Subjob {

    private final JobPersister persister;

    private Object persisterCookie = null;

    // �ϴμ��뵽"���Զ���"��ʱ��
    private long whenLastEnterRetryQueue = 0;

    // ���Դ���
    private int retryTimes = 0;

    // ���һ�η������󱻷��͵�ʱ��
    private long whenLastDispatchReqSent = 0;

    // ���һ�α����ɵ����ĸ��ڵ�
    private String lastWorkerNodeId = null;

    // ���һ��״̬�����¼�����
    private SubjobStatusReportEvent lastStatusReportEvent = null;

    // ��������
    private static final String PROP_bornTime = "bornTime"; //����ҵ������ʱ��

    private long bornTime = 0;

    private long finishedTime = 0;

    // ����ҵ�ĵ�ǰ״̬
    private JobState state = JobState.PENDING;

    // �Ƿ��Ǵӳ־ö����лָ�������ҵ(ǰ������������)
    private final boolean isRestored;

    // readFromEventLogs()�Ƿ񱻵����ˣ�����isRestoredΪtrueʱʹ��
    private boolean isReadFromEventLogsCalled = false;

    public SubjobEntry(JobPersister persister) {
        this.persister = persister;
        this.isRestored = false;
        setBornTime(System.currentTimeMillis());
    }

    // for persister
    public SubjobEntry(JobPersister persister, JobState state) {
        this.persister = persister;
        this.state = state;
        this.isRestored = true;
    }

    public SubjobEntry(JobPersister persister, Subjob seed) {
        super(seed);
        this.persister = persister;
        this.isRestored = false;
        setBornTime(System.currentTimeMillis());
    }

    public void setBornTime(long bornTime) {
        this.bornTime = bornTime;
        this.props.put(PROP_bornTime, bornTime + "");
    }

    public long getBornTime() {
        if (this.bornTime == 0) {
            String v = this.props.get(PROP_bornTime);
            this.bornTime = v != null ? Long.parseLong(v) : System.currentTimeMillis();
        }
        return this.bornTime;
    }

    public JobState getState() {
        return state;
    }

    @Override
    public byte[] getBodyBytes() {
        if (this.bodyBytes == null && this.isRestored) {
            this.bodyBytes = this.persister.readSubjobBodyBytes(this);
        }
        return this.bodyBytes;
    }

    public String getKey() {
        return Helper.makeSubjobKey(this.subjobCB);
    }

    public void setWhenLastEnterRetryQueue(long now) {
        whenLastEnterRetryQueue = now;
    }

    // �ϴμ��뵽"���Զ���"��ʱ��
    public long getWhenLastEnterRetryQueue() {
        return whenLastEnterRetryQueue;
    }

    // ���Դ���
    public int getRetryTimes() {
        if (this.isRestored) {
            readFromEventLogs();
        }
        return retryTimes;
    }

    public void increaseRetryTimes() {
        if (this.isRestored) {
            readFromEventLogs();
        }
        retryTimes += 1;
    }

    public long getWhenLastDispatchReqSent() {
        return whenLastDispatchReqSent;
    }

    public String getLastWorkerNodeId() {
        return lastWorkerNodeId;
    }

    /**
     * ��������ҵ���ʱ��, ��persistor����
     * @param timestamp
     */
    public void setFinishedTime(long timestamp) {
        this.finishedTime = timestamp;
    }

    public void processEvent(SubjobBaseEvent event) {
        // ������־���־ô洢�У��������
        this.persister.writeSubjobEventLog(this, event);

        if (event instanceof SubjobDispatchReqSentEvent) {
            SubjobDispatchReqSentEvent e = (SubjobDispatchReqSentEvent)event;
            this.lastWorkerNodeId = e.getWorkerNodeId();
            this.whenLastDispatchReqSent = e.getTimestamp();
            return;
        }

        if (event instanceof SubjobStatusReportEvent) {
            this.lastStatusReportEvent = (SubjobStatusReportEvent)event;
            return;
        }
    }

    public void setFailureFinished() {
        // �ڳ־ô洢�б�ǳ�"ʧ�����"
        this.state = JobState.FINISHED_FAILED;
        this.finishedTime = System.currentTimeMillis();
        this.persister.writeSubjobFinishState(this, this.state);
    }

    public void setSuccessFinished() {
        // �ڳ־ô洢�б�ǳ�"�ɹ����"
        this.state = JobState.FINISHED_SUCCESS;
        this.finishedTime = System.currentTimeMillis();
        this.persister.writeSubjobFinishState(this, this.state);
    }

    public Object getPersisterCookie() {
        return persisterCookie;
    }

    public void setPersisterCookie(Object persisterCookie) {
        this.persisterCookie = persisterCookie;
    }

    public SubjobStatusReportEvent getLastStatusReportEvent() {
        if (this.isRestored && lastStatusReportEvent == null) {
            readFromEventLogs();
        }
        return lastStatusReportEvent;
    }

    private void readFromEventLogs() {
        if (!this.isReadFromEventLogsCalled) {
            this.isReadFromEventLogsCalled = true;
            this.persister.readSubjobEventLogs(this, (lineReader) -> {
                feedWithEventLines(lineReader);
            });
        }
    }

    /**
     * ��֮ǰ�־û�����־��¼�лָ�һЩ״̬����persister���á�
     * @param linereader
     */
    private void feedWithEventLines(LineNumberReader linereader) throws Exception {
        String lastStatusReportEventLine = null;
        int dispatchedTimes = 0;
        for (;;) {
            String line = linereader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();

            // Ŀǰֻ���SubjobDispatchAckReceivedEvent, SubjobStatusReportEvent�¼�,
            // ������ԵĴ���������״̬
            if (line.startsWith(SubjobDispatchAckReceivedEvent.EVENTID)) {
                dispatchedTimes += 1;
                continue;
            }
            if (line.startsWith(SubjobStatusReportEvent.EVENTID)) {
                lastStatusReportEventLine = line;
                continue;
            }
        }

        this.retryTimes = Math.max(0, dispatchedTimes - 1);

        if (lastStatusReportEventLine != null) {
            this.lastStatusReportEvent = (SubjobStatusReportEvent)
                    SubjobBaseEvent.parseEventFromLine(lastStatusReportEventLine);
        }
    }

    public AdminMissionItemDO asMissionItem() {
        AdminMissionItemDO amid = new AdminMissionItemDO();

        String jobId = this.subjobCB.getJobId();
        int subjobSeqNo = this.subjobCB.getSubjobSeqNo();
        amid.setId(Helper.makeMissionItemId(jobId, subjobSeqNo));
        amid.setMissionId(jobId);
        amid.setGeoEntityId(this.subjobCB.getOptions().get(ConstKeys.SUBJOB_PROP_GeoEntityId));

        // missionItem״̬
        String itemStatus = JobState.formatToMissionStatus(this.state);
        amid.getOptions().put("status", itemStatus);

        // ��ʼ�����ʱ��
        amid.getOptions().put("bornTime", this.getBornTime() + "");        
        if (this.finishedTime > 0) {
            amid.getOptions().put("finishedTime", this.finishedTime + "");
        }

        return amid;
    }

    void dumpState(SubjobDumpStateDO statedo) {
        statedo.getSubjobCB().copyFrom(this.getSubjobCB());
        statedo.setRetryTimes(this.retryTimes);
        statedo.setWhenLastEnterRetryQueue(this.whenLastEnterRetryQueue);
        statedo.setBornTime(this.getBornTime());
        statedo.setFinishedTime(this.finishedTime);
        statedo.setState(this.state + "");
        statedo.setRestored(this.isRestored);
        statedo.setTotalWorkload(this.getTotalWorkload());
        if (this.lastStatusReportEvent != null) {
            statedo.setCompletedWorkload(this.lastStatusReportEvent.getCompletedWorkload());
            statedo.setLastStatusCode(this.lastStatusReportEvent.getStatusCode());
            statedo.setLastStatusMessage(this.lastStatusReportEvent.getStatusMessage());
        }
    }

    void dumpEventLogs(ByteArrayOutputStream os) {
        this.persister.readSubjobEventLogs(this, (lineReader) -> {
            try {
                printEventLogLines(lineReader, os);
            } catch (IOException ex) {
                ;
            }
        });
    }

    private void printEventLogLines(LineNumberReader lineReader, ByteArrayOutputStream os) throws IOException {
        final byte[] LN = new byte[] {0x0d};
        for (;;) {
            String line = lineReader.readLine();
            if (line == null) {
                break;
            }
            os.write(line.getBytes());
            os.write(LN);
        }
    }


}
