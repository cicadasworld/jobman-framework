package gtcloud.jobman.core.scheduler.event;

import gtcloud.jobman.core.pdo.Phase;

//"子作业的状态报告"
public class SubjobStatusReportEvent extends SubjobBaseEvent {

    public static final String EVENTID = "StatusReport";

    // "子作业"的最近状态码, 0表示正常, 其它值表示发生了错误
    private final int statusCode;

    // "子作业"的最近状态描述
    private final String statusMessage;

    // "子作业"目前已经完成的工作量
    private final double completedWorkload;

    // 子作业执行的当前阶段: Phase.PHASE_BEGIN, PHASE_DONE, ...
    private final int phase;

    public SubjobStatusReportEvent(long timestamp,
			                       int statusCode,
			                       String statusMessage,
			                       double completedWorkload,
			                       int phase) {
        super(timestamp);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.completedWorkload = completedWorkload;
        this.phase = phase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public double getCompletedWorkload() {
        return completedWorkload;
    }

    public int getPhase() {
        return phase;
    }

    @Override
    public String formatToLine() {
        String sphase = "unknown";
        if (phase == Phase.PHASE_BEGIN) {
            sphase = "PHASE_BEGIN";
        }
        else if (phase == Phase.PHASE_DONE) {
            sphase = "PHASE_DONE";
        }
        else if (phase == Phase.PHASE_INPROGRESS) {
            sphase = "PHASE_INPROGRESS";
        }

        return String.format("%s|%d|%s|%d|%s|%.8f|%s",
                EVENTID,
                timestamp,
                formatTimestamp(timestamp),
                statusCode,
                statusMessage,
                completedWorkload,
                sphase);
    }

    static SubjobStatusReportEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 7) {
            throw new Exception("事件文本行格式错误: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        int statusCode = Integer.parseInt(vec[3]);
        String statusMessage = vec[4];
        if (statusMessage.equals("null")) {
        	statusMessage = null;
        }
        double completedWorkload = Double.parseDouble(vec[5]);
        String sphase = vec[6];

        int phase = -1;
        if ("PHASE_BEGIN".equalsIgnoreCase(sphase)) {
            phase = Phase.PHASE_BEGIN;
        }
        else if ("PHASE_DONE".equalsIgnoreCase(sphase)) {
            phase = Phase.PHASE_DONE;
        }
        else if ("PHASE_INPROGRESS".equalsIgnoreCase(sphase)) {
            phase = Phase.PHASE_INPROGRESS;
        }

        return new SubjobStatusReportEvent(timestamp,
                statusCode,
                statusMessage,
                completedWorkload,
                phase);
    }

}
