package gtcloud.jobman.core.scheduler.event;

//"子作业分派应答已经接收"
public class SubjobDispatchAckReceivedEvent extends SubjobBaseEvent {
	
    public static final String EVENTID = "DispAckRcved";

    // 哪个处理节点发送来的应答
    private final String workerNodeId;

    public SubjobDispatchAckReceivedEvent(long timestamp, String workerNodeId) {
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

    static SubjobDispatchAckReceivedEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 4) {
            throw new Exception("事件文本行格式错误: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        String workerNodeId = vec[3];
        return new SubjobDispatchAckReceivedEvent(timestamp, workerNodeId);
    }
}
