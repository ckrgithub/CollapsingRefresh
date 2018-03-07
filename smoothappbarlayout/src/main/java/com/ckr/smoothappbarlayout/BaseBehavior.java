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
import android.hardware.SensorManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.ckr.smoothappbarlayout.base.LogUtil;
import com.ckr.smoothappbarlayout.base.OnFlingCallBack;
import com.ckr.smoothappbarlayout.base.OnSmoothScrollListener;
import com.ckr.smoothappbarlayout.base.Utils;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.support.v4.view.ViewCompat.offsetTopAndBottom;
import static com.ckr.smoothappbarlayout.base.LogUtil.Logd;
import static com.ckr.smoothappbarlayout.base.LogUtil.Loge;
import static com.ckr.smoothappbarlayout.base.LogUtil.Logw;

/**
 * Created by PC大佬 on 2018/2/9.
 */
public abstract class BaseBehavior extends AppBarLayout.Behavior implements OnSmoothScrollListener {
	private static final String TAG = "BaseBehavior";
	protected AppBarLayout mAppBarLayout;
	private DragCallback mDragCallbackListener;
	private boolean mIsOnInit = false;
	protected View mScrollTarget;
	private Runnable mFlingRunnable;
	private OverScroller mScroller;
	protected int mTotalScrollY;
	private boolean isFling;
	private static final int VELOCITY_UNITS = 1000;//1000 provides pixels per second

	private ViewOffsetHelper mViewOffsetHelper;
	private int mTempTopBottomOffset = 0;
	protected int mCurrentOffset;
	private float velocityY;
	protected OnFlingCallBack callBack;
	private float mPhysicalCoeff;
	private double flingDistance;

	private boolean autoScroll;
	boolean isInterrupt;
	boolean onStop;
	public int flagScrollY;
	private int lastScrollY;

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
		this.mAppBarLayout = child;
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
		final float ppi = child.getResources().getDisplayMetrics().density * 160.0f;
		mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
				* 39.37f // inch/meter
				* ppi
				* 0.84f; // look and feel tuning
	}

	static final Interpolator sQuinticInterpolator = new Interpolator() {
		@Override
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};

	/**
	 * {@link android.support.design.widget.HeaderBehavior}中fling()
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
		Logd(TAG, "fling: run: startY:" + startY + ",minOffset:" + minOffset + ",maxOffset:" + maxOffset
				+ ",velocityY:" + velocityY + ",mCurrentOffset:" + mCurrentOffset + ",mTotalScrollY:" + mTotalScrollY);
		if (mFlingRunnable != null) {
			layout.removeCallbacks(mFlingRunnable);
			mFlingRunnable = null;
		}
		if (mScroller == null) {
			mScroller = new OverScroller(layout.getContext(), sQuinticInterpolator);
		}
		Log.d(TAG, "fling: getTop:" + mAppBarLayout.getTop() + ",bottom:" + mAppBarLayout.getBottom());
		// TODO: 2018/2/22
		mScroller.fling(0, 0, 0, Math.round(velocityY)
				, 0, 0, -423, 423);
		boolean canScroll = mScroller.computeScrollOffset();
		Logd(TAG, "fling: run: canScroller: " + canScroll);
		if (canScroll) {
			mFlingRunnable = new FlingRunnable(layout, target, velocityY < 0 ? true : false, flingUp, velocityY);
			ViewCompat.postOnAnimation(layout, mFlingRunnable);
			return true;
		} else {
			return false;
		}
	}


	/**
	 * {@link AppBarLayout.Behavior}中onStopNestedScroll,onFling
	 * {@link android.support.design.widget.HeaderBehavior}中onInterceptTouchEvent,fling
	 */
	private class FlingRunnable implements Runnable {
		private final AppBarLayout mLayout;
		private final View scrollTarget;
		private final boolean isFlingUp;
		private final boolean accuracy;
		private float velocityY;
		private boolean isInterrupt;
		private int mLastY = 0;

		FlingRunnable(AppBarLayout layout, View target, boolean flingUp, boolean accuracy, float velocityY) {
			mLayout = layout;
			scrollTarget = target;
			isFlingUp = flingUp;
			this.accuracy = accuracy;
			this.mLastY = 0;
			isInterrupt = false;
			this.velocityY = velocityY;
		}

		@Override
		public void run() {
			if (mLayout != null && mScroller != null && mScrollTarget == scrollTarget) {
				final OverScroller mScroller = BaseBehavior.this.mScroller;
				if (mScroller.computeScrollOffset()) {
					isFling = true;
					int currVelocity = (int) mScroller.getCurrVelocity();
					int currY = mScroller.getCurrY();
					int finalY = mScroller.getFinalY();
					int dy = finalY - currY;
					int y = mLastY - currY;//-7.-11,8,33
					mLastY = currY;
					Loge(TAG, "run: fling: currY:" + currY + ",y:" + y + ",finalY:" + finalY + ",dy:" + dy + ",currVelocity:" + currVelocity);
					if (mCurrentOffset == -423 && callBack != null && !autoScroll && velocityY > 0) {
						autoScroll = true;
						callBack.onFlingFinished(velocityY, 0, scrollTarget);
					} else {
						if (y != 0) {
							setTopAndBottomOffset(y);
						} else {
						}
					}
					ViewCompat.postOnAnimation(mLayout, this);
				} else {
					Log.d(TAG, "run: fling:  isFling=false");
					isFling = false;
					autoScroll = false;
				}
			}
		}
	}

	@Override
	public boolean onNestedFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target,
								 float velocityX, float velocityY, boolean consumed) {
		Loge(TAG, "NestedScrollingParent,onNestedFling: fling: [" + velocityX + "], velocityY = [" + velocityY + "], consumed = [" + consumed + "]"
				+ "，mTotalScrollY：" + mTotalScrollY);
		if (consumed) {
			if (velocityY < 0) {
				this.velocityY = velocityY;
			}
		}
		return true;
	}

	@Override
	public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {
		Logd(TAG, "NestedScrollingParent,onNestedPreFling, fling = [" + velocityX + "], velocityY = [" + velocityY + "]");
		return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
	}

	@Override
	public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
		Logd(TAG, "NestedScrollingParent,onStartNestedScroll, onStartNestedScroll = [" + nestedScrollAxes + "]" + ",autoScroll:" + autoScroll);
		mScrollTarget = target;
		return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
	}

	@Override
	public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
		Logw(TAG, "NestedScrollingParent,onNestedPreScroll: dx:" + dx + ",dy:" + dy + ",consumed[0]" + consumed[0] + ",consumed[1]" + consumed[1] + ",mTotalScrollY：" + mTotalScrollY);
		// TODO: 2017/11/7
		isInterrupt = true;
		if (dy != 0 ) {
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
		}
	}


	@Override
	public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
		Loge(TAG, "NestedScrollingParent,onNestedScroll, dyConsumed = [" + dyConsumed + "]" + ", dyUnconsumed = [" + dyUnconsumed + "]" + ",type0:" + type);
		if (isFling) {//正在执行滚动动画时拦截
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
		if (dyConsumed == 0) {
			onStop = true;
			if (velocityY == 0) {
				return;
			}
			float velocityY = this.velocityY;
			this.velocityY = 0;
			flingDistance = getSplineFlingDistance((int) velocityY);
			int flingDuration = getSplineFlingDuration((int) velocityY);
			Logd(TAG, "NestedScrollingParent  fling: velocityY =" + velocityY + ",flagScrollY:" + flagScrollY
					+ ",flingDistance:" + flingDistance + ",flingDuration:" + flingDuration);
			/*if (target instanceof RecyclerView) {
				RecyclerView recyclerView = (RecyclerView) target;
				int scrollExtent = recyclerView.computeVerticalScrollExtent();
				int scrollOffset = recyclerView.computeVerticalScrollOffset();
				int scrollRange = recyclerView.computeVerticalScrollRange();
				Log.d(TAG, "onFling: fling:  scrollExtent:" + scrollExtent + ",scrollOffset:" + scrollOffset + ",scrollRange:" + scrollRange);
			}*/
//			float flingY = (float) (getVelocityWithDistance(flingDistance - flagScrollY)*velocityY/Math.abs(velocityY));
			float flingY = (float) ((flingDistance - flagScrollY) * velocityY / flingDistance);
			int subVelocity = getVelocityWithDistance(flagScrollY);
			int subVelocity2 = getVelocityWithDistance(flingDistance - flagScrollY);
			int sumVelocity = getVelocityWithDistance(flingDistance);
			Logd(TAG, "onFling: fling:  subVelocity:" + subVelocity + ",subV2:" + subVelocity2 + ",sumVelocity:" + sumVelocity);

			Logd(TAG, "onNestedScroll: fling:  flingY:" + flingY);
			fling(child, mScrollTarget, 423, 0, 0
					, -Math.abs(subVelocity2), true);
		} else {
			onStop = false;
		}
	}

	/**
	 * {@link android.widget.OverScroller}中getSplineFlingDistance
	 *
	 * @param velocity
	 * @return
	 */
	private double getSplineFlingDistance(int velocity) {
		final double l = getSplineDeceleration(velocity);
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
	}

	private int getVelocityWithDistance(double distance) {

		final double decelMinusOne = DECELERATION_RATE - 1.0;
		double l = Math.log(distance / (mFlingFriction * mPhysicalCoeff)) * decelMinusOne / DECELERATION_RATE;
		int velocity = (int) (Math.exp(l) * (mFlingFriction * mPhysicalCoeff) / INFLEXION);
		return velocity;
	}

	private int getSplineFlingDuration(int velocity) {
		final double l = getSplineDeceleration(velocity);
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		return (int) (1000.0 * Math.exp(l / decelMinusOne));
	}

	private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
	private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
	// Fling friction
	private float mFlingFriction = ViewConfiguration.getScrollFriction();

	private double getSplineDeceleration(int velocity) {
		return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
	}

	@Override
	public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
		Loge(TAG, "NestedScrollingParent: fling: onStopNestedScroll() autoScroll = [" + autoScroll + "]" + ",type0:" + type);
		super.onStopNestedScroll(coordinatorLayout, abl, target, type);
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


	@Override
	public void setCurrentScrollY(int scrollY) {
		lastScrollY = scrollY;
		mTotalScrollY = scrollY;
		int top = mAppBarLayout.getTop();
		if (top != -423) {
			if (lastScrollY != 0) {
				boolean canScrollUp = Utils.canScrollUp(mScrollTarget);
				Loge(TAG, "setCurrentScrollY: canScrollUp:" + canScrollUp);
				if (!canScrollUp) {
//					noHandle = true;
				}
			}
		}
	}

	protected void syncOffset(int newOffset, int dy) {
		Logd(TAG, "syncOffset: newOffset:" + newOffset + ",dy:" + dy);
		if (isFling) {
			LogUtil.Logi(TAG, "syncOffset: isFling");
			return;
		}
		// TODO: 2017/11/7
		if (isInterrupt) {
			isInterrupt = false;
			return;
		}
		Loge(TAG, "run: mScrollY:" + newOffset + ",dy:" + dy);
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
			Loge(TAG, "onLayoutChild: ");
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
			Logw(TAG, "run:  updateOffsets: top:" + top + ",mLayoutTop:" + mLayoutTop + ",offset:" + offset);
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
			Logd(TAG, "fling: newOffset:" + newOffset + ",offset:" + offset);
			if (newOffset != curOffset) {
				curOffset = newOffset;
				dispatchOffsetUpdates(mAppBarLayout, newOffset);
			}
		}

		public boolean setTopAndBottomOffset(int offset) {
			mOffsetTop = offset;
			updateOffsets();
			return true;
		}
	}
}