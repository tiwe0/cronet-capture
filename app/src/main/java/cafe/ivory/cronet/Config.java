package cafe.ivory.cronet;

import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public final SharedPreferences sp;
    public static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>() {{
        put("magicNumber", "i0v0");
        put("host", "127.0.0.1");
        put("port", "9000");
        put("urlRequest", "org.chromium.net.h0");
        put("urlResponseInfo", "org.chromium.net.i0");
        put("getUrl", "f");
        put("byteBuffer", "java.nio.ByteBuffer");
        put("callback", "kj5.g");
        put("onReadCompleted", "c");
        put("onSucceeded", "f");
    }};

    public Config(SharedPreferences sp) {
        this.sp = sp;
    }

    public void reset() {
        for (Map.Entry<String, String> entry : DEFAULT_VALUES.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sp.edit().putString(key, value).apply();
        }
    }

    public void putString(String key, String value) {
        sp.edit().putString(key, value).apply();
    }
}
