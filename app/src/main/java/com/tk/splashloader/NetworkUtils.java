package com.tk.splashloader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * <pre>
 *     author : TK
 *     time   : 2017/10/26
 *     desc   : 网络工具
 * </pre>
 */
public final class NetworkUtils {
    private NetworkUtils() {
        throw new IllegalStateException();
    }

    /**
     * 网络是否连接畅通
     *
     * @return
     */
    public static boolean isConnected() {
        NetworkInfo info = ((ConnectivityManager) Utils.getApp().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}