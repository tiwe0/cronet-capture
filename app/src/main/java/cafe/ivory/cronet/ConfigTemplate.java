package cafe.ivory.cronet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置模板类 - 为常见应用提供预设配置
 */
public class ConfigTemplate {
    
    /**
     * 预设配置项
     */
    public static class Preset {
        public final String name;           // 显示名称
        public final String identifier;     // 配置标识
        public final String description;    // 描述
        public final Map<String, String> values;  // 配置值
        
        public Preset(String name, String identifier, String description) {
            this.name = name;
            this.identifier = identifier;
            this.description = description;
            this.values = new HashMap<>();
        }
        
        public Preset setValue(String key, String value) {
            this.values.put(key, value);
            return this;
        }
    }
    
    /**
     * 所有预设配置列表
     */
    private static final List<Preset> PRESETS = new ArrayList<>();
    
    static {
        // 小红书预设配置
        PRESETS.add(new Preset(
            "小红书 (9.18.0)",
            "com.xingin.xhs",
            "小红书配置"
        )
            .setValue("magicNumber", "i0v0")
            .setValue("host", "127.0.0.1")
            .setValue("port", "9000")
            .setValue("urlRequest", "org.chromium.net.h0")
            .setValue("urlResponseInfo", "org.chromium.net.i0")
            .setValue("getUrl", "f")
            .setValue("byteBuffer", "java.nio.ByteBuffer")
            .setValue("callback", "kj5.g")
            .setValue("onReadCompleted", "c")
            .setValue("onSucceeded", "f")
        );
        
        // 可以添加更多预设配置
        // 示例: 抖音配置
        PRESETS.add(new Preset(
            "抖音(37.7.0)",
            "com.ss.android.ugc.aweme",
            "抖音配置"
        )
            .setValue("magicNumber", "i0v0")
            .setValue("host", "127.0.0.1")
            .setValue("port", "9000")
            .setValue("urlRequest", "com.ttnet.org.chromium.net.UrlRequest")
            .setValue("urlResponseInfo", "com.ttnet.org.chromium.net.UrlResponseInfo")
            .setValue("getUrl", "getUrl")
            .setValue("byteBuffer", "java.nio.ByteBuffer")
            .setValue("callback", "com.ttnet.org.chromium.net.impl.VersionSafeCallbacks$UrlRequestCallback")
            .setValue("onReadCompleted", "onReadCompleted")
            .setValue("onSucceeded", "onSucceeded")
        );
        
        // 通用配置模板
        PRESETS.add(new Preset(
            "微博(16.1.4)",
            "com.sina.weibo",
            "适用于大多数使用 Cronet 的应用"
        )
            .setValue("magicNumber", "i0v0")
            .setValue("host", "127.0.0.1")
            .setValue("port", "9000")
            .setValue("urlRequest", "org.chromium.net.UrlRequest")
            .setValue("urlResponseInfo", "org.chromium.net.UrlResponseInfo")
            .setValue("getUrl", "getUrl")
            .setValue("byteBuffer", "java.nio.ByteBuffer")
            .setValue("callback", "org.chromium.net.impl.VersionSafeCallbacks$UrlRequestCallback")
            .setValue("onReadCompleted", "onReadCompleted")
            .setValue("onSucceeded", "onSucceeded")
        );
    }
    
    /**
     * 获取所有预设配置
     */
    public static List<Preset> getAllPresets() {
        return new ArrayList<>(PRESETS);
    }
    
    /**
     * 根据标识获取预设配置
     */
    public static Preset getPresetByIdentifier(String identifier) {
        for (Preset preset : PRESETS) {
            if (preset.identifier.equals(identifier)) {
                return preset;
            }
        }
        return null;
    }
    
    /**
     * 根据名称获取预设配置
     */
    public static Preset getPresetByName(String name) {
        for (Preset preset : PRESETS) {
            if (preset.name.equals(name)) {
                return preset;
            }
        }
        return null;
    }
    
    /**
     * 应用预设配置到 Config 对象
     */
    public static void applyPreset(Config config, Preset preset) {
        if (preset == null || config == null) {
            return;
        }
        
        for (Map.Entry<String, String> entry : preset.values.entrySet()) {
            config.putString(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 应用预设配置到 Config 对象 (通过标识)
     */
    public static void applyPreset(Config config, String identifier) {
        Preset preset = getPresetByIdentifier(identifier);
        applyPreset(config, preset);
    }
    
    /**
     * 获取预设配置的显示名称列表
     */
    public static List<String> getPresetNames() {
        List<String> names = new ArrayList<>();
        for (Preset preset : PRESETS) {
            names.add(preset.name);
        }
        return names;
    }
    
    /**
     * 检查某个标识是否为预设配置
     */
    public static boolean isPreset(String identifier) {
        return getPresetByIdentifier(identifier) != null;
    }
}
