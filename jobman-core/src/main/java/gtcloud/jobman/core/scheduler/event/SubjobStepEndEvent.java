package gtcloud.jobman.core.scheduler.event;

//"����ҵ��ĳ���������"
public class SubjobStepEndEvent extends SubjobBaseEvent {
	
    static final String EVENTID = "StepEndddddd";

    // ����ı�ʶ
    private final String stepId;

    public SubjobStepEndEvent(long timestamp, String stepId) {
        super(timestamp);
        this.stepId = stepId;
    }

    public String getStepId() {
        return stepId;
    }

    @Override
    public String formatToLine() {
        return String.format("%s|%d|%s|%s",
                EVENTID,
                timestamp,
                formatTimestamp(timestamp),
                stepId);
    }

    static SubjobStepEndEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 4) {
            throw new Exception("�¼��ı��и�ʽ����: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        String stepId = vec[3];
        return new SubjobStepEndEvent(timestamp, stepId);
    }
}
