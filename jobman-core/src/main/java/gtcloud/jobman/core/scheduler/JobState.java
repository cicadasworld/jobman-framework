package gtcloud.jobman.core.scheduler;

/**
 * 作业或子作业的状态。
 */
public enum JobState {
    /**
     * 作业或子作业正在等待分派执行
     */
    PENDING,

    /**
     * 作业或子作业已经完成，且成功完成
     */
    FINISHED_SUCCESS,

    /**
     * 作业或子作业已经完成，但失败完成
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
