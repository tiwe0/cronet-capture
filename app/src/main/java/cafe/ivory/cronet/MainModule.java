package cafe.ivory.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class MainModule extends XposedModule {

    private static MainModule mainModule;
    
    // 初始化标志，防止重复执行
    private static boolean hooked;

    public static String packageName = "cafe.ivory.love";

    // 魔法数字
    public static String magicNumber = "i0v0";

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

    public static native void initTransport();
    public static native void sendByteBuffer(String tagUrl, ByteBuffer byteBuffer, int position);
    public static native void sendString(String tagUrl, String string);
    public static native void end(String tagUrl);

    public MainModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log("MainModule at " + param.getProcessName());
        mainModule = this;
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

        }catch (NoSuchMethodException e) {
            log("Hook loadClass 失败: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public void loadNativeCronetForward(){
        try{
            String path = getApplicationInfo().nativeLibraryDir + "/libcronet_forwarder.so";
            System.load(path);
            log("加载 libcronet_forwarder.so 成功");
            MainModule.initTransport();
            log("初始化延迟连接成功");
        } catch (Exception e) {
            log("加载 libcronet_forwarder.so 失败: " + e.toString());
        }
    }

    @XposedHooker
    static public class hookAttachContextMethod implements  XposedInterface.Hooker {
        @AfterInvocation
        public static void afterInvocation(AfterHookCallback callback, hookAttachContextMethod context) {

            if (hooked) return;
            hooked = true;

            // 获取 Context 和 ClassLoader
            Context theContext = (Context) callback.getArgs()[0];
            ClassLoader realLoader = theContext.getClassLoader();

            // 初始化包名
            packageName = theContext.getPackageName();
            mainModule.log("attachBaseContext出发点,当前包名: " + packageName);

            // 通过 ContentProvider 读取配置,传入包名获取对应配置
            Uri uri = Uri.parse("content://cafe.ivory.cronet.config");
            Bundle configBundle = theContext.getContentResolver().call(uri, "getConfig", packageName, null);
            mainModule.log("尝试获取包名对应的配置: " + packageName);

            if (configBundle != null) {
                // 显示使用的配置标识
                String appIdentifier = configBundle.getString("appIdentifier", "未知");
                mainModule.log("成功获取配置,配置标识: " + appIdentifier);
                
                // 读取魔术数字
                magicNumber = configBundle.getString("magicNumber", magicNumber);
                mainModule.log("使用魔术数字: " + magicNumber);

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
            } else {
                mainModule.log("通过 ContentProvider 获取配置失败");
                mainModule.log("包名: " + packageName + " 可能没有对应的配置");
                mainModule.log("请在 MainActivity 中为此包名创建配置,Hook 取消");
                return;
            }

            mainModule.log("配置加载完毕");
            mainModule.log("开始加载native库");
            mainModule.loadNativeCronetForward();
            mainModule.log("native库加载完毕");
            mainModule.log("开始查找目标类并Hook");

            try {
                Callback = realLoader.loadClass(callbackClassName);
                mainModule.log("成功找到 Callback 类: " + callbackClassName);

                UrlRequest = realLoader.loadClass(urlRequestClassName);
                mainModule.log("成功找到 UrlRequest 类: " + urlRequestClassName);

                UrlResponseInfo = realLoader.loadClass(urlResponseInfoClassName);
                mainModule.log("成功找到 UrlResponseInfo 类: " + urlResponseInfoClassName);

                ByteBuffer = realLoader.loadClass(byteBufferClassName);
                mainModule.log("成功找到 ByteBuffer 类: " + byteBufferClassName);

                hookCronetCallback(Callback);

                mainModule.log("初始化完成");
            } catch (ClassNotFoundException e) {
                mainModule.log("Hook 失败: " + e.toString());
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
                MainModule.sendByteBuffer(url, byteBuffer, position);
            } catch (InvocationTargetException | IllegalAccessException e) {
                mainModule.log(e.toString());
            }
            return new onReadCompletedHook();
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
                MainModule.end(url);
            } catch (InvocationTargetException | IllegalAccessException e) {
                mainModule.log(e.toString());
            }
            return new onSucceededHook();
        }
    }
}
