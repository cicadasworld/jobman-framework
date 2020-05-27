package platon;

/**
 * Long值的Holder类，用来存储FZDL接口方法中类型为"long"的"out"参数。
 *
 * <p>如果FZDL接口方法中将一个long标记成"out"参数，则调用方法时必须传递一个<code>LongHolder</code>
 * 实例作为方法调用中的相应参数，方法调用返回后通过其<code>value</code>字段来获得相应的输出值。
 */
public class LongHolder {

    /**
     * 构造一个新的LongHolder对象，将其value字段初始化为0。
     */
    public LongHolder() {}

    /**
     * 构造一个新的LongHolder对象，并使用给定long值初始化其value字段。
     *
     * @param value 用来初始化新建LongHolder对象的value字段。
     */
    public LongHolder(long value) {
        this.value = value;
    }

    /**
     * 此LongHolder对象保存的long值。
     */
    public long value = 0;
}
