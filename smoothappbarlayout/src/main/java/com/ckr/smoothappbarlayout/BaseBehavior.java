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

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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
	protected AppBarLayout child;
	private DragCallback mDragCallbackListener;
	private boolean mIsOnInit = false;
	protected View vScrollTarget;
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

	//	static final Interpolator sQuinticInterpolator = new AccelerateInterpolator();
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
//			if (mScroller != null) {
//				mScroller.abortAnimation();
//			}
			layout.removeCallbacks(mFlingRunnable);
			mFlingRunnable = null;
		}
		if (mScroller == null) {
			mScroller = new OverScroller(layout.getContext(), sQuinticInterpolator);
		}
		Log.d(TAG, "fling: getTop:" + child.getTop() + ",bottom:" + child.getBottom());
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
//			onFlingFinished(layout);
			return false;
		}
	}

	protected void onFlingFinished(AppBarLayout layout) {
		snapToChildIfNeeded(layout);
	}

	private void snapToChildIfNeeded(AppBarLayout layout) {
		int childCount = layout.getChildCount();
//		for (int i = 0; i < childCount; i++) {
//			View childAt = layout.getChildAt(0);
//			int top = -childAt.getTop();
//			int bottom = -childAt.getBottom();
//			Log.d(TAG, "run: top:" + top + ",bottom:" + bottom+",childAt:"+childAt);
//		}
		View childAt = layout.getChildAt(0);
		int top = -childAt.getTop();
		int bottom = -childAt.getBottom();
		bottom += ViewCompat.getMinimumHeight(layout);
		int newOffset = mCurrentOffset < (top + bottom) / 2 ? bottom : top;
		Log.d(TAG, "run: top:" + top + ",bottom:" + bottom + ",mCurrentOffset:" + mCurrentOffset
				+ ",newOffset:" + newOffset);
		animateOffsetTo(layout, (int) clamp(newOffset, -423, 0), 0);
	}

	private void animateOffsetTo(AppBarLayout layout, int clamp, int velocityY) {
		velocityY = Math.abs(velocityY);
		int duration;
		if (velocityY > 0) {
			duration = 3 * Math.round(1000 * (Math.abs(clamp) / velocityY));
		} else {
			float ratio = clamp / layout.getHeight();
			duration = (int) ((ratio + 1) * 150);
		}
		animateOffsetWithDuration(layout, clamp, duration);
	}

	private ValueAnimator mOffsetAnimator;
	static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
	private static final int MAX_OFFSET_ANIMATION_DURATION = 600; // ms

	private void animateOffsetWithDuration(AppBarLayout layout, int clamp, int duration) {
		if (mOffsetAnimator == null) {
			mOffsetAnimator = new ValueAnimator();
			mOffsetAnimator.setInterpolator(DECELERATE_INTERPOLATOR);
			mOffsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					int animatedValue = (int) animation.getAnimatedValue();
					Logd(TAG, "onAnimationUpdate:  run:  animatedValue:" + animatedValue);
					setTopAndBottomOffset(animatedValue);
				}
			});
		} else {
			mOffsetAnimator.cancel();
		}
		Logd(TAG, "run: clamp：" + clamp + ",duration:" + duration + ",mCurrentOffset:" + mCurrentOffset);
		mOffsetAnimator.setDuration(Math.min(duration, MAX_OFFSET_ANIMATION_DURATION));
		mOffsetAnimator.setIntValues(mCurrentOffset, clamp);
		mOffsetAnimator.start();
	}

	public static float clamp(float value, float min, float max) {
		if (value < min) {
			return min;
		} else if (value > max) {
			return max;
		}
		return value;
	}

	/**
	 * RecyclerView is calculating a scroll.
	 * If there are too many of these in Systrace, some Views inside RecyclerView might be causing
	 * it. Try to avoid using EditText, focusable views or handle them with care.
	 */
	static final String TRACE_SCROLL_TAG = "RV Scroll";

	/**
	 * {@link AppBarLayout.Behavior}中onStopNestedScroll,onFlingFinished
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
			if (mLayout != null && mScroller != null && vScrollTarget == scrollTarget) {
				if (mScroller.computeScrollOffset()) {
					isFling = true;
//					if (mLastY == Integer.MIN_VALUE) {
//						int currY = mScroller.getCurrY();
//						mLastY = currY;
//
//					} else {
					int currY = mScroller.getCurrY();
					int y = mLastY - currY;//-7.-11,8,33
					mLastY = currY;
					Loge(TAG, "run: fling: currY:" + currY + ",y:" + y);
					if (y != 0 /*&& !isInterrupt*/) {
						setTopAndBottomOffset(y);
					} else {
						isInterrupt = true;
//						mScroller.abortAnimation();
					}
					ViewCompat.postOnAnimation(mLayout, this);
//					}
				} else {
					Log.d(TAG, "run: fling:  isFling=false");
					isFling = false;
					callBack.onFlingFinished(velocityY);
//					onFlingFinished(mLayout);
					/*if (vScrollTarget instanceof RecyclerView) {
						RecyclerView view= (RecyclerView) vScrollTarget;
					}*/
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

//				handleFling();
//            if (velocityY < -VELOCITY_UNITS) {
//                flingHandle(child, target, velocityY);
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
	public void handleFling() {
		if (velocityY == 0) {
			return;
		}
		float velocityY = this.velocityY;
		this.velocityY = 0;
		flingHandle(child, vScrollTarget, velocityY);
	}

	private void flingHandle(AppBarLayout child, View target, float velocityY) {
		final int targetScroll = child.getTop();
		if (targetScroll != 0) {
			int startY = mTotalScrollY > -targetScroll ? mTotalScrollY : mTotalScrollY - targetScroll;
			Logd(TAG, "onNestedFling: startY:" + startY);
			fling(child, target, startY, targetScroll, 0, -velocityY, false);
		}
	}

	@Override
	public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {
		Logd(TAG, "NestedScrollingParent,onNestedPreFling, fling = [" + velocityX + "], velocityY = [" + velocityY + "]");
		return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
	}

	@Override
	public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
		Logd(TAG, "NestedScrollingParent,onStartNestedScroll, onStartNestedScroll = [" + nestedScrollAxes + "]" + ",target:" + target);
		vScrollTarget = target;
		return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
	}

	@Override
	public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
		Logw(TAG, "NestedScrollingParent,onNestedPreScroll: dx:" + dx + ",dy:" + dy + ",consumed[0]" + consumed[0] + ",consumed[1]" + consumed[1] + ",mTotalScrollY：" + mTotalScrollY);
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
	boolean onStop;

	@Override
	public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
		Loge(TAG, "NestedScrollingParent,onNestedScroll, dyConsumed = [" + dyConsumed + "]" + ", dyUnconsumed = [" + dyUnconsumed + "]" + ",type0:" + type);
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
		if (dyConsumed == 0) {
			onStop = true;
			if (velocityY == 0) {
				return;
			}
			float velocityY = this.velocityY*3 / 10;
			Logd(TAG, "NestedScrollingParent  fling: velocityY =" + this.velocityY + ",/:" + velocityY);
//			float velocityY = this.velocityY * 423 / 1000;
			this.velocityY = 0;
			fling(child, vScrollTarget, 423, 0, 0
					, velocityY, false);
		} else {
			onStop = false;
		}
	}

	@Override
	public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
		Loge(TAG, "NestedScrollingParent: fling: onStopNestedScroll() target = [" + target + "]" + ",type0:" + type);
		super.onStopNestedScroll(coordinatorLayout, abl, target, type);
//		if (type== ViewCompat.TYPE_TOUCH) {
//			onFlingFinished(child);
			/*if (velocityY == 0||!onStop) {
				return;
			}
			float velocityY = this.velocityY;
			this.velocityY = 0;
			fling(child,vScrollTarget,0,0,0
					,velocityY,false);*/
//		}
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
				Loge(TAG, "setCurrentScrollY: canScrollUp:" + canScrollUp);
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
		Loge(TAG, "syncOffset: noHandle:" + noHandle + ",lastScrollY:" + lastScrollY);
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
		Loge(TAG, "run: mScrollY:" + newOffset + ",dy:" + dy
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