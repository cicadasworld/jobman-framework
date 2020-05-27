package gtcloud.jobman.core.scheduler;

import java.util.ArrayList;

import gtcloud.common.basetypes.Lifecycle;

public interface JobDispatcherFactory extends Lifecycle {
    /**
     * 返回当前工厂支持的作业类别列表。
     * @param result 携带当前工厂支持的作业类别列表返回, 如[IMPORT_IMAGE_DATA]。
     */
    void getSupportedJobCategory(ArrayList<String> result);

    /**
     * 根据作业类别创建作业分派器.
     *
     * @param jobCategory 作业类别, 如 IMPORT_IMAGE_DATA
     *
     * @return 作业分派器对象.
     */
    JobDispatcher createJobDispatcher(String jobCategory);

    /**
     * 给定类别的作业是否支持重试.
     *
     * @param jobCategory 作业类别, 如 IMPORT_IMAGE_DATA
     *
     * @return 若支持重试返回true.
     */
    boolean isSubjobRetriable(String jobCategory);

    /**
     * 返回给定类别的作业包含哪几个处理步骤.
     * @param jobCategory 作业类别, 如 IMPORT_IMAGE_DATA
     * @param result 输出参数，返回作业步骤定义.
     */
    default void getStepInfoList(String jobCategory, ArrayList<StepInfo> result) {
        ;
    }
}
