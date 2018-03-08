package com.ckr.smoothappbarlayout.base;

import android.view.View;

/**
 * Created by Administrator on 2018/3/5.
 */

public interface OnFlingCallBack {
    void onFlingFinished(float velocityY, int dy, View target);
}
