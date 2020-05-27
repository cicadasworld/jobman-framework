package platon;

/**
 * String值的Holder类，用来存储FZDL接口方法中类型为"string"的"out"参数。
 *
 * <p>如果FZDL接口方法中将一个string标记成"out"参数，则调用方法时必须传递一个<code>StringHolder</code>
 * 实例作为方法调用中的相应参数，方法调用返回后通过其<code>value</code>字段来获得相应的输出值。
 */
public class StringHolder {

    /**
     * 构造一个新的StringHolder对象，将其value字段初始化为null。
     */
    public StringHolder() {}

    /**
     * 构造一个新的StringHolder对象，并使用给定String值初始化其value字段。
     *
     * @param value 用来初始化新建StringHolder对象的value字段。
     */
    public StringHolder(String value) {
        this.value = value;
    }

    /**
     * 此StringHolder对象保存的String值。
     */
    public String value = null;
}
