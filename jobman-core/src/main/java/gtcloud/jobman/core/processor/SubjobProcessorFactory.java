package gtcloud.jobman.core.processor;

import java.util.ArrayList;

import gtcloud.common.basetypes.Lifecycle;

public interface SubjobProcessorFactory extends Lifecycle {
    /**
     * 返回当前工厂支持的作业类别列表。
     * @param result 携带当前工厂支持的作业类别列表返回,如[IMPORT_IMAGE_DATA]。
     */
    void getSupportedJobCategory(ArrayList<String> result);

    /**
     * 根据作业类别创建子作业处理器对象.
     *
     * @param jobCategory 作业类别, 如 IMPORT_IMAGE_DATA
     *
     * @return 子作业处理器对象.
     */
    SubjobProcessor createSubjobProcessor(String jobCategory);
}
