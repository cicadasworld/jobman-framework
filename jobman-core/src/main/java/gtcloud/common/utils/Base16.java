package gtcloud.common.utils;

public final class Base16 {

    /**
     * Encodes hex octects into Base16
     *
     * @param binaryData
     *            Array containing binaryData
     * @return Encoded Base16 array
     */
    public static byte[] encode(byte[] binaryData) {
        return encode(binaryData, 0, binaryData.length);
    }

    // 整数转成16进制字符
    public static int intToHexChar(int n) {
        if (0 <= n && n <= 9)
            return '0' + n;
        if (10 <= n && n <= 15)
            return 'A' - 10 + n;
        return -1;
    }

    // 16进制字符转成整数
    public static int hexCharToInt(int c) {
        if ('0' <= c && c <= '9')
            return c - '0';
        if ('a' <= c && c <= 'f')
            return c - 'a' + 10;
        if ('A' <= c && c <= 'F')
            return c - 'A' + 10;
        return -1;
    }

    /**
     * Encodes hex octects into Base16
     *
     * @param binaryData
     *            Array containing binaryData
     * @param nlen
     *            count of bytes to encode
     * @return Encoded Base16 array
     */
    public static byte[] encode(byte[] binaryData, int offset, int nlen) {
        int nlen16 = 2 * nlen;
        byte[] b16 = new byte[nlen16];

        int j = 0;
        int end = offset + nlen;
        int hi, lo;

        for (int i = offset; i < end; ++i) {
            hi = (binaryData[i] >> 4) & 0x0F;
            lo = (binaryData[i]) & 0x0F;
            b16[j + 0] = (byte) intToHexChar(hi);
            b16[j + 1] = (byte) intToHexChar(lo);
            j += 2;
        }

        return b16;
    }
}