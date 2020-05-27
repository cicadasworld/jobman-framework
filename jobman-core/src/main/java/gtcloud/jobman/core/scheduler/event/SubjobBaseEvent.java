package gtcloud.jobman.core.scheduler.event;

import java.util.Calendar;

/**
 * 子作业事件基类。
 */
public class SubjobBaseEvent {
	
    // 事件发生时的时间戳
    protected final long timestamp;

    public SubjobBaseEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 将当前事件对象格式化成单行文本。
     * @return 单行文本. eventId|timestamp|YYYY/MM/DD HH:MM:SS.sss|...
     */
    public String formatToLine() {
        return String.format("BaseEvent|%d|%s",
                timestamp,
                formatTimestamp(timestamp));    	
    }
    
    /**
     * 从文本行中解析出事件对象。
     * @param line 文本行
     * @return 事件对象
     * @throws Exception 若文本行无效将抛出异常
     */
    public static SubjobBaseEvent parseEventFromLine(String line) throws Exception {
        String[] vec = line.trim().split("\\|");
        if (vec == null || vec.length < 3) {
            throw new Exception("事件文本行格式错误: " + line);
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

        throw new Exception("不认识的事件Id, 事件文本行=" + line);
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
