package cafe.ivory.cronet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "cafe.ivory.cronet.MainActivity";
    public static final Map<String, Integer> EDIT_TEXT_IDS = new HashMap<String, Integer>() {{
        put("magicNumber", R.id.et_magic_number);
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
    private ConfigManager configManager;
    private Config config;
    private Spinner spinnerAppSelect;
    private ArrayAdapter<String> appListAdapter;
    private String currentAppIdentifier = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        configManager = new ConfigManager(this);
        
        // 初始化预设配置
        initPresetConfigs();
        
        config = configManager.getConfig(currentAppIdentifier);
        
        initSpinner();
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
                Log.i(TAG, "保存配置项: " + key + " = " + value);
            }
            Log.i(TAG, "保存配置");
        });
    }

    private void initPresetConfigs() {
        // 获取所有预设配置
        List<ConfigTemplate.Preset> presets = ConfigTemplate.getAllPresets();
        
        // 检查并创建预设配置
        for (ConfigTemplate.Preset preset : presets) {
            // 检查配置是否已存在(通过检查是否有保存的值)
            Config existingConfig = configManager.getConfig(preset.identifier);
            String existingHost = existingConfig.getString("host");
            
            // 如果配置不存在或为空,则应用预设
            if (existingHost == null || existingHost.equals(Config.DEFAULT_VALUES.get("host"))) {
                ConfigTemplate.applyPreset(existingConfig, preset);
                Log.i(TAG, "已初始化预设配置: " + preset.name + " (" + preset.identifier + ")");
            } else {
                Log.i(TAG, "预设配置已存在,跳过: " + preset.name);
            }
        }
    }

    private void initConfig(Config config){
        for (Map.Entry<String, Integer> entry : EDIT_TEXT_IDS.entrySet()) {
            String key = entry.getKey();
            EditText editText = findViewById(entry.getValue());
            String value = config.getString(key);
            editText.setText(value);
        }
    }
    
    private void initSpinner() {
        spinnerAppSelect = findViewById(R.id.spinner_app_select);
        if (spinnerAppSelect == null) {
            // 如果布局中没有 Spinner,创建一个简单的切换对话框
            return;
        }
        
        updateAppList();
        
        spinnerAppSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedApp = (String) parent.getItemAtPosition(position);
                if (!selectedApp.equals(currentAppIdentifier)) {
                    switchConfig(selectedApp);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void updateAppList() {
        List<String> appList = configManager.getAppList();
        appListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appList);
        appListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAppSelect.setAdapter(appListAdapter);
        
        // 设置当前选中项
        int position = appList.indexOf(currentAppIdentifier);
        if (position >= 0) {
            spinnerAppSelect.setSelection(position);
        }
    }
    
    private void switchConfig(String appIdentifier) {
        currentAppIdentifier = appIdentifier;
        config = configManager.getConfig(appIdentifier);
        initConfig(config);
        Toast.makeText(this, "已切换到: " + appIdentifier, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "切换配置到: " + appIdentifier);
    }
    
    public void onAddConfig(View view) {
        // 显示选项: 从预设创建 或 自定义创建
        new AlertDialog.Builder(this)
            .setTitle("添加新配置")
            .setItems(new String[]{"从预设模板创建", "自定义配置"}, (dialog, which) -> {
                if (which == 0) {
                    showPresetDialog();
                } else {
                    showCustomConfigDialog();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showPresetDialog() {
        List<ConfigTemplate.Preset> presets = ConfigTemplate.getAllPresets();
        String[] presetNames = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) {
            presetNames[i] = presets.get(i).name + "\n" + presets.get(i).description;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("选择预设配置")
            .setItems(presetNames, (dialog, which) -> {
                ConfigTemplate.Preset preset = presets.get(which);
                createConfigFromPreset(preset);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showCustomConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("自定义配置");
        
        final EditText input = new EditText(this);
        input.setHint("输入 app 标识(如包名)");
        builder.setView(input);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String appIdentifier = input.getText().toString().trim();
            if (!appIdentifier.isEmpty()) {
                configManager.getConfig(appIdentifier);
                updateAppList();
                switchConfig(appIdentifier);
                Toast.makeText(this, "已添加配置: " + appIdentifier, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "添加自定义配置: " + appIdentifier);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void createConfigFromPreset(ConfigTemplate.Preset preset) {
        Config config = configManager.getConfig(preset.identifier);
        ConfigTemplate.applyPreset(config, preset);
        updateAppList();
        switchConfig(preset.identifier);
        Toast.makeText(this, "已应用预设: " + preset.name, Toast.LENGTH_LONG).show();
        Log.i(TAG, "从预设创建配置: " + preset.name + " (" + preset.identifier + ")");
    }
    
    public void onDeleteConfig(View view) {
        if ("default".equals(currentAppIdentifier)) {
            Toast.makeText(this, "不能删除默认配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("删除配置")
            .setMessage("确定要删除配置 " + currentAppIdentifier + " 吗?")
            .setPositiveButton("确定", (dialog, which) -> {
                configManager.deleteConfig(currentAppIdentifier);
                updateAppList();
                switchConfig("default");
                Toast.makeText(this, "已删除配置", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
}