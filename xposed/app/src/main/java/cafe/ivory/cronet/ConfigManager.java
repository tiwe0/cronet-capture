package cafe.ivory.cronet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final String PREFS_NAME = "config";
    private static final String KEY_APP_LIST = "app_list";
    private static final String SEPARATOR = ",";
    
    private final SharedPreferences sp;
    private final Map<String, Config> configCache = new HashMap<>();
    
    public ConfigManager(Context context) {
        this.sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取指定 app 的配置
     * @param appIdentifier app 标识(如包名)
     * @return Config 实例
     */
    public Config getConfig(String appIdentifier) {
        if (!configCache.containsKey(appIdentifier)) {
            Config config = new Config(sp, appIdentifier);
            configCache.put(appIdentifier, config);
            
            // 将 app 添加到列表中
            addAppToList(appIdentifier);
        }
        return configCache.get(appIdentifier);
    }

    /**
     * 获取所有已保存的 app 标识列表
     * @return app 标识列表
     */
    public List<String> getAppList() {
        String appListStr = sp.getString(KEY_APP_LIST, "");
        List<String> appList = new ArrayList<>();
        if (!appListStr.isEmpty()) {
            String[] apps = appListStr.split(SEPARATOR);
            for (String app : apps) {
                if (!app.trim().isEmpty()) {
                    appList.add(app.trim());
                }
            }
        }
        // 确保至少有一个默认配置
        if (appList.isEmpty()) {
            appList.add("default");
        }
        return appList;
    }

    /**
     * 添加 app 到列表
     */
    private void addAppToList(String appIdentifier) {
        List<String> appList = getAppList();
        if (!appList.contains(appIdentifier)) {
            appList.add(appIdentifier);
            String appListStr = String.join(SEPARATOR, appList);
            sp.edit().putString(KEY_APP_LIST, appListStr).apply();
        }
    }

    /**
     * 删除指定 app 的配置
     * @param appIdentifier app 标识
     */
    public void deleteConfig(String appIdentifier) {
        // 从缓存中移除
        configCache.remove(appIdentifier);
        
        // 删除所有相关的配置项
        SharedPreferences.Editor editor = sp.edit();
        for (String key : Config.DEFAULT_VALUES.keySet()) {
            editor.remove(appIdentifier + "_" + key);
        }
        
        // 从列表中移除
        List<String> appList = getAppList();
        appList.remove(appIdentifier);
        String appListStr = String.join(SEPARATOR, appList);
        editor.putString(KEY_APP_LIST, appListStr);
        editor.apply();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        configCache.clear();
    }
}
