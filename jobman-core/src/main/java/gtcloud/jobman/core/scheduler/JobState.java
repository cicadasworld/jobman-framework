package gtcloud.jobman.core.scheduler;

/**
 * ��ҵ������ҵ��״̬��
 */
public enum JobState {
    /**
     * ��ҵ������ҵ���ڵȴ�����ִ��
     */
    PENDING,

    /**
     * ��ҵ������ҵ�Ѿ���ɣ��ҳɹ����
     */
    FINISHED_SUCCESS,

    /**
     * ��ҵ������ҵ�Ѿ���ɣ���ʧ�����
     */
    FINISHED_FAILED;


    public static String formatToString(JobState js) {
        if (js == PENDING) {
            return "pending";
        }
        if (js == FINISHED_SUCCESS) {
            return "finished_success";
        }
        if (js == FINISHED_FAILED) {
            return "finished_failed";
        }
        return "unknown";
    }

    public static JobState parse(String str) {
        if ("pending".equalsIgnoreCase(str)) {
            return PENDING;
        }
        if ("finished_success".equalsIgnoreCase(str)) {
            return FINISHED_SUCCESS;
        }
        if ("finished_failed".equalsIgnoreCase(str)) {
            return FINISHED_FAILED;
        }
        return null;
    }

    public static String formatToMissionStatus(JobState js) {
        final String STATUS_PROCESSING = "processing";
        final String STATUS_SUCCESSED = "ok";
        final String STATUS_FAILED = "-1";
        if (js == PENDING) {
            return STATUS_PROCESSING;
        }
        if (js == FINISHED_SUCCESS) {
            return STATUS_SUCCESSED;
        }
        if (js == FINISHED_FAILED) {
            return STATUS_FAILED;
        }
        return "unknown";
    }
}
