package platon;


public interface Freezable {

    // 将当前对象打包进给定的字节流中。
    void freeze(ByteStream output) throws FreezeException;

    // 从给定字节流中解包, 并将解包得到的数据覆盖到当前对象上。
    void defreeze(ByteStream input) throws DefreezeException;

    // 克隆当前对象。
    Freezable makeClone();

    // 从给定对象拷贝数据到当前对象。
    void copyFrom(Freezable from);
}
