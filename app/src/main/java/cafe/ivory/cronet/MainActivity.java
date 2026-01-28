package cafe.ivory.cronet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    SharedPreferences sp;
    public static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>() {{
        put("host", "127.0.0.1");
        put("port", "9999");
        put("urlRequest", "org.chromium.net.UrlRequest");
        put("urlResponseInfo", "org.chromium.net.UrlResponseInfo");
        put("getUrl", "getUrl");
        put("bytebuffer", "java.nio.ByteBuffer");
        put("callback", "org.chromium.net.UrlRequest$Callback");
        put("onReadCompleted", "onReadCompleted");
        put("onSucceeded", "onSucceeded");
    }};
    public static final Map<String, Integer> EDIT_TEXT_IDS = new HashMap<String, Integer>() {{
       put("host", R.id.et_host);
       put("port", R.id.et_port);
       put("urlRequest", R.id.cls_urlrequest);
       put("urlResponseInfo", R.id.cls_urlresponseinfo);
       put("getUrl", R.id.method_geturl);
       put("bytebuffer", R.id.cls_bytebuffer);
       put("callback", R.id.cls_callback);
       put("onReadCompleted", R.id.method_onreadcompleted);
       put("onSucceeded", R.id.method_onsucceeded);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context deviceContext = createDeviceProtectedStorageContext();
        sp = deviceContext.getSharedPreferences("config", Context.MODE_PRIVATE);

        loadConfig();

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            resetConfig(sp);
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            saveConfig(sp);
        });
    }

    private void loadConfig() {
        for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
            EditText et = findViewById(entry.getValue());
            // 读取保存的值，如果没有则用 DEFAULT_VALUES 里的
            String savedValue = sp.getString(entry.getKey(), DEFAULT_VALUES.get(entry.getKey()));
            et.setText(savedValue);
        }
        fixPermissions();
    }

    private void resetConfig(SharedPreferences sp) {
        SharedPreferences.Editor e = sp.edit();
        for (Map.Entry<String, String> entry : DEFAULT_VALUES.entrySet()) {
            e.putString(entry.getKey(), entry.getValue());
            EditText et = findViewById(EDIT_TEXT_IDS.get(entry.getKey()));
            et.setText(entry.getValue());
        }
        e.apply();
        fixPermissions();
    }

    private void saveConfig(SharedPreferences sp) {
        // 从 EditText 获取用户输入并保存到 SharedPreferences
        SharedPreferences.Editor e = sp.edit();
        for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
            EditText et = findViewById(entry.getValue());
            String value = et.getText().toString().trim();
            e.putString(entry.getKey(), value);
        }
        e.apply();
        fixPermissions();
    }

    @SuppressLint("SetWorldReadable")
    private void fixPermissions() {
        File sharedPrefsDir = new File(getDataDir(), "shared_prefs");
        File prefFile = new File(sharedPrefsDir, "config.xml");
        if (prefFile.exists()) {
            prefFile.setReadable(true, false); // 让所有进程可读
        }
    }
}