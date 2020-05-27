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
 * 子作业对象(对内，用于内部实现)。
 */
public class SubjobEntry extends Subjob {

    private final JobPersister persister;

    private Object persisterCookie = null;

    // 上次加入到"重试队列"的时刻
    private long whenLastEnterRetryQueue = 0;

    // 重试次数
    private int retryTimes = 0;

    // 最近一次分派请求被发送的时刻
    private long whenLastDispatchReqSent = 0;

    // 最近一次被分派到了哪个节点
    private String lastWorkerNodeId = null;

    // 最近一次状态报告事件数据
    private SubjobStatusReportEvent lastStatusReportEvent = null;

    // 其它属性
    private static final String PROP_bornTime = "bornTime"; //子作业产生的时刻

    private long bornTime = 0;

    private long finishedTime = 0;

    // 子作业的当前状态
    private JobState state = JobState.PENDING;

    // 是否是从持久队列中恢复的子作业(前世保存下来的)
    private final boolean isRestored;

    // readFromEventLogs()是否被调用了，仅在isRestored为true时使用
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

    // 上次加入到"重试队列"的时刻
    public long getWhenLastEnterRetryQueue() {
        return whenLastEnterRetryQueue;
    }

    // 重试次数
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
     * 设置子作业完成时刻, 由persistor调用
     * @param timestamp
     */
    public void setFinishedTime(long timestamp) {
        this.finishedTime = timestamp;
    }

    public void processEvent(SubjobBaseEvent event) {
        // 薄记日志到持久存储中，方便调试
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
        // 在持久存储中标记成"失败完成"
        this.state = JobState.FINISHED_FAILED;
        this.finishedTime = System.currentTimeMillis();
        this.persister.writeSubjobFinishState(this, this.state);
    }

    public void setSuccessFinished() {
        // 在持久存储中标记成"成功完成"
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
     * 从之前持久化的日志记录中恢复一些状态，由persister调用。
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

            // 目前只需读SubjobDispatchAckReceivedEvent, SubjobStatusReportEvent事件,
            // 获得重试的次数、最后的状态
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

        // missionItem状态
        String itemStatus = JobState.formatToMissionStatus(this.state);
        amid.getOptions().put("status", itemStatus);

        // 开始、完成时刻
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
