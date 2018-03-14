package com.ckr.smoothappbarlayout.listener;

import android.view.View;

/**
 * Created by Administrator on 2018/3/14.
 */

public interface OnScrollListener {

    void setScrollTarget(View target);

    void setCurrentScrollY(int scrollY);

    void onScrollChanged(View view, int y, int dy);

    void onScrollValueChanged(View view, int scrollY, boolean onStartNestedFling);
    void setOnFlingListener(OnFlingListener onFlingListener);
}
