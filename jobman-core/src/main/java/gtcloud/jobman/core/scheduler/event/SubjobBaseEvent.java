package gtcloud.jobman.core.scheduler.event;

import java.util.Calendar;

/**
 * ����ҵ�¼����ࡣ
 */
public class SubjobBaseEvent {
	
    // �¼�����ʱ��ʱ���
    protected final long timestamp;

    public SubjobBaseEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * ����ǰ�¼������ʽ���ɵ����ı���
     * @return �����ı�. eventId|timestamp|YYYY/MM/DD HH:MM:SS.sss|...
     */
    public String formatToLine() {
        return String.format("BaseEvent|%d|%s",
                timestamp,
                formatTimestamp(timestamp));    	
    }
    
    /**
     * ���ı����н������¼�����
     * @param line �ı���
     * @return �¼�����
     * @throws Exception ���ı�����Ч���׳��쳣
     */
    public static SubjobBaseEvent parseEventFromLine(String line) throws Exception {
        String[] vec = line.trim().split("\\|");
        if (vec == null || vec.length < 3) {
            throw new Exception("�¼��ı��и�ʽ����: " + line);
        }
        final String eventId = vec[0];

        if (SubjobStatusReportEvent.EVENTID.equals(eventId)) {
            return SubjobStatusReportEvent.parseFromLine(vec, line);
        }

        if (SubjobStepBeginEvent.EVENTID.equals(eventId)) {
            return SubjobStepBeginEvent.parseFromLine(vec, line);
        }

        if (SubjobStepEndEvent.EVENTID.equals(eventId)) {
            return SubjobStepEndEvent.parseFromLine(vec, line);
        }

        if (SubjobDispatchReqSentEvent.EVENTID.equals(eventId)) {
            return SubjobDispatchReqSentEvent.parseFromLine(vec, line);
        }

        if (SubjobDispatchAckReceivedEvent.EVENTID.equals(eventId)) {
            return SubjobDispatchAckReceivedEvent.parseFromLine(vec, line);
        }

        throw new Exception("����ʶ���¼�Id, �¼��ı���=" + line);
    }
    
    protected static String formatTimestamp(long millisec) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisec);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        int msec = calendar.get(Calendar.MILLISECOND);
        return String.format("%d/%02d/%02d %02d:%02d:%02d.%03d", year, month, day, hour, minute, sec, msec);
    }    
}
