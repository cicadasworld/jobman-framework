package gtcloud.common.utils;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Selector;

public class ChannelUtils {

    public static void close(Channel ch) {
        if (ch == null) {
            return;
        }
        try {
            ch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(Selector selector) {
        if (selector == null) {
            return;
        }
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
