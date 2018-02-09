package com.ckr.collapsingrefresh;

import android.app.Application;

/**
 * Created by Administrator on 2017/8/11.
 */

public class BaseApplication extends Application {
    private static BaseApplication application;

    public static BaseApplication getInstance() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        //自定义缓存路径
//        PicassoUtil.configPicasso();
    }
}
