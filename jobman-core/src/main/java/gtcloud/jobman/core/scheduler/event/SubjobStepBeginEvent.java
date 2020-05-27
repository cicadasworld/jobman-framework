package gtcloud.jobman.core.scheduler.event;

//"����ҵ��ĳ�����迪ʼ"
public class SubjobStepBeginEvent extends SubjobBaseEvent {
	
    static final String EVENTID = "StepBeginnnn";

    // ����ı�ʶ
    private final String stepId;

    public SubjobStepBeginEvent(long timestamp, String stepId) {
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

    static SubjobStepBeginEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 4) {
            throw new Exception("�¼��ı��и�ʽ����: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        String stepId = vec[3];
        return new SubjobStepBeginEvent(timestamp, stepId);
    }
}
