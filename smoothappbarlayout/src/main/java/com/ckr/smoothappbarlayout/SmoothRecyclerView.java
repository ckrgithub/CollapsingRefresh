package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.ckr.smoothappbarlayout.listener.OnFlingListener;
import com.ckr.smoothappbarlayout.listener.OnSmoothScrollListener;

import static com.ckr.smoothappbarlayout.utils.LogUtil.Logd;
import static com.ckr.smoothappbarlayout.utils.LogUtil.Loge;
import static com.ckr.smoothappbarlayout.utils.LogUtil.Logw;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class SmoothRecyclerView extends RecyclerView implements OnFlingListener {
	private static final String TAG = "SmoothRecyclerView";
	private static final String ARG_SCROLL_DISTANCE = "arg_scroll_distance";
	private static final String ARG_SUPER = "arg_super";
	private static final int VELOCITY_UNITS = 1000;//1000 provides pixels per second
	private int mTouchSlop;
	private int mTotalScrollY;
	private int mScrollState;
	private boolean mIsBeingDragged;
	private boolean isInterruptFling;
	private boolean forwardDirection;//滑动方向

	// A context-specific coefficient adjusted to physical values.
	private float mPhysicalCoeff;
	private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
	private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
	private float mFlingFriction = ViewConfiguration.getScrollFriction();
	public double mTotalFlingDistance;
	public double mDiffFlingDistance;

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
		final ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
		final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
		mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
				* 39.37f // inch/meter
				* ppi
				* 0.84f; // look and feel tuning
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		Logd(TAG, "onInterceptTouchEvent: action:" + action + ",mIsBeingDragged:" + mIsBeingDragged);
		// Shortcut since we're being dragged
		if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
			return true;
		}
		ensureVelocityTracker();
		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(ev);
		}
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				mIsBeingDragged = false;
				mLastMotionY = (int) (ev.getRawY() + 0.5f);
				mActivePointerId = ev.getPointerId(0);
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				if (pointerIndex == -1) {
					break;
				}
				mLastMotionY = (int) (ev.getRawY() + 0.5f);
				mIsBeingDragged = true;
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				mIsBeingDragged = false;
				resetTouch();
				break;
			}
		}
		Logd(TAG, "onInterceptTouchEvent: end  mLastMotionY:" + mLastMotionY + ",mIsBeingDragged:" + mIsBeingDragged);
		return super.onInterceptTouchEvent(ev);
	}

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
					+ ",abs:" + abs + ",mTotalScrollY：" + mTotalScrollY);
			if (state > 0) {
				if (mScrollState != SCROLL_STATE_SETTLING) {
					switch (action) {
						case MotionEvent.ACTION_DOWN:
							mActivePointerId = e.getPointerId(0);
							mLastMotionY = (int) (e.getRawY() + 0.5f);
							break;
						case MotionEvent.ACTION_MOVE:
							int index = e.findPointerIndex(mActivePointerId);
							if (index == -1) {
								return false;
							}
							int y = (int) (e.getRawY() + 0.5f);
							int dy = mLastMotionY - y;
							if (dy > 0) {
								forwardDirection = true;
							} else {
								forwardDirection = false;
							}
							if (!mIsBeingDragged) {
								mIsBeingDragged = true;
							}
							if (mIsBeingDragged) {
								mLastMotionY = y;
								if (state == 2) {
									if (!forwardDirection) {
										Loge(TAG, "onTouchEvent: onScrollChanged  state:" + state + ",dy:" + dy);
										isInterruptFling = true;
										if (mTotalScrollY == 0) {
											mSmoothScrollListener.onScrollChanged(this,
													computeHorizontalScrollOffset(), 0,
													0, dy,
													getLayoutManager().findViewByPosition(0) != null);
										}
									}
								} else {
									if (mTotalScrollY == 0) {
										Loge(TAG, "onTouchEvent: onScrollChanged  state:" + state + ",dy:" + dy);
										isInterruptFling = true;
										mSmoothScrollListener.onScrollChanged(this,
												computeHorizontalScrollOffset(), 0,
												0, dy,
												getLayoutManager().findViewByPosition(0) != null);
									}
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
								if (mVelocityTracker != null) {
									eventAddedToVelocityTracker = true;
									mVelocityTracker.addMovement(e);
									int minFlingVelocity = getMinFlingVelocity();
									int maxFlingVelocity = getMaxFlingVelocity();
									mVelocityTracker.computeCurrentVelocity(4000, maxFlingVelocity);
									float yvel = -mVelocityTracker.getYVelocity(mActivePointerId);
									float absY = Math.abs(yvel);
									boolean fling = (absY > minFlingVelocity);
									Logd(TAG, "onTouchEvent: fling: minFlingVelocity:" + minFlingVelocity + ",maxFlingVelocity:" + maxFlingVelocity + ",yvel：" + yvel);
									if (fling) {
										yvel = forwardDirection ? Math.abs(yvel) : -Math.abs(yvel);
										if (forwardDirection) {
											mTotalFlingDistance = getSplineFlingDistance((int) yvel);
											mDiffFlingDistance = mTotalFlingDistance - currentOffset - totalScrollRange;
//                                        int flingDuration = getSplineFlingDuration((int) yvel);
											Logd(TAG, "onTouchEvent: fling: forwardDirection:" + forwardDirection + ",yvel：" + yvel
													+ ",mTotalFlingDistance:" + mTotalFlingDistance + ",mDiffFlingDistance:" + mDiffFlingDistance);
										}
										mSmoothScrollListener.onFlingFinished(this, yvel);
									}
								}
							}
							resetTouch();
							break;
					}

					if (isInterruptFling) {
						if (!eventAddedToVelocityTracker && mVelocityTracker != null) {
							mVelocityTracker.addMovement(e);
						}
						return true;
					}
				}
			}
		}
		if (!eventAddedToVelocityTracker && mVelocityTracker != null) {
			mVelocityTracker.addMovement(e);
		}
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
	private int getSplineFlingDuration(int velocity) {
		final double l = getSplineDeceleration(velocity);
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		return (int) (1000.0 * Math.exp(l / decelMinusOne));
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

	private void ensureVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	public void setCurrentScrollY() {
		if (mSmoothScrollListener != null) {
			mSmoothScrollListener.setCurrentScrollY(mTotalScrollY);
		}
	}

	public void setOnSmoothScrollListener(OnSmoothScrollListener listener) {
		this.mSmoothScrollListener = listener;
		listener.setOnFlingListener(this);
	}

	@Override
	public void onFlingFinished(float velocityY, int dy, View target) {
		Logd(TAG, "onFlingFinished: fling: velocityY:" + velocityY);
		if (mSmoothScrollListener != null) {
			int currentOffset = mSmoothScrollListener.getCurrentOffset();
			int totalScrollRange = mSmoothScrollListener.getTotalRange();
			Logd(TAG, "onFlingFinished: fling:  currentOffset:" + currentOffset + ",mDiffFlingDistance:" + mDiffFlingDistance);
			if (velocityY > 0 && currentOffset == -totalScrollRange) {
				if (target instanceof RecyclerView) {
					SmoothRecyclerView recyclerView = (SmoothRecyclerView) target;
					double fDistance = recyclerView.mDiffFlingDistance;
					double flingDistance = recyclerView.mTotalFlingDistance;
					Log.d(TAG, "onFlingFinished: fling:  fDist:" + fDistance + ",mTotalFlingDistance:" + flingDistance);
					int flingY = (int) (velocityY * Math.min(fDistance, flingDistance * 3 / 4f) / flingDistance);
					int subVelocity = getVelocityWithDistance(fDistance);
					int subVelocity2 = getVelocityWithDistance(flingDistance - fDistance);
					int sumVelocity = getVelocityWithDistance(flingDistance);
					Logd(TAG, "onFlingFinished: fling:  subVelocity:" + subVelocity + ",subV2:" + subVelocity2 + ",sumVelocity:" + sumVelocity);
					boolean b = Math.abs(flingY) > 0;
					if (b) {
						recyclerView.fling(0, (int) subVelocity);
						Logd(TAG, "onFlingFinished: fling: ,flingY:" + flingY);
						mScrollState = SCROLL_STATE_IDLE;
					}
				}
			}
		}
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
		Logd(TAG, "onScrolled() called with: mTotalScrollY = [" + mTotalScrollY + "], dy = [" + dy + "]");
		if (mSmoothScrollListener != null) {
			mSmoothScrollListener.onScrollValueChanged(this, mTotalScrollY, false);
		}
		if (mTotalScrollY==0&&dy<0) {
			if (mSmoothScrollListener != null) {
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
