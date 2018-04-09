package com.ckr.smoothappbarlayout.listener;

import android.view.View;

/**
 * Created by Administrator on 2018/3/14.
 */

public interface OnScrollListener extends OnOffsetListener,OnFlingListener{

    void setScrollTarget(View target);

    void onScrolled(View view, int dx, int dy);

    void onPreFling(View view, int scrollY);

    void setCanDragHeader(boolean allow);

}
