package com.ckr.smoothappbarlayout.listener;

import android.view.View;

import com.scwang.smartrefresh.listener.OnCollapsingListener;

/**
 * Created by Administrator on 2018/3/14.
 */

public interface OnScrollListener extends OnCollapsingListener,OnFlingListener{

    void setScrollTarget(View target);

    void onScrollChanged(View view, int y, int dy);

    void onScrollValueChanged(View view, int scrollY, boolean onStartNestedFling);

    void setCanDragHeader(boolean allow);

}
