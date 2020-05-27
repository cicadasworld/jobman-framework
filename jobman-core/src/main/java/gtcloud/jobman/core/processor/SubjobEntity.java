package gtcloud.jobman.core.processor;

import gtcloud.common.basetypes.PropertiesEx;

/**
 * "子作业"实体对象。
 */
public interface SubjobEntity {
    /**
     * 返回"子作业"的描述子。
     * @return "子作业"的描述子
     */
    SubjobHandle getHandle();

    /**
     * 返回"主作业"的标题, 如'影像产品数据入库-20170427123022', 主要用于UI显示.
     * @return "主作业"的标题
     */
    String getJobCaption();

    /**
     * 获得作业的优先级。
     * @return 作业的优先级。
     */
    int getPriority();
    
    /**
     * 获得作业是否为收尾作业。
     * @return 是否为收尾作业。
     */
    boolean isReduce();

    /**
     * 返回子作业的可选属性。
     * @return 子作业的可选属性
     */
    PropertiesEx getOptions();

    /**
     * 返回子作业数据体，该数据的解释依赖于具体实现。
     * @return 子作业数据体
     */
    byte[] getSubjobBody();

    /**
     * 从给定对象拷贝数据到当前对象。
     * @param from
     * @param copyBody
     */
    void copyFrom(SubjobEntity from, boolean copyBody);
}
