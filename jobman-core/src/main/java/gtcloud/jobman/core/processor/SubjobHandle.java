package gtcloud.jobman.core.processor;

// "子作业"描述子接口
public interface SubjobHandle {

    // "主作业"的标识，全局唯一, 如'4ea6530716d149429edb5749460938ec'
    String getJobId();

    // "主作业"的类别, 如'IMPORT_IMAGE_DATA'
    String getJobCategory();

    // 当前"子作业"的序号, 从0开始
    int getSubjobSeqNo();

    // 当前"子作业"在内，其所属"主作业"共包括几个子作业
    int getSiblingsCount();

    // 返回"子作业"的唯一性key
    String getSubjobKey();

    // 从给定对象拷贝数据到当前对象
    void copyFrom(SubjobHandle from);
}
