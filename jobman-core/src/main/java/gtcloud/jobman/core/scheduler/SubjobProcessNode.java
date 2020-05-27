package gtcloud.jobman.core.scheduler;

// 子作业处理节点
public interface SubjobProcessNode {
    /**
     * 返回节点Id.
     * @return 节点Id
     */
    String getNodeId();

    /**
     * 返回节点序号.
     * @return 节点序号
     */
    int getNodeSeqNo();

    /**
     * 判断当前节点是否处于活动状态.
     * @return
     */
    boolean isAlive();

}
