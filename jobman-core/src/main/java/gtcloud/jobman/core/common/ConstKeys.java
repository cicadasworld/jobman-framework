package gtcloud.jobman.core.common;

public class ConstKeys {
    public static final String OPTION_PROCESSOR_POOL_SIZE = "processorPoolSize";
    public static final String OPTION_PROCESSOR_BASE_URL = "processorBaseURL";

    // 作业提交者用户的ID, 该字段将设置在JobControlBlockDO的options属性集中
    public static final String JOB_PROP_SubmitterUserId = "_submitterUserId";

    // 提交者的机器IP, 该字段将设置在JobControlBlockDO的options属性集中
    public static final String JOB_PROP_SubmitterHost = "_submitterHost";

    // 子作业关联的地理资源关联的ID(若有意义的话), 该字段将设置在SubjobControlBlockDO的options属性集中
    public static final String SUBJOB_PROP_GeoEntityId = "_geoEntityId";

    // 子作业关联的地理资源关联的文件(若有意义的话), 该字段将设置在SubjobControlBlockDO的options属性集中
    public static final String SUBJOB_PROP_GeoEntityFile = "_geoEntityFile";
    
}
