package platon;

public class VarInt {

    public static int zigZagEncode32(int n) {
      // Note:  the right-shift must be arithmetic
      return (n << 1) ^ (n >> 31);
    }

    public static int zigZagDecode32(int n) {
      return (n >> 1) ^ -(n & 1);
    }

    public static long zigZagEncode64(long n) {
      // Note:  the right-shift must be arithmetic
      return (n << 1) ^ (n >> 63);
    }

    public static long zigZagDecode64(long n) {
      return (n >> 1) ^ -(n & 1);
    }

    // encode v into buf[offset...), return next write offset
    public static int encode32(byte[] buf, int offset, int v0) {
    	long v = v0;
        if (v < 0) {
            v = 4294967296L + v;
        }
        
        final int B = 128;
        if (v < (1<<7)) {
            buf[offset++] = (byte)(v);
        } else if (v < (1<<14)) {
            buf[offset++] = (byte)(v | B);
            buf[offset++] = (byte)(v>>7);
        } else if (v < (1<<21)) {
            buf[offset++] = (byte)(v | B);
            buf[offset++] = (byte)((v>>7) | B);
            buf[offset++] = (byte)(v>>14);
        } else if (v < (1<<28)) {
            buf[offset++] = (byte)(v | B);
            buf[offset++] = (byte)((v>>7) | B);
            buf[offset++] = (byte)((v>>14) | B);
            buf[offset++] = (byte)(v>>21);
        } else {
            buf[offset++] = (byte)(v | B);
            buf[offset++] = (byte)((v>>7) | B);
            buf[offset++] = (byte)((v>>14) | B);
            buf[offset++] = (byte)((v>>21) | B);
            buf[offset++] = (byte)(v>>28);
        }
        return offset;
    }

    // decode buf[offset, ...) into v, return next decode offset or return -1 on EOF
    public static int decode32(byte[] buf, int offset, int limit, int[] v) {
        int result = 0;
        for (int shift = 0; shift <= 28 && offset < limit; shift += 7) {
            int b = buf[offset++];
            if ((b & 128) != 0) {
                // More bytes are present
                result |= ((b & 127) << shift);
            } else {
                result |= (b << shift);
                v[0] = result;
                return offset;
            }
        }
        return -1;
    }

    // encode v into buf[offset...), return next write offset
    public static int encode64(byte[] buf, int offset, long v) {
        final int B = 128;
        while (v >= B) {
            buf[offset++] = (byte)((v & (B-1)) | B);
            v >>= 7;
        }
        buf[offset++] = (byte)(v);
        return offset;
    }

    // decode buf[offset, ...) into v, return next decode offset or return -1 on EOF
    public static int decode64(byte[] buf, int offset, int limit, long[] v) {
        long result = 0;
        for (int shift = 0; shift <= 63 && offset < limit; shift += 7) {
            int b = buf[offset++];
            if ((b & 128) != 0) {
                // More bytes are present
                result |= ((b & 127) << shift);
            } else {
                result |= (b << shift);
                v[0] = result;
                return offset;
            }
        }
        return -1;
    }
}
