package gtcloud.common.basetypes;

/**
 * 该接口用来代表资源（如js、瓦片等）的属性。
 */
public interface ResourceAttributes {

    /**
     * 获得资源的ETag。
     * @return 资源的ETag。
     */
    String getETag();

    /**
     * 获得资源的最后修改时间。
     * @return 资源的最后修改时间。
     */
    long getLastModified();
}
