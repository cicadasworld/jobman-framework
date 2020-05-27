package gtcloud.jobman.core.scheduler.event;

import gtcloud.jobman.core.pdo.Phase;

//"����ҵ��״̬����"
public class SubjobStatusReportEvent extends SubjobBaseEvent {

    public static final String EVENTID = "StatusReport";

    // "����ҵ"�����״̬��, 0��ʾ����, ����ֵ��ʾ�����˴���
    private final int statusCode;

    // "����ҵ"�����״̬����
    private final String statusMessage;

    // "����ҵ"Ŀǰ�Ѿ���ɵĹ�����
    private final double completedWorkload;

    // ����ҵִ�еĵ�ǰ�׶�: Phase.PHASE_BEGIN, PHASE_DONE, ...
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
            throw new Exception("�¼��ı��и�ʽ����: " + rawLine);
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
