package gtcloud.common;

import okhttp3.OkHttpClient;

public class SharedOkHttpClient {

    private static class OkHttpClientHolder {
        private static final OkHttpClient client = new OkHttpClient.Builder().build();
    }

    public static OkHttpClient get() {
        return OkHttpClientHolder.client;
    }
}
