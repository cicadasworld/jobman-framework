package gtcloud.jobman.core.processor;

import gtcloud.common.basetypes.Lifecycle;

/**
 * 子作业处理器接口。
 */
public interface SubjobProcessor extends Lifecycle {
    /**
     * 运行给定子作业, 该方法实现时必须保证线程安全。
     * @param ctx 子作业运行上下文;
     * @param job 待运行的子作业对象。
     * @throws SubjobException
     */
    void processSubjob(SubjobContext ctx, SubjobEntity job) throws SubjobException;
    
    /**
     * 运行收尾工作的作业。
     * @param ctx 子作业运行上下文;
     * @param job 待运行的子作业对象。
     * @throws SubjobException
     */
    default void processReduce(SubjobContext ctx, SubjobEntity job) throws SubjobException {
    	//默认是什么也不做
    }
}
