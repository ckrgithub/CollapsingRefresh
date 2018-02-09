package com.ckr.smoothappbarlayout.base;

import android.util.Log;

/**
 * Created by Administrator on 2017/10/23.
 */

public class LogUtil {
    private static final String TAG = "LogUtil";
    private static final boolean DEBUG = true;

    public static void Logd(String tag, String msg) {
        if (DEBUG) {
            Log.d(TAG, tag + "-->" + msg);
        }
    }

    public static void Logi(String tag, String msg) {
        if (DEBUG) {
            Log.i(TAG, tag + "-->" + msg);
        }
    }

    public static void Logw(String tag, String msg) {
        if (DEBUG) {
            Log.w(TAG, tag + "-->" + msg);
        }
    }

    public static void Loge(String tag, String msg) {
        if (DEBUG) {
            Log.e(TAG, tag + "-->" + msg);
        }
    }

    public static void Logv(String tag, String methodName, String msg) {
        if (DEBUG) {
            Log.v(TAG, tag + "-->" + msg);
        }
    }
}
