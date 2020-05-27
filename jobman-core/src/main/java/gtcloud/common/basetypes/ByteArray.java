package gtcloud.common.basetypes;

public class ByteArray {

    public byte[] array = null;

    public int offset = 0;

    public int length = 0;

    public ByteArray() {}

    public ByteArray(byte[] bytes) {
        this.array = bytes;
        offset = 0;
        length = bytes.length;
    }

    public ByteArray(byte[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }
}
