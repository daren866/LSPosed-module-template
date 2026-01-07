package com.example.module;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

/**
 * 这是 Xposed 模块的入口类。
 * 客户化建议：
 * 1. 修改包名 `com.example.module` 为你自己的包名。
 * 2. 在 `onSystemServerLoaded` 或 `onPackageLoaded` 中添加你的 Hook 逻辑。
 */
@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
public class MainModule extends XposedModule {

    public MainModule(XposedInterface base, ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onSystemServerLoaded(@NonNull SystemServerLoadedParam param) {
        super.onSystemServerLoaded(param);
        // 在这里添加针对 System Server 的 Hook 逻辑
        // 例如:
        // try {
        //     var classLoader = param.getClassLoader();
        //     var clazz = classLoader.loadClass("com.android.server.wm.WindowManagerService");
        //     // hook(method, MyHooker.class);
        // } catch (Throwable t) {
        //     log("Hook failed", t);
        // }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        // 在这里添加针对特定应用的 Hook 逻辑
        // if (param.getPackageName().equals("com.target.package")) {
        //     // ...
        // }
    }

    /**
     * 这是一个简单的 Hooker 示例。
     */
    @XposedHooker
    private static class ExampleHooker implements Hooker {
        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            // 在方法执行前执行的逻辑
        }

        // @AfterInvocation
        // public static void after(@NonNull AfterHookCallback callback) {
        //     // 在方法执行后执行的逻辑
        // }
    }
}
