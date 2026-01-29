package cafe.ivory.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

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
    private static String[] configKeys = {
            "host", "port", "urlRequest", "urlResponseInfo",
            "getUrl", "byteBuffer", "callback", "onReadCompleted", "onSucceeded"
    };

    // 状态
    public static boolean findTargetClass = false;
    private boolean soLoaded = false;

    // 转发配置
    public static String host = "127.0.0.1";
    public static int port = 9000;

    // 混淆类名和字段名
    public static String callbackClassName = "kj5.g";
    public static String urlRequestClassName = "org.chromium.net.h0";
    public static String urlResponseInfoClassName = "org.chromium.net.i0";
    public static String byteBufferClassName = "java.nio.ByteBuffer";
    public static String getUrlMethodName = "f";
    public static String onReadCompletedMethodName = "c";
    public static String onSucceededMethodName = "f";

    // 混淆类的反射缓存
    public static Method getUrl;
    public static Class<?> UrlRequest;
    public static Class<?> UrlResponseInfo;
    public static Class<?> ByteBuffer;
    public static Class<?> Callback;

    public native void initTransport(String host, int port);
    public native void sendData(String tag, ByteBuffer byteBuffer, int position);
    public native void endData(String tag);

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
            initTransport(host, port);
            log("初始化延迟连接成功");
        } catch (Exception e) {
            log("加载 libcronet_forwarder.so 失败: " + e.toString());
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (!param.isFirstPackage()) return;
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

            Uri uri = Uri.parse("content://cafe.ivory.cronet.config");
            Bundle configBundle = theContext.getContentResolver().call(uri, "getConfig", null, null);
            if (configBundle != null) {
                // 更新转发配置
                host = configBundle.getString("host", host);
                port = Integer.parseInt(configBundle.getString("port", Integer.toString(port)));
                mainModule.log("更新转发配置: " + host + ":" + port);

                // 更新混淆类名和方法名
                callbackClassName = configBundle.getString("callback", callbackClassName);
                mainModule.log("使用 Callback 类名: " + callbackClassName);

                urlRequestClassName = configBundle.getString("urlRequest", urlRequestClassName);
                mainModule.log("使用 UrlRequest 类名: " + urlRequestClassName);

                urlResponseInfoClassName = configBundle.getString("urlResponseInfo", urlResponseInfoClassName);
                mainModule.log("使用 UrlResponseInfo 类名: " + urlResponseInfoClassName);

                byteBufferClassName = configBundle.getString("byteBuffer", byteBufferClassName);
                mainModule.log("使用 ByteBuffer 类名: " + byteBufferClassName);

                getUrlMethodName = configBundle.getString("getUrl", getUrlMethodName);
                mainModule.log("使用 getUrl 方法名: " + getUrlMethodName);

                onReadCompletedMethodName = configBundle.getString("onReadCompleted", onReadCompletedMethodName);
                mainModule.log("使用 onReadCompleted 方法名: " + onReadCompletedMethodName);

                onSucceededMethodName = configBundle.getString("onSucceeded", onSucceededMethodName);
                mainModule.log("使用 onSucceeded 方法名: " + onSucceededMethodName);
            }

            try {
                Callback = realLoader.loadClass(callbackClassName);
                mainModule.log("成功找到 Callback 类: " + callbackClassName);
                UrlRequest = realLoader.loadClass(urlRequestClassName);
                mainModule.log("成功找到 UrlRequest 类: " + urlRequestClassName);
                UrlResponseInfo = realLoader.loadClass(urlResponseInfoClassName);
                mainModule.log("成功找到 UrlResponseInfo 类: " + urlResponseInfoClassName);
                ByteBuffer = realLoader.loadClass(byteBufferClassName);
                mainModule.log("成功找到 ByteBuffer 类: " + byteBufferClassName);

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
            getUrl = UrlResponseInfo.getDeclaredMethod(getUrlMethodName);
            mainModule.log("缓存 getUrl 方法: " + getUrl);

            Method onReadCompletedMethod = targetClass.getDeclaredMethod(
                    onReadCompletedMethodName, UrlRequest, UrlResponseInfo, ByteBuffer
            );
            mainModule.hook(onReadCompletedMethod, onReadCompletedHook.class);
            mainModule.log("成功 Hook onReadCompleted: " + onReadCompletedMethodName);

            Method onSucceededMethod = targetClass.getDeclaredMethod(
                    onSucceededMethodName, UrlRequest, UrlResponseInfo
            );
            mainModule.hook(onSucceededMethod, onSucceededHook.class);
            mainModule.log("成功 Hook onSucceeded: " + onSucceededMethodName);

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
