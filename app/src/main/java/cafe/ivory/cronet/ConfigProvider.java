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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigProvider extends ContentProvider {

    public static final String[] CONFIG_KEYS = {
            "host", "port", "urlRequest", "urlResponseInfo",
            "getUrl", "byteBuffer", "callback", "onReadCompleted", "onSucceeded", "filterUrlPrefix"
    };

    @Override
    public boolean onCreate() { return true; }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        // 我们约定 method 为 "getConfig"
        if ("getConfig".equals(method)) {
            // 读取 sp 文件（确保和你 MainActivity 存的时候名字一致）
            SharedPreferences sp = Objects.requireNonNull(getContext()).getSharedPreferences("config", Context.MODE_PRIVATE);
            Bundle reply = new Bundle();
            for (String key : CONFIG_KEYS) {
                String value = sp.getString(key, "空");
                reply.putString(key, value);
            }
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