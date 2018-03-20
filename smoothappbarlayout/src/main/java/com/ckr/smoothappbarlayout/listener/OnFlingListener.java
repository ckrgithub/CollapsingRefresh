package com.ckr.smoothappbarlayout.listener;

import android.view.View;

/**
 * Created by Administrator on 2018/3/5.
 */

public interface OnFlingListener {

    void onStartFling(View target,float velocityY);

    void onDispatchFling(View view, int mScrollState);

}
