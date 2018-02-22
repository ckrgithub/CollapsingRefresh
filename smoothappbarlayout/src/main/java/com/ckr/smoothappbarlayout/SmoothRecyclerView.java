package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.ckr.smoothappbarlayout.base.OnSmoothScrollListener;

import static com.ckr.smoothappbarlayout.base.LogUtil.Logd;
import static com.ckr.smoothappbarlayout.base.LogUtil.Loge;
import static com.ckr.smoothappbarlayout.base.LogUtil.Logw;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class SmoothRecyclerView extends RecyclerView {
    private static final String TAG = "SmoothRecyclerView";
    private static final String ARG_CURRENT_SCROLLY = "arg_current_scroll_y";
    private static final String ARG_SUPER = "arg_super";
    private static final int EXTRA_VELOCITY_Y = 2000;//2000 to lift speed
    private static final int VELOCITY_UNITS = 1000;//1000 provides pixels per second
    private int mTotalScrollY;
    private OnSmoothScrollListener listener;
    private int mLastY;
    private int mTouchSlop;
    private int mScrollState;
    private int mPointId;
    private boolean isInterrupt;

    public SmoothRecyclerView(Context context) {
        this(context, null);
    }

    public SmoothRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmoothRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
    }

    private boolean forwardDirection;//滑动方向

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Logd(TAG, "onTouchEvent: mScrollState:"+mScrollState);
        isInterrupt = false;
        MotionEvent motionEvent = MotionEvent.obtain(e);
        int action = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        if (listener != null) {
            int currentOffset = listener.getCurrentOffset();
            int abs = Math.abs(currentOffset);
            int state = 0;
            if (abs < 423) {
                state = 1;
            } else {
                boolean canScrollDown = canScrollVertically(-1);
                Logw(TAG, "onTouchEvent: canScrollDown:" + canScrollDown);
                if ((abs == 423 && !canScrollDown)) {
                    state = 2;
                }
            }
            Logd(TAG, "onTouchEvent: state:" + state + ",abs:" + abs + ",mTotalScrollY：" + mTotalScrollY);
            if (state > 0) {
                if (mScrollState != SCROLL_STATE_SETTLING) {
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            isInterrupt = true;
                            mPointId = e.getPointerId(0);
                            mLastY = (int) (e.getRawY() + 0.5f);
                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            isInterrupt = true;
                            mPointId = e.getPointerId(actionIndex);
                            mLastY = (int) (e.getRawY() + 0.5f);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            int index = e.findPointerIndex(mPointId);
                            if (index < 0) {
                                return false;
                            }
                            int y = (int) (e.getRawY() + 0.5f);
                            int dy = mLastY - y;
                            if (dy > 0) {
                                forwardDirection = true;
                            } else {
                                forwardDirection = false;
                            }
                            mLastY = y;
                            if (state == 2) {
                                if (!forwardDirection) {
                                    Loge(TAG, "onTouchEvent: isInterrupt  canScrollDown ,拦截"+ ",dy:" + dy+",isFling:"+isFling);
                                    isInterrupt = true;
                                    if (mTotalScrollY == 0) {
                                        listener.onScrollChanged(this,
                                                computeHorizontalScrollOffset(), 0,
                                                0, dy,
                                                getLayoutManager().findViewByPosition(0) != null);
                                    }
                                }
                            } else {
                                if (mTotalScrollY == 0) {
                                Loge(TAG, "onTouchEvent: isInterrupt = true,拦截"+ ",dy:" + dy+",isFling:"+isFling);
                                    isInterrupt = true;
                                    listener.onScrollChanged(this,
                                            computeHorizontalScrollOffset(), 0,
                                            0, dy,
                                            getLayoutManager().findViewByPosition(0) != null);
                                } else {

                                }
                            }
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            isInterrupt = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            isInterrupt = true;
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            isInterrupt = true;
                            onPointerUp(e);
                            break;
                    }
                    if (isInterrupt) {
                        return true;
                    }

                }

            }

        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPointId = e.getPointerId(0);
                mLastY = (int) (e.getRawY() + 0.5f);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mPointId = e.getPointerId(actionIndex);
                mLastY = (int) (e.getRawY() + 0.5f);
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = (int) (e.getRawY() + 0.5f);
                break;
        }
        return super.onTouchEvent(e);
    }

    private void onPointerUp(MotionEvent e) {
        MotionEvent motionEvent = MotionEvent.obtain(e);
        int actionIndex = motionEvent.getActionIndex();
        if (e.getPointerId(actionIndex) == mPointId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mPointId = e.getPointerId(newIndex);
            mLastY = (int) (e.getRawY() + 0.5f);
        }
    }

    public void setCurrentScrollY() {
        if (listener != null) {
            listener.setCurrentScrollY(mTotalScrollY);
        }
    }

    public void setOnSmoothScrollListener(OnSmoothScrollListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mTotalScrollY = bundle.getInt(ARG_CURRENT_SCROLLY);
        Parcelable superState = bundle.getParcelable(ARG_SUPER);
        super.onRestoreInstanceState(superState);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_CURRENT_SCROLLY, mTotalScrollY);
        bundle.putParcelable(ARG_SUPER, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    public void onScrollStateChanged(int state) {
//		super.onScrollStateChanged(state);
//		SCROLL_STATE_IDLE://0
//		SCROLL_STATE_DRAGGING://1
//		SCROLL_STATE_SETTLING://2
        mScrollState = state;
        switch (state) {
            case SCROLL_STATE_IDLE:
                isFling = false;
                break;
            case SCROLL_STATE_SETTLING:
                isFling = true;
                break;
            case SCROLL_STATE_DRAGGING:
                break;
        }
        Logd(TAG, "onScrollChanged: mScrollState:" + mScrollState);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        Logd(TAG, "onScrolled() called with: mTotalScrollY = [" + mTotalScrollY + "], dy = [" + dy + "]" + ",isFling:" + isFling);
        mTotalScrollY += dy;
        if (mTotalScrollY==0) {
            if (listener != null) {
                listener.handleFling();
            }
        }
    }

    boolean isFling;

    @Override
    public boolean fling(int velocityX, int velocityY) {
        int minFlingVelocity = getMinFlingVelocity();
        boolean fling = (Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity);
        if (!fling) {
            Logd(TAG, "fling: 拦截:" + minFlingVelocity);
            return true;
        }
        Loge(TAG, "fling onScrollChanged, velocityX = [" + velocityX + "], velocityY = [" + velocityY + "]" + ",mTotalScrollY：" + mTotalScrollY);
        //make more smooth
        if (velocityY > 0) {
            return super.fling(velocityX, velocityY);
        } else {
            if (listener != null) {
                if (isInterrupt) {
                    Loge(TAG, "fling,onTouchEvent isInterrupt = true,isFling:"+isFling);
                    return true;
                } else {
                    Loge(TAG, "fling,onTouchEvent isInterrupt = false,isFling:"+isFling);
                }
            }
            return super.fling(velocityX, velocityY);
        }
    }
}
