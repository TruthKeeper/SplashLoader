package com.tk.splashloader;

import android.app.Application;

/**
 * <pre>
 *      author : TK
 *      time : 2017/10/26
 *      desc :
 * </pre>
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
