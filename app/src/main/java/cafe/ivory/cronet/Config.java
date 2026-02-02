package cafe.ivory.cronet;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public final SharedPreferences sp;
    private final String appIdentifier;
    
    public static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>() {{
        put("magicNumber", "i0v0");
        put("host", "127.0.0.1");
        put("port", "9000");
        put("urlRequest", "org.chromium.net.UrlRequest");
        put("urlResponseInfo", "org.chromium.net.UrlResponseInfo");
        put("getUrl", "getUrl");
        put("byteBuffer", "java.nio.ByteBuffer");
        put("callback", "org.chromium.net.impl.VersionSafeCallbacks$UrlRequestCallback");
        put("onReadCompleted", "onReadCompleted");
        put("onSucceeded", "onSucceeded");
    }};

    public Config(SharedPreferences sp) {
        this(sp, "default");
    }

    public Config(SharedPreferences sp, String appIdentifier) {
        this.sp = sp;
        this.appIdentifier = appIdentifier;
    }

    private String getKey(String key) {
        return appIdentifier + "_" + key;
    }

    public String getAppIdentifier() {
        return appIdentifier;
    }

    public String getString(String key) {
        return sp.getString(getKey(key), DEFAULT_VALUES.get(key));
    }

    public void reset() {
        for (Map.Entry<String, String> entry : DEFAULT_VALUES.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sp.edit().putString(getKey(key), value).apply();
        }
    }

    public void putString(String key, String value) {
        sp.edit().putString(getKey(key), value).apply();
    }
}
