package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import com.ckr.smoothappbarlayout.base.LogUtil;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class SmoothCoordinatorLayout extends CoordinatorLayout {
    private static final String TAG = "SmartCoordinatorLayout";

    public SmoothCoordinatorLayout(Context context) {
        super(context);
    }

    public SmoothCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasureChild(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        LogUtil.Logd(TAG, "onMeasureChild() called with: child = [" + child + "], parentWidthMeasureSpec = [" + parentWidthMeasureSpec
                + "], widthUsed = [" + widthUsed + "], parentHeightMeasureSpec = [" + parentHeightMeasureSpec + "], heightUsed = [" + heightUsed + "]");
        super.onMeasureChild(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    @Override
    public void onLayoutChild(View child, int layoutDirection) {
        LogUtil.Logd(TAG, "onLayoutChild() called with: child = [" + child + "], layoutDirection = [" + layoutDirection + "]");
        super.onLayoutChild(child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return super.onStartNestedScroll(child, target, nestedScrollAxes);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override
    public void onStopNestedScroll(View target) {
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // TODO: 2017/11/7  
        super.onNestedPreScroll(target,dx,dy,consumed);

    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        LogUtil.Logd(TAG, "onNestedFling: velocityY:" + velocityY + ",velocityX:" + velocityX);
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        LogUtil.Logd(TAG, "onNestedPreFling: onNestedPreFling,velocityY:" + velocityY + ",velocityX:" + velocityX);
        return super.onNestedPreFling(target, velocityX, velocityY);
    }
}
