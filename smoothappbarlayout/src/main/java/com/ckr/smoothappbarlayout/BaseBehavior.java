/*
 * Copyright 2016 "Henry Tao <hi@henrytao.me>"
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ckr.smoothappbarlayout.base.LogUtil;
import com.ckr.smoothappbarlayout.base.OnSmoothScrollListener;
import com.ckr.smoothappbarlayout.base.Utils;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.support.v4.view.ViewCompat.offsetTopAndBottom;
import static com.ckr.smoothappbarlayout.base.LogUtil.Logd;


public abstract class BaseBehavior extends AppBarLayout.Behavior implements OnSmoothScrollListener {
    private static final String TAG = "BaseBehavior";
    private AppBarLayout child;
    private DragCallback mDragCallbackListener;
    private boolean mIsOnInit = false;
    protected View vScrollTarget;
    private Runnable mFlingRunnable;
    private ScrollerCompat mScroller;
    protected int mTotalScrollY;
    private boolean isFling;
    private static final int VELOCITY_UNITS = 1000;//1000 provides pixels per second

    private ViewOffsetHelper mViewOffsetHelper;
    private int mTempTopBottomOffset = 0;
    protected int mCurrentOffset;
    private float velocityY;

    public BaseBehavior() {
    }

    public BaseBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout coordinatorLayout, AppBarLayout child, int parentWidthMeasureSpec, int widthUsed,
                                  int parentHeightMeasureSpec, int heightUsed) {
        if (!mIsOnInit && coordinatorLayout != null && child != null) {
            mIsOnInit = true;
            init(child);
        }
        return super.onMeasureChild(coordinatorLayout, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    private void init(final AppBarLayout child) {
        this.child = child;
        // TODO: 2017/11/1 是否允许header拖动
        if (mDragCallbackListener == null) {
            mDragCallbackListener = new DragCallback() {
                @Override
                public boolean canDrag(AppBarLayout appBarLayout) {
                    return false;
                }
            };
            setDragCallback(mDragCallbackListener);
        }
    }

    /**
     * to see HeaderBehavior.method:fling()
     *
     * @param layout
     * @param startY
     * @param minOffset
     * @param maxOffset
     * @param velocityY
     * @param flingUp
     * @return
     */
    final boolean fling(AppBarLayout layout, View target, int startY, int minOffset,
                        int maxOffset, float velocityY, boolean flingUp) {
        Logd(TAG, "fling: startY:" + startY + ",minOffset:" + minOffset + ",maxOffset:" + maxOffset
                + ",maxOffset:" + maxOffset + ",velocityY:" + velocityY + ",flingUp:" + flingUp + ",mTotalScrollY:" + mTotalScrollY);
        if (mFlingRunnable != null) {
            if (mScroller != null) {
                mScroller.abortAnimation();
            }
            layout.removeCallbacks(mFlingRunnable);
            mFlingRunnable = null;
        }
        if (mScroller == null) {
            mScroller = ScrollerCompat.create(layout.getContext());
        }
        mScroller.fling(0, -startY, 0, Math.round(velocityY), 0, 0, minOffset, maxOffset);
        boolean canScroll = mScroller.computeScrollOffset();
        Logd(TAG, "fling: canScroller: " + canScroll);
        if (canScroll) {
            mFlingRunnable = new FlingRunnable(layout, target, flingUp, mTotalScrollY <= 423);
            ViewCompat.postOnAnimation(layout, mFlingRunnable);
            return true;
        } else {
            return false;
        }
    }

    private class FlingRunnable implements Runnable {
        private final AppBarLayout mLayout;
        private final View scrollTarget;
        private final boolean isFlingUp;
        private final boolean accuracy;

        FlingRunnable(AppBarLayout layout, View target, boolean flingUp, boolean accuracy) {
            mLayout = layout;
            scrollTarget = target;
            isFlingUp = flingUp;
            this.accuracy = accuracy;
        }

        @Override
        public void run() {
            if (mLayout != null && mScroller != null && vScrollTarget == scrollTarget) {
                boolean canScroll = mScroller.computeScrollOffset();
                Logd(TAG, "run: canScroll:" + canScroll + ",isFlingUp:" + isFlingUp);
                if (canScroll) {
                    int currY = mScroller.getCurrY();
                    isFling = true;
                    if (isFlingUp) {
                        int y = -currY;
                        LogUtil.Loge(TAG, "run: currY:" + currY);
                        if (currY <= 423) {
                            setTopAndBottomOffset(y);
                        }
                    } else {
                        int top = child.getTop();
                        int y = 0;
                        if (accuracy) {
                            y = Math.abs(currY - top);
                        } else {
                            y = Math.abs(mTotalScrollY + top);
                        }
                        LogUtil.Loge(TAG, "run: currY:" + currY + ",mTotalScrollY:" + mTotalScrollY + ",top:" + top + ",y:" + y);
                        if (mTotalScrollY <= 423) {
                            setTopAndBottomOffset(y);
                        }
                    }
                    // Post ourselves so that we run on the next animation
                    ViewCompat.postOnAnimation(mLayout, this);
                } else {
                    isFling = false;
                    int top = child.getTop();
                    Logd(TAG, "run: isFling = false," + ",top:" + top);
                    if (top != -423) {
                        Logd(TAG, "run: isFling = false" + ",mTotalScrollY:" + mTotalScrollY);
                        if (mTotalScrollY != 0) {
                            scrollTarget.scrollBy(0, -mTotalScrollY);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target,
                                 float velocityX, float velocityY, boolean consumed) {
        Logd(TAG, "onNestedFling: velocityX = [" + velocityX + "], velocityY = [" + velocityY + "], consumed = [" + consumed + "]");
        if (consumed) {
            if (velocityY < -VELOCITY_UNITS) {
                /*final int targetScroll = child.getTop();
                if (targetScroll != 0) {
                    int startY = mTotalScrollY > -targetScroll ? mTotalScrollY : mTotalScrollY -targetScroll;
                    fling(child, target, startY, targetScroll, 0, -velocityY, false);
                }*/
            } /*else if (velocityY > 0) {
                final int targetScroll = child.getTop();
                if (targetScroll != 423) {
                    fling(child, target, targetScroll, -targetScroll, 423, velocityY, true);
                }
            }*/
            // TODO: 2017/11/7  
        }
//        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
        return true;
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {
        Logd(TAG, "onNestedPreFling, velocityX = [" + velocityX + "], velocityY = [" + velocityY + "]");
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
        Logd(TAG, "onNestedPreScroll: dx:" + dx + ",dy:" + dy + ",consumed[0]" + consumed[0] + ",consumed[1]" + consumed[1]+",mTotalScrollY："+mTotalScrollY);
        // TODO: 2017/11/7
        isInterrupt = true;
        if (dy != 0 /*&& !mSkipNestedPreScroll*/) {
            int min, max;
            if (dy < 0) {
                // We're scrolling down
                min = 0;
                max = 423;
//                int top = child.getTop();
                if (mTotalScrollY > 423) {
                    return;
                }
                super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
//                dy = Math.abs(Math.max(-423, dy));
//                setTopAndBottomOffset(dy);
//                super.onNestedPreScroll();
            } else {
                // We're scrolling up
                min = -423;
                max = 0;
                dy = Math.max(-423, -dy);
                setTopAndBottomOffset(dy);
            }
            /*int top = child.getTop();
            if (dy == top) {
                return;
            }*/

        }
//                super.onNestedPreScroll(coordinatorLayout,child,target,dx,dy,consumed);

    }

    // TODO: 2017/11/3  dyConsumed是recyclerview的dy,
    boolean isInterrupt;

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        Logd(TAG, "onNestedScroll, dyConsumed = [" + dyConsumed + "]" + ", dyUnconsumed = [" + dyUnconsumed + "]");
        if (isFling) {//正在执行滚动动画时拦截
            return;
        }
        if (noHandle) {
            return;
        }
//        LogUtil.Logw(TAG, "onNestedScroll: mScrollY:" + dyUnconsumed);
//        LogUtil.Loge(TAG, "onNestedScroll: setTopAndBottomOffset");
        // TODO: 2017/11/7
       /* if (dyConsumed != 0) {//向上拖动时，调整appBarLayout的top
            setTopAndBottomOffset(dyConsumed + dyUnconsumed);
        } else if (dyUnconsumed != 0) {//向下拖动时，调整appBarLayout的top
            setTopAndBottomOffset(-dyUnconsumed);
        }*/

        if (dyUnconsumed != 0) {//向下拖动时，调整appBarLayout的top
//            setTopAndBottomOffset(-dyUnconsumed);
//            if (dyUnconsumed < 0) {
//                super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
//            }
        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        Logd(TAG, "onStartNestedScroll, onStartNestedScroll = [" + nestedScrollAxes + "]" + ",target:" + target);
        vScrollTarget = target;
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
    }

    protected void dispatchOffsetUpdates(AppBarLayout layout, int translationOffset) {
        if (layout instanceof SmoothAppBarLayout) {
            List listeners = ((SmoothAppBarLayout) layout).mOffsetChangedListeners;
            int i = 0;
            for (int z = listeners.size(); i < z; ++i) {
                WeakReference ref = (WeakReference) listeners.get(i);
                AppBarLayout.OnOffsetChangedListener listener = ref != null ? (AppBarLayout.OnOffsetChangedListener) ref.get() : null;
                if (listener != null) {
                    listener.onOffsetChanged(layout, translationOffset);
                }
            }
        }
    }

    private int lastScrollY;
    private boolean noHandle;

    @Override
    public void setCurrentScrollY(int scrollY) {
        lastScrollY = scrollY;
        mTotalScrollY = scrollY;
        int top = child.getTop();
        if (top != -423) {
            if (lastScrollY != 0) {
                boolean canScrollUp = Utils.canScrollUp(vScrollTarget);
                LogUtil.Loge(TAG, "setCurrentScrollY: canScrollUp:" + canScrollUp);
                if (!canScrollUp) {
                    noHandle = true;
                }
            }
        }
    }

    protected void syncOffset(int newOffset, int dy, final int mTotalScrollY) {
        Logd(TAG, "syncOffset: newOffset:" + newOffset + ",dy:" + dy
                + ",mTotalScrollY:" + mTotalScrollY);
        if (isFling) {
            LogUtil.Logi(TAG, "syncOffset: isFling");
            return;
        }
        // TODO: 2017/11/7  
        if (isInterrupt) {
            isInterrupt = false;
            return;
        }
        LogUtil.Loge(TAG, "syncOffset: noHandle:" + noHandle + ",lastScrollY:" + lastScrollY);
        if (lastScrollY != 0) {
            int top = child.getTop();
            if (top != -423) {
                if (mTotalScrollY <= lastScrollY) {
                    lastScrollY = mTotalScrollY;
                    noHandle = true;
                    return;
                } else {
                    newOffset = -mTotalScrollY + lastScrollY;
                    newOffset = newOffset * 2;
                    lastScrollY = mTotalScrollY;
                    noHandle = false;
                }
            } else {
                noHandle = false;
            }
        } else {
            noHandle = false;
        }
        LogUtil.Loge(TAG, "onNestedScroll: mScrollY:" + newOffset + ",dy:" + dy
                + ",mTotalScrollY:" + mTotalScrollY);
        setTopAndBottomOffset(newOffset);
    }

    @Override
    public boolean setTopAndBottomOffset(int offset) {
        if (mViewOffsetHelper != null) {
            return mViewOffsetHelper.setTopAndBottomOffset(offset);
        } else {
            mTempTopBottomOffset = offset;
        }
        return false;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, AppBarLayout child, int layoutDirection) {
        layoutChild(parent, child, layoutDirection);
        if (mViewOffsetHelper == null) {
            mViewOffsetHelper = new ViewOffsetHelper(child);
        }
        mViewOffsetHelper.onViewLayout();
        if (mTempTopBottomOffset != 0) {
            LogUtil.Loge(TAG, "onLayoutChild: ");
            mViewOffsetHelper.setTopAndBottomOffset(mTempTopBottomOffset);
            mTempTopBottomOffset = 0;
        }
        return true;
    }

    @Override
    protected void layoutChild(CoordinatorLayout parent, AppBarLayout child, int layoutDirection) {
        super.layoutChild(parent, child, layoutDirection);
        Logd(TAG, "layoutChild: viewPager.getMeasureHeight=1743:1920-239dp+432(maxScrollOffset)+36dp");
    }

    public final class ViewOffsetHelper {
        private final View mView;
        private int mLayoutTop;
        private int mOffsetTop;
        private int curOffset;

        public ViewOffsetHelper(View view) {
            mView = view;
        }

        public void onViewLayout() {
            // Now grab the intended top
            Log.d(TAG, "onViewLayout: ");
            mLayoutTop = mView.getTop();
            // And offset it as needed
            updateOffsets();
        }

        private void updateOffsets() {
            int offset = mOffsetTop;
            int top = mView.getTop();
            LogUtil.Logw(TAG, "updateOffsets: top:" + top + ",mLayoutTop:" + mLayoutTop + ",offset:" + offset);
            int newOffset;
            if (top + offset > 0) {
                offsetTopAndBottom(mView, -top);
                newOffset = 0;
            } else if (top == -423) {
                if (offset > 0) {
                    offsetTopAndBottom(mView, offset);
                    newOffset = top + offset;
                } else {
                    newOffset = -423;
                }
            } else if (top + offset < -423) {
                offset = -423 - top;
                offsetTopAndBottom(mView, offset);
                newOffset = -423;
            } else {
                offsetTopAndBottom(mView, offset);
                newOffset = top + offset;
            }
            mCurrentOffset = newOffset;
            LogUtil.Loge(TAG, "updateOffsets: newOffset:" + newOffset + ",mTotalScrollY:" + mTotalScrollY);
            if (newOffset != curOffset) {
                curOffset = newOffset;
                dispatchOffsetUpdates(child, newOffset);
            }
        }

        public boolean setTopAndBottomOffset(int offset) {
            mOffsetTop = offset;
            updateOffsets();
            return true;
        }
    }
}