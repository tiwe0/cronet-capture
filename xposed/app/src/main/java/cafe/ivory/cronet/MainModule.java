package cafe.ivory.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainModule extends XposedModule {

    private static final String TAG = "CronetCapture";

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

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        mainModule = this;
        logInfo("MainModule at " + param.getProcessName());
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.Q)
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (!param.isFirstPackage()) {
            return;
        }

        try {
            Method attachMethod = ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
            hook(attachMethod).intercept(chain -> {
                Object result = chain.proceed();
                handleAttachContext(chain);
                return result;
            });
        } catch (NoSuchMethodException e) {
            logError("Hook attachBaseContext 失败", e);
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public void loadNativeCronetForward() {
        try {
            String path = getModuleApplicationInfo().nativeLibraryDir + "/libcronet_forwarder.so";
            System.load(path);
            logInfo("加载 libcronet_forwarder.so 成功");
            MainModule.initTransport();
            logInfo("初始化延迟连接成功");
        } catch (Throwable t) {
            logError("加载 libcronet_forwarder.so 失败", t);
        }
    }

    private static void handleAttachContext(XposedInterface.Chain chain) {
        if (hooked) {
            return;
        }
        hooked = true;

        try {
            Context theContext = (Context) chain.getArg(0);
            ClassLoader realLoader = theContext.getClassLoader();

            packageName = theContext.getPackageName();
            mainModule.logInfo("attachBaseContext出发点,当前包名: " + packageName);

            Uri uri = Uri.parse("content://cafe.ivory.cronet.config");
            Bundle configBundle = theContext.getContentResolver().call(uri, "getConfig", packageName, null);
            mainModule.logInfo("尝试获取包名对应的配置: " + packageName);

            if (configBundle != null) {
                String appIdentifier = configBundle.getString("appIdentifier", "未知");
                mainModule.logInfo("成功获取配置,配置标识: " + appIdentifier);

                magicNumber = configBundle.getString("magicNumber", magicNumber);
                mainModule.logInfo("使用魔术数字: " + magicNumber);

                host = configBundle.getString("host", host);
                port = Integer.parseInt(configBundle.getString("port", Integer.toString(port)));
                mainModule.logInfo("更新转发配置: " + host + ":" + port);

                callbackClassName = configBundle.getString("callback", callbackClassName);
                mainModule.logInfo("使用 Callback 类名: " + callbackClassName);

                urlRequestClassName = configBundle.getString("urlRequest", urlRequestClassName);
                mainModule.logInfo("使用 UrlRequest 类名: " + urlRequestClassName);

                urlResponseInfoClassName = configBundle.getString("urlResponseInfo", urlResponseInfoClassName);
                mainModule.logInfo("使用 UrlResponseInfo 类名: " + urlResponseInfoClassName);

                byteBufferClassName = configBundle.getString("byteBuffer", byteBufferClassName);
                mainModule.logInfo("使用 ByteBuffer 类名: " + byteBufferClassName);

                getUrlMethodName = configBundle.getString("getUrl", getUrlMethodName);
                mainModule.logInfo("使用 getUrl 方法名: " + getUrlMethodName);

                onReadCompletedMethodName = configBundle.getString("onReadCompleted", onReadCompletedMethodName);
                mainModule.logInfo("使用 onReadCompleted 方法名: " + onReadCompletedMethodName);

                onSucceededMethodName = configBundle.getString("onSucceeded", onSucceededMethodName);
                mainModule.logInfo("使用 onSucceeded 方法名: " + onSucceededMethodName);
            } else {
                mainModule.logInfo("通过 ContentProvider 获取配置失败");
                mainModule.logInfo("包名: " + packageName + " 可能没有对应的配置");
                mainModule.logInfo("请在 MainActivity 中为此包名创建配置,Hook 取消");
                return;
            }

            mainModule.logInfo("配置加载完毕");
            mainModule.logInfo("开始加载native库");
            mainModule.loadNativeCronetForward();
            mainModule.logInfo("native库加载完毕");
            mainModule.logInfo("开始查找目标类并Hook");

            Callback = realLoader.loadClass(callbackClassName);
            mainModule.logInfo("成功找到 Callback 类: " + callbackClassName);

            UrlRequest = realLoader.loadClass(urlRequestClassName);
            mainModule.logInfo("成功找到 UrlRequest 类: " + urlRequestClassName);

            UrlResponseInfo = realLoader.loadClass(urlResponseInfoClassName);
            mainModule.logInfo("成功找到 UrlResponseInfo 类: " + urlResponseInfoClassName);

            ByteBuffer = realLoader.loadClass(byteBufferClassName);
            mainModule.logInfo("成功找到 ByteBuffer 类: " + byteBufferClassName);

            hookCronetCallback(Callback);
            mainModule.logInfo("初始化完成");
        } catch (Throwable t) {
            mainModule.logError("Hook 失败", t);
        }
    }

    private static void hookCronetCallback(Class<?> targetClass) {
        try {
            getUrl = UrlResponseInfo.getDeclaredMethod(getUrlMethodName);
            mainModule.logInfo("缓存 getUrl 方法: " + getUrl);

            Method onReadCompletedMethod = targetClass.getDeclaredMethod(
                    onReadCompletedMethodName, UrlRequest, UrlResponseInfo, ByteBuffer
            );
            mainModule.hook(onReadCompletedMethod).intercept(chain -> {
                onReadCompleted(chain);
                return chain.proceed();
            });
            mainModule.logInfo("成功 Hook onReadCompleted: " + onReadCompletedMethodName);

            Method onSucceededMethod = targetClass.getDeclaredMethod(
                    onSucceededMethodName, UrlRequest, UrlResponseInfo
            );
            mainModule.hook(onSucceededMethod).intercept(chain -> {
                onSucceeded(chain);
                return chain.proceed();
            });
            mainModule.logInfo("成功 Hook onSucceeded: " + onSucceededMethodName);

        } catch (Throwable t) {
            mainModule.logError("Error in finding class method & hooking", t);
        }
    }

    private static void onReadCompleted(XposedInterface.Chain chain) {
        try {
            String url = (String) getUrl.invoke(chain.getArg(1));
            java.nio.ByteBuffer byteBuffer = (java.nio.ByteBuffer) chain.getArg(2);
            int position = byteBuffer.position();
            mainModule.logInfo("onReadCompleted: " + url);
            MainModule.sendByteBuffer(url, byteBuffer, position);
        } catch (InvocationTargetException | IllegalAccessException e) {
            mainModule.logError("onReadCompleted hook 失败", e);
        }
    }

    private static void onSucceeded(XposedInterface.Chain chain) {
        try {
            String url = (String) getUrl.invoke(chain.getArg(1));
            mainModule.logInfo("onSucceeded: " + url);
            MainModule.end(url);
        } catch (InvocationTargetException | IllegalAccessException e) {
            mainModule.logError("onSucceeded hook 失败", e);
        }
    }

    private void logInfo(String message) {
        log(Log.INFO, TAG, message);
    }

    private void logError(String message, Throwable throwable) {
        log(Log.ERROR, TAG, message, throwable);
    }
}
