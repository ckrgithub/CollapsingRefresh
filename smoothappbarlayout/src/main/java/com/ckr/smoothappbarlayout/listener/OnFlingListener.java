package com.ckr.smoothappbarlayout.listener;

import android.view.View;

/**
 * Created by Administrator on 2018/3/5.
 */

public interface OnFlingListener {
    void onStartFling(float velocityY, int dy, View target);
}
