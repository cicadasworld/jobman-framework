package platon;

/**
 * Boolean值的Holder类，用来存储FZDL接口方法中类型为"bool"的"out"参数。
 *
 * <p>如果FZDL接口方法中将一个boolean标记成"out"参数，则调用方法时必须传递一个<code>BooleanHolder</code>
 * 实例作为方法调用中的相应参数，方法调用返回后通过其<code>value</code>字段来获得相应的输出值。
 */
public final class BooleanHolder
{
    /**
     * 构造一个新的BooleanHolder对象，将其value字段初始化为false。
     */
    public BooleanHolder() {}

    /**
     * 构造一个新的BooleanHolder对象，并使用给定boolean值初始化其value字段。
     *
     * @param value 用来初始化新建BooleanHolder对象的value字段。
     */
    public BooleanHolder(boolean value) {
        this.value = value;
    }

    /**
     * 此BooleanHolder对象保存的boolean值。
     */
    public boolean value = false;
}
