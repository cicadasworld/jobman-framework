package gtcloud.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gtcloud.common.basetypes.ByteArray;

public class StreamUtils {
	
    public static void close(InputStream istream) {
        if (istream == null) {
            return;
        }
        try {
            istream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close(OutputStream ostream) {
        if (ostream == null) {
            return;
        }
        try {
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static ByteArray readInputStream(InputStream fis) throws IOException {
        final int CHUNK_SIZE = 1024 * 4;
        byte[] chunk = new byte[CHUNK_SIZE];
        int total = 0;

        int sz= CHUNK_SIZE;
        byte bb[] = new byte[sz];

        for (;;) {
            int nread = fis.read(chunk, 0 , CHUNK_SIZE);
            if (nread <= 0) {
                break;
            }

            if (total + nread > sz) {
                sz = sz * 2 + 4;
                byte[] tmp = new byte[sz];
                System.arraycopy(bb, 0, tmp, 0, total);
                bb = tmp;
            }

            System.arraycopy(chunk, 0, bb, total, nread);
            total += nread;
        }

        return new ByteArray(bb, 0, total);
    }
}
