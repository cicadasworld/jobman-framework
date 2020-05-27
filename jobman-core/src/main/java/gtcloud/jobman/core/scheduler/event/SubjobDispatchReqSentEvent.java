package gtcloud.jobman.core.scheduler.event;

//"����ҵ���������Ѿ����͸�ĳ������ڵ�"
public class SubjobDispatchReqSentEvent extends SubjobBaseEvent {
	
    static final String EVENTID = "DispaReqSent";

    // �����͸����ĸ�����ڵ�
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
     * �ӵ����ı��н����������ֶΡ�
     * @param line �����ı���
     */
    static SubjobDispatchReqSentEvent parseFromLine(String[] vec, String rawLine) throws Exception {
        if (vec == null || vec.length != 4) {
            throw new Exception("�¼��ı��и�ʽ����: " + rawLine);
        }
        assert vec[0].equals(EVENTID);
        long timestamp = Long.parseLong(vec[1]);
        //String YYYYMMDDHHMMSSsss = vec[2];
        String workerNodeId = vec[3];
        return new SubjobDispatchReqSentEvent(timestamp, workerNodeId);
    }
}
