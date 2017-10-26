package com.tk.splashloader;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.NonNull;

/**
 * <pre>
 *      author : TK
 *      time : 2017/10/26
 *      desc :
 * </pre>
 */

public final class Utils {
    @SuppressLint("StaticFieldLeak")
    private static Application app = null;

    private Utils() {
        throw new IllegalStateException();
    }

    /**
     * 初始化工具类
     *
     * @param app 应用
     */
    public static void init(@NonNull Application app) {
        Utils.app = app;
    }

    /**
     * 获取Application
     *
     * @return
     */
    public static Application getApp() {
        if (app == null) {
            throw new NullPointerException("should init first !");
        }
        return app;
    }
}
