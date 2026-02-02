package cafe.ivory.cronet;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigProvider extends ContentProvider {

    public static final String[] CONFIG_KEYS = {
            "magicNumber", "host", "port", "urlRequest", "urlResponseInfo",
            "getUrl", "byteBuffer", "callback", "onReadCompleted", "onSucceeded"
    };

    @Override
    public boolean onCreate() { return true; }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Context context = getContext();
        if (context == null) return null;
        
        ConfigManager configManager = new ConfigManager(context);
        
        // 获取配置
        if ("getConfig".equals(method)) {
            // arg 为 app 标识,如果为空则使用 default
            String appIdentifier = (arg != null && !arg.isEmpty()) ? arg : "default";
            Config config = configManager.getConfig(appIdentifier);
            
            Bundle reply = new Bundle();
            for (String key : CONFIG_KEYS) {
                String value = config.getString(key);
                reply.putString(key, value != null ? value : "");
            }
            reply.putString("appIdentifier", appIdentifier);
            return reply;
        }
        
        // 获取所有配置列表
        if ("getAppList".equals(method)) {
            Bundle reply = new Bundle();
            reply.putStringArrayList("appList", new ArrayList<>(configManager.getAppList()));
            return reply;
        }
        
        return null;
    }

    // 其他方法直接返回默认值即可
    @Nullable @Override public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) { return null; }
    @Nullable @Override public String getType(@NonNull Uri uri) { return null; }
    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
}