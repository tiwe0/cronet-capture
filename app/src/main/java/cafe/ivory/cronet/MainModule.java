package cafe.ivory.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class MainModule extends XposedModule {

    private static MainModule mainModule;

    // 状态
    public static boolean findTargetClass = false;
    private boolean soLoaded = false;

    // 混淆类的反射缓存
    public static Method getUrl;
    public static Class<?> UrlRequest;
    public static Class<?> UrlResponseInfo;
    public static Class<?> ByteBuffer;
    public static Class<?> Callback;
    public native void initTransport(String host, int port);
    public native void sendData(String tag, ByteBuffer byteBuffer, int position);
    public native void endData(String tag);

    private void loadConfigPrefs() {
        SharedPreferences pref = getRemotePreferences("config");
        Map<String, ?> allEntries = pref.getAll();
        if (allEntries.isEmpty()) {
            log("配置为空xp");
        } else {
            log("读取到配置");
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                log("key: " + entry.getKey() + "| value: " + entry.getValue().toString());
            }
        }
        for (Map.Entry<String, Integer> entry : MainActivity.EDIT_TEXT_IDS.entrySet()) {
            String key = entry.getKey();
            String value = pref.getString(key, "空");
            log("加载配置: " + key + " = " + value);
        }
    }

    public MainModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log("MainModule at " + param.getProcessName());
        mainModule = this;
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public void loadNativeCronetForward(){
        if (soLoaded) return;
        try{
            String path = getApplicationInfo().nativeLibraryDir + "/libcronet_forwarder.so";
            System.load(path);
            soLoaded = true;
            log("加载 libcronet_forwarder.so 成功");
            initTransport("127.0.0.1", 9999);
            log("初始化延迟连接成功");
        } catch (Exception e) {
            log("加载 libcronet_forwarder.so 失败: " + e.toString());
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (findTargetClass) return;
        loadConfigPrefs();
        try {
            Method attachMethod = ClassLoader.getSystemClassLoader()
                    .loadClass("android.content.ContextWrapper")
                    .getDeclaredMethod("attachBaseContext", Context.class);

            hook(attachMethod, hookAttachContextMethod.class);
            loadNativeCronetForward();

        }catch (NoSuchMethodException e) {
            log("Hook loadClass 失败: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @XposedHooker
    static public class hookAttachContextMethod implements  XposedInterface.Hooker {
        @AfterInvocation
        public static void afterInvocation(AfterHookCallback callback, hookAttachContextMethod context) {
            if (findTargetClass) {
                return;
            }

            Context theContext = (Context) callback.getArgs()[0];
            ClassLoader realLoader = theContext.getClassLoader();
            mainModule.log("attachBaseContext出发点，当前包名: " + theContext.getPackageName());

            try {
                Callback = realLoader.loadClass("kj5.g");
                mainModule.log("成功找到kj5.g");

                UrlRequest = realLoader.loadClass("org.chromium.net.h0");
                UrlResponseInfo = realLoader.loadClass("org.chromium.net.i0");
                ByteBuffer = realLoader.loadClass("java.nio.ByteBuffer");

                findTargetClass = true;
                hookCronetCallback(Callback);
            } catch (ClassNotFoundException e) {
                mainModule.log(e.toString());
            }
        }
    }

    private static void hookCronetCallback(Class<?> targetClass) {
        try {
            // 缓存一些反射结果
            getUrl = UrlResponseInfo.getDeclaredMethod("f");

            final String onReadCompletedMethodName = "c"; // onReadCompleted
            Method onReadCompletedMethod = targetClass.getDeclaredMethod(
                    onReadCompletedMethodName, UrlRequest, UrlResponseInfo, ByteBuffer
            );
            mainModule.log("Hook onReadCompleted: " + onReadCompletedMethod);
            mainModule.hook(onReadCompletedMethod, onReadCompletedHook.class);
            mainModule.log("Hooking onReadCompleted completed :)");

            final String onSucceededMethodName = "f";
            Method onSucceededMethod = targetClass.getDeclaredMethod(
                    onSucceededMethodName, UrlRequest, UrlResponseInfo
            );
            mainModule.log("Hook onSucceeded: " + onSucceededMethodName);
            mainModule.hook(onSucceededMethod, onSucceededHook.class);
            mainModule.log("Hooking onSucceeded completed :)");

        } catch (Exception ex) {
            mainModule.log("Error in finding class method & hooking :: " + ex);
        }
    }

    /// Hook Cronet 每次读取完数据  方法 "c"
    @XposedHooker
    static public class onReadCompletedHook implements XposedInterface.Hooker {
        @BeforeInvocation
        public static onReadCompletedHook beforeInvocation(BeforeHookCallback callback) {
            try {
                String url = (String) getUrl.invoke(callback.getArgs()[1]);
                java.nio.ByteBuffer byteBuffer = (java.nio.ByteBuffer) callback.getArgs()[2];
                int position = byteBuffer.position();
                mainModule.log("onReadCompleted: " + url);
                mainModule.sendData(url, byteBuffer, position);
            } catch (InvocationTargetException | IllegalAccessException e) {
                mainModule.log(e.toString());
            }
            return new onReadCompletedHook();
        }

        @AfterInvocation
        public static void afterInvocation(AfterHookCallback callback, onReadCompletedHook context) {

        }
    }

    /// Hook Cronet 请求完全结束  方法 "f"
    @XposedHooker
    static public class onSucceededHook implements  XposedInterface.Hooker {
        @BeforeInvocation
        public static onSucceededHook beforeInvocation(BeforeHookCallback callback) {
            try {
                String url = (String) getUrl.invoke(callback.getArgs()[1]);
                mainModule.log("onSucceeded: " + url);
                mainModule.endData(url);
            } catch (InvocationTargetException | IllegalAccessException e) {
                mainModule.log(e.toString());
            }

            return new onSucceededHook();
        }

        @AfterInvocation
        public static void afterInvocation(AfterHookCallback callback, onSucceededHook context) {

        }
    }

}
