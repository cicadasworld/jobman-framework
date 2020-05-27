package gtcloud.jobman.core.scheduler;

/**
 * 代表作业处理过程中的一个步骤，如影像文件入库，可分为"从入库工作站上拉取文件、创建缩略图、..."等几个步骤。
 */
public class StepInfo {
    
    // 步骤的唯一性标识
    private String id;

    // 对步骤的说明
    private String caption;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
