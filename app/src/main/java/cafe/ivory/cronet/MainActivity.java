package cafe.ivory.cronet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "cafe.ivory.cronet.MainActivity";
    public static final Map<String, Integer> EDIT_TEXT_IDS = new HashMap<String, Integer>() {{
        put("host", R.id.et_host);
        put("port", R.id.et_port);
        put("urlRequest", R.id.cls_urlrequest);
        put("urlResponseInfo", R.id.cls_urlresponseinfo);
        put("getUrl", R.id.method_geturl);
        put("byteBuffer", R.id.cls_bytebuffer);
        put("callback", R.id.cls_callback);
        put("onReadCompleted", R.id.method_onreadcompleted);
        put("onSucceeded", R.id.method_onsucceeded);
    }};
    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        config = new Config(getSharedPreferences("config", Context.MODE_PRIVATE));
        initConfig(config);

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            config.reset();
            for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
                String key = entry.getKey();
                EditText editText = findViewById(entry.getValue());
                String value = Config.DEFAULT_VALUES.get(key);
                editText.setText(value);
            }
            Log.i(TAG, "重置配置");
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
                String key = entry.getKey();
                EditText editText = findViewById(entry.getValue());
                String value = editText.getText().toString();
                config.putString(key, value);
            }
            Log.i(TAG, "保存配置");
        });
    }

    private void initConfig(Config config){
        SharedPreferences sp = config.sp;
        for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
            String key = entry.getKey();
            EditText editText = findViewById(entry.getValue());
            String value = sp.getString(key, Config.DEFAULT_VALUES.get(key));
            editText.setText(value);
        }
    }
}