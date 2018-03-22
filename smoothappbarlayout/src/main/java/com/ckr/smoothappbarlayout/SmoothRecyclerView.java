package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.ckr.smoothappbarlayout.listener.OnFlingListener;
import com.ckr.smoothappbarlayout.listener.OnSmoothScrollListener;

import static com.scwang.smartrefresh.util.LogUtil.Logd;
import static com.scwang.smartrefresh.util.LogUtil.Loge;
import static com.scwang.smartrefresh.util.LogUtil.Logw;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class SmoothRecyclerView extends RecyclerView implements OnFlingListener {
	private static final String TAG = "SmoothRecyclerView";
	private static final String ARG_SCROLL_DISTANCE = "arg_scroll_distance";
	private static final String ARG_SUPER = "arg_super";
	private static final int VELOCITY_UNITS = 4000;//4000 provides pixels per second
	private int mTotalScrollY;
	private int mScrollState;
	private boolean mIsBeingDragged;
	private boolean isInterruptFling;//拦截touch事件
	private boolean forwardDirection;//滑动方向

	// A context-specific coefficient adjusted to physical values.
	private float mPhysicalCoeff;
	private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
	private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
	private float mFlingFriction = ViewConfiguration.getScrollFriction();
	private double mTotalFlingDistance;
	private double mDiffFlingDistance;

	private OnSmoothScrollListener mSmoothScrollListener;
	private VelocityTracker mVelocityTracker;
	private boolean eventAddedToVelocityTracker;
	private int mActivePointerId = -1;
	private int mLastMotionY;

	public SmoothRecyclerView(Context context) {
		this(context, null);
	}

	public SmoothRecyclerView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SmoothRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
		mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
				* 39.37f // inch/meter
				* ppi
				* 0.84f; // look and feel tuning
	}

	/**
	 * see to {@link android.support.design.widget.HeaderBehavior}
	 *
	 * @param ev
	 * @return
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getActionMasked();
		Logd(TAG, "onInterceptTouchEvent: action:" + action + ",mIsBeingDragged:" + mIsBeingDragged);
		// Shortcut since we're being dragged
		if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
			return true;
		}
		mSmoothScrollListener.setScrollTarget(this);
		ensureVelocityTracker();
		addEventToVelocityTracker(ev);
		switch (action) {
			case MotionEvent.ACTION_DOWN: {
				mIsBeingDragged = false;
				mLastMotionY = getRawY(ev);
				mActivePointerId = ev.getPointerId(0);
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				if (pointerIndex == -1) {
					break;
				}
				mLastMotionY = getRawY(ev);
				mIsBeingDragged = true;
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				mActivePointerId = -1;
				mIsBeingDragged = false;
				resetTouch();
				break;
			}
		}
		Logd(TAG, "onInterceptTouchEvent: end  mLastMotionY:" + mLastMotionY + ",mIsBeingDragged:" + mIsBeingDragged);
		return super.onInterceptTouchEvent(ev);
	}

	private int getRawY(MotionEvent ev) {
		return (int) (ev.getRawY() + 0.5f);
	}

	private void addEventToVelocityTracker(MotionEvent ev) {
		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(ev);
		}
	}

	private void ensureVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	/**
	 * see to {@link android.support.design.widget.HeaderBehavior}
	 *
	 * @param e
	 * @return
	 */
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		isInterruptFling = false;
		eventAddedToVelocityTracker = false;
		int action = e.getActionMasked();
		ensureVelocityTracker();
		if (mSmoothScrollListener != null) {
			int currentOffset = mSmoothScrollListener.getCurrentOffset();
			int totalScrollRange = mSmoothScrollListener.getTotalRange();
			int abs = Math.abs(currentOffset);
			int state = 0;
			if (abs < totalScrollRange) {
				state = 1;
			} else {
				boolean canScrollDown = canScrollVertically(-1);
				Logw(TAG, "onTouchEvent: canScrollDown:" + canScrollDown);
				if ((abs == totalScrollRange && !canScrollDown)) {
					state = 2;
				}
			}
			Logd(TAG, "onTouchEvent: action:" + action + ",state:" + state + ", mScrollState:" + mScrollState
					+ ",abs:" + abs + ",totalScrollRange:" + totalScrollRange + ",mTotalScrollY：" + mTotalScrollY);
			if (state > 0) {
				if (mScrollState != SCROLL_STATE_SETTLING) {
					switch (action) {
						case MotionEvent.ACTION_DOWN:
							mActivePointerId = e.getPointerId(0);
							mLastMotionY = getRawY(e);
							break;
						case MotionEvent.ACTION_MOVE:
							int y = getRawY(e);
							int dy = (mLastMotionY - y);
							if (dy > 0) {
								forwardDirection = true;
							} else {
								forwardDirection = false;
							}
							mLastMotionY = y;
							if (state == 2) {
								if (!forwardDirection) {
									isInterruptFling = true;
									Loge(TAG, "onTouchEvent: onScrollChanged  state:" + state + ",dy:" + dy + ",currentOffset:" + currentOffset);
									if (mTotalScrollY == 0) {
										mSmoothScrollListener.onScrollChanged(this, 0, -dy);
									}
								}
							} else {
								if (mTotalScrollY == 0) {
									Loge(TAG, "onTouchEvent: onScrollChanged  state:" + state + ",dy:" + dy + ",currentOffset:" + currentOffset);
									isInterruptFling = true;
									mSmoothScrollListener.onScrollChanged(this, 0, -dy);
								}
							}
							break;
						case MotionEvent.ACTION_CANCEL:
							isInterruptFling = true;
							mIsBeingDragged = false;
							mActivePointerId = -1;
							resetTouch();
							break;
						case MotionEvent.ACTION_UP:
							Logd(TAG, "onTouchEvent: fling: ACTION_UP:" + currentOffset + ",forwardDirection：" + forwardDirection);
							if (!forwardDirection && currentOffset == 0) {
							} else if (mTotalScrollY == 0) {
								isInterruptFling = true;
								eventAddedToVelocityTracker = true;
								addEventToVelocityTracker(e);
								int minFlingVelocity = getMinFlingVelocity();
								int maxFlingVelocity = getMaxFlingVelocity();
								mVelocityTracker.computeCurrentVelocity(VELOCITY_UNITS, maxFlingVelocity);
								float yvel = -mVelocityTracker.getYVelocity(mActivePointerId);
								float absY = Math.abs(yvel);
								boolean fling = (absY > minFlingVelocity);
								Logd(TAG, "onTouchEvent: fling: minFlingVelocity:" + minFlingVelocity + ",maxFlingVelocity:" + maxFlingVelocity + ",yvel：" + yvel);
								if (fling) {
									yvel = forwardDirection ? Math.abs(yvel) : -Math.abs(yvel);
									if (forwardDirection) {
										mTotalFlingDistance = getSplineFlingDistance((int) yvel);
										mDiffFlingDistance = mTotalFlingDistance - currentOffset - totalScrollRange;
										Logd(TAG, "onTouchEvent: fling: forwardDirection:" + forwardDirection + ",yvel：" + yvel
												+ ",mTotalFlingDistance:" + mTotalFlingDistance + ",mDiffFlingDistance:" + mDiffFlingDistance);
									}
									mSmoothScrollListener.onStartFling(this, yvel);
								}
							}
							resetTouch();
							break;
					}
					if (isInterruptFling) {
						if (!eventAddedToVelocityTracker) {
							addEventToVelocityTracker(e);
						}

						return true;
					}
				}
			}
		}
		addEventToVelocityTracker(e);
		return super.onTouchEvent(e);
	}

	/**
	 * {@link android.widget.OverScroller}
	 *
	 * @param velocity
	 * @return
	 */
	private double getSplineFlingDistance(int velocity) {
		final double l = getSplineDeceleration(velocity);
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
	}

	/**
	 * {@link android.widget.OverScroller}
	 *
	 * @param velocity
	 * @return
	 */
	private double getSplineDeceleration(int velocity) {
		return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
	}

	/**
	 * {@link android.widget.OverScroller}
	 *
	 * @param distance
	 * @return
	 */
	private int getVelocityWithDistance(double distance) {
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		double l = Math.log(distance / (mFlingFriction * mPhysicalCoeff)) * decelMinusOne / DECELERATION_RATE;
		int velocity = (int) (Math.exp(l) * (mFlingFriction * mPhysicalCoeff) / INFLEXION);
		return velocity;
	}

	private void resetTouch() {
		if (mVelocityTracker != null) {
			mVelocityTracker.clear();
		}
	}

	public void setOnSmoothScrollListener(OnSmoothScrollListener listener) {
		this.mSmoothScrollListener = listener;
	}

	@Override
	public void onStartFling(View target, float velocityY) {
		Logd(TAG, "onStartFling: fling: velocityY:" + velocityY);
		if (mSmoothScrollListener != null) {
			int currentOffset = mSmoothScrollListener.getCurrentOffset();
			int totalScrollRange = mSmoothScrollListener.getTotalRange();
			Logd(TAG, "onStartFling: fling:  currentOffset:" + currentOffset + ",mDiffFlingDistance:" + mDiffFlingDistance);
			if (velocityY > 0 && currentOffset == -totalScrollRange) {
					double fDistance = mDiffFlingDistance;
					double flingDistance = mTotalFlingDistance;
					Logd(TAG, "onStartFling: fling:  fDist:" + fDistance + ",mTotalFlingDistance:" + flingDistance);
					int subVelocity = getVelocityWithDistance(fDistance);
					boolean b = Math.abs(subVelocity) > 0;
					if (b) {
						fling(0, (int) subVelocity);
						mScrollState = SCROLL_STATE_IDLE;
					}
			}
		}
	}

	@Override
	public void onDispatchFling(View view, int mScrollState) {

	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		mTotalScrollY = bundle.getInt(ARG_SCROLL_DISTANCE);
		Parcelable superState = bundle.getParcelable(ARG_SUPER);
		super.onRestoreInstanceState(superState);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putInt(ARG_SCROLL_DISTANCE, mTotalScrollY);
		bundle.putParcelable(ARG_SUPER, super.onSaveInstanceState());
		return bundle;
	}

	@Override
	public void onScrollStateChanged(int state) {
		mScrollState = state;
		Logd(TAG, "onScrollChanged: mScrollState:" + mScrollState);
	}

	@Override
	public void onScrolled(int dx, int dy) {
		super.onScrolled(dx, dy);
		mTotalScrollY += dy;
		Logd(TAG, "onScrolled() NestedScrollingParent: mTotalScrollY = [" + mTotalScrollY + "], dy = [" + dy + "]");
		if (dy > 0) {
			forwardDirection = true;
		} else {
			forwardDirection = false;
		}
		if (mSmoothScrollListener != null) {
			mSmoothScrollListener.onScrollValueChanged(this, mTotalScrollY, false);
		}
		if (mTotalScrollY == 0 && dy < 0) {
			if (mSmoothScrollListener != null) {
				Logd(TAG, "fling:: startDispatch");
				mSmoothScrollListener.onDispatchFling(this, SCROLL_STATE_IDLE);
			}
		}
	}

	@Override
	public boolean fling(int velocityX, int velocityY) {
		Loge(TAG, "fling: velocityX = [" + velocityX + "], velocityY:" + velocityY + "]" + ",mTotalScrollY：" + mTotalScrollY);
		if (mSmoothScrollListener != null) {
			mSmoothScrollListener.onScrollValueChanged(this, mTotalScrollY, true);
		}
		if (velocityY > 0) {
			return super.fling(velocityX, velocityY);
		} else {
			if (mSmoothScrollListener != null) {
				if (isInterruptFling) {
					Loge(TAG, "fling:  onTouchEvent isInterruptFling = true,");
					return true;
				} else {
					Loge(TAG, "fling:  onTouchEvent isInterruptFling = false,");
				}
			}
			return super.fling(velocityX, velocityY);
		}
	}
}
