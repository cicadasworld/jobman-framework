package gtcloud.jobman.core.scheduler.event;

//"子作业分派请求已经发送给某个处理节点"
public class SubjobDispatchReqSentEvent extends SubjobBaseEvent {
	
    static final String EVENTID = "DispaReqSent";

    // 请求发送给了哪个处理节点
    private final String workerNodeId;

    public SubjobDispatchReqSentEvent(long timestamp, String workerNodeId) {
        super(timestamp);
        this.workerNodeId = workerNodeId;
    }

    public String getWorkerNodeId() {
        return workerNodeId;
    }

    @Override
    public String formatToLine() {
        return String.format("%s|%d|%s|%s",
                EVENTID,
                timestamp,
                formatTimestamp(timestamp),
                workerNodeId);
    }

    /**
     * 从单行文本中解析出各个字段。
     * @param line 单行文本。
     */
    static SubjobDispatchReqSentEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 4) {
            throw new Exception("事件文本行格式错误: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        String workerNodeId = vec[3];
        return new SubjobDispatchReqSentEvent(timestamp, workerNodeId);
    }
}
