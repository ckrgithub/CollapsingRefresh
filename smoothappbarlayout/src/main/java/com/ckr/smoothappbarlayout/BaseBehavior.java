
package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.hardware.SensorManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.ckr.smoothappbarlayout.listener.OnFlingListener;
import com.ckr.smoothappbarlayout.listener.OnScrollListener;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.support.v4.view.ViewCompat.offsetTopAndBottom;
import static com.scwang.smartrefresh.util.LogUtil.Logd;
import static com.scwang.smartrefresh.util.LogUtil.Loge;
import static com.scwang.smartrefresh.util.LogUtil.Logw;

/**
 * Created by PC大佬 on 2018/2/9.
 */
public abstract class BaseBehavior extends AppBarLayout.Behavior implements OnScrollListener {
	private static final String TAG = "BaseBehavior";
	private static final int VELOCITY_UNITS = 1000;//1000 provides pixels per second
	protected AppBarLayout mAppBarLayout;
	protected View mScrollTarget;//可滚动的view
	protected int mTotalScrollY;//mScrollTarget总共滚动的距离
	protected int mScrollYWhenPreFling;//mScrollTarget开始fling时的滚动距离
	private boolean mIsOnInit = false;
	private Runnable mFlingRunnable;
	private OverScroller mScroller;

	private ViewOffsetHelper mViewOffsetHelper;
	private int mTempTopBottomOffset = 0;
	protected int mTotalScrollRange;
	protected int mCurrentOffset;
	private float velocityY;//向下滚动时的velocity
	private boolean isNestedPreScroll;//防止跳动

	protected OnFlingListener mOnFlingListener;
	private double flingDistance;

	private float mPhysicalCoeff;
	private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
	private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
	// Fling friction
	private float mFlingFriction = ViewConfiguration.getScrollFriction();
	static final Interpolator sQuinticInterpolator = new Interpolator() {
		@Override
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};

	private static final int INVALID_POINTER = -1;
	private boolean mIsBeingDragged;
	private int mActivePointerId = INVALID_POINTER;
	private int mLastMotionY;
	private int mTouchSlop = -1;
	private VelocityTracker mVelocityTracker;

	public BaseBehavior() {

	}

	public BaseBehavior(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onMeasureChild(CoordinatorLayout coordinatorLayout, AppBarLayout child, int parentWidthMeasureSpec, int widthUsed,
								  int parentHeightMeasureSpec, int heightUsed) {
		if (!mIsOnInit) {
			mIsOnInit = true;
			init(child);
		}
		return super.onMeasureChild(coordinatorLayout, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
	}

	private void init(final AppBarLayout child) {
		this.mAppBarLayout = child;
//		setDragCallback(new DragCallback() {//不允许head布局拖动
//			@Override
//			public boolean canDrag(AppBarLayout appBarLayout) {
//				return false;
//			}
//		});
		final float ppi = child.getResources().getDisplayMetrics().density * 160.0f;
		mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
				* 39.37f // inch/meter
				* ppi
				* 0.84f; // look and feel tuning
	}

	@Override
	public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
//		return super.onInterceptTouchEvent(parent, child, ev);
		if (mTouchSlop < 0) {
			mTouchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
		}

		final int action = ev.getAction();

		// Shortcut since we're being dragged
		if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
			return true;
		}

		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				mIsBeingDragged = false;
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();
				if (canDragView(child) && parent.isPointInChildBounds(child, x, y)) {
					mLastMotionY = y;
					mActivePointerId = ev.getPointerId(0);
					ensureVelocityTracker();
				}
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				final int activePointerId = mActivePointerId;
				if (activePointerId == INVALID_POINTER) {
					// If we don't have a valid id, the touch down wasn't on content.
					break;
				}
				final int pointerIndex = ev.findPointerIndex(activePointerId);
				if (pointerIndex == -1) {
					break;
				}

				final int y = (int) ev.getY(pointerIndex);
				final int yDiff = Math.abs(y - mLastMotionY);
				if (yDiff > mTouchSlop) {
					mIsBeingDragged = true;
					mLastMotionY = y;
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				break;
			}
		}

		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(ev);
		}

		return mIsBeingDragged;
	}

	boolean canDragView(View view) {
		return true;
	}

	private void ensureVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	@Override
	public boolean onTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
		if (mTouchSlop < 0) {
			mTouchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
		}

		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();

				if (parent.isPointInChildBounds(child, x, y) && canDragView(child)) {
					mLastMotionY = y;
					mActivePointerId = ev.getPointerId(0);
					ensureVelocityTracker();
				} else {
					return false;
				}
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
				if (activePointerIndex == -1) {
					return false;
				}

				final int y = (int) ev.getY(activePointerIndex);
				int dy = mLastMotionY - y;

				if (!mIsBeingDragged && Math.abs(dy) > mTouchSlop) {
					mIsBeingDragged = true;
					if (dy > 0) {
						dy -= mTouchSlop;
					} else {
						dy += mTouchSlop;
					}
				}

				if (mIsBeingDragged) {
					mLastMotionY = y;
					// We're being dragged so scroll the ABL
					if (dy != 0) {
						Logd(TAG, "onTouchEvent: dy:" + dy + ",y:" + y + ",mCurrentOffset:" + mCurrentOffset);
						int translationOffset = Math.max(-mTotalScrollRange, -dy);
						Loge(TAG, "onTouchEvent: translationOffset:" + translationOffset);
						syncOffset(mScrollTarget, translationOffset);
					}
				}
				break;
			}

			case MotionEvent.ACTION_UP:
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(ev);
					mVelocityTracker.computeCurrentVelocity(1000);
					float yvel = -mVelocityTracker.getYVelocity(mActivePointerId);
					Logd(TAG, "onTouchEvent: yvel:"+yvel);
					fling(mAppBarLayout, mScrollTarget, yvel, false,false);
				}
				// $FALLTHROUGH
			case MotionEvent.ACTION_CANCEL: {
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				break;
			}
		}

		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(ev);
		}

		return true;
	}

	@Override
	public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
		Logw(TAG, "NestedScrollingParent,onNestedPreScroll: dy:" + dy + ",mTotalScrollY：" + mTotalScrollY + ",mCurrentOffset:" + mCurrentOffset);
		if (dy != 0) {
			if (dy < 0) {
				// We're scrolling down
				if (!isCurrentView(target)) return;
				isNestedPreScroll = true;
			} else {
				// We're scrolling up
				if (!isCurrentView(target)) return;
				if (mCurrentOffset == -mTotalScrollRange) {
					Loge(TAG, "NestedScrollingParent,onNestedPreScroll: isNestedPreScroll=true:");
					isNestedPreScroll = true;
					return;
				}
				isNestedPreScroll = false;
				Loge(TAG, "NestedScrollingParent,onNestedPreScroll: setTopAndBottomOffset:");
				dy = Math.max(-mTotalScrollRange, -dy);
				setTopAndBottomOffset(dy);
			}
		}
	}

	protected void dispatchFling(AppBarLayout child, View target) {
		if (!isCurrentView(target)) return;
		if (this.velocityY == 0) {
			return;
		}
		Logd(TAG, "NestedScrollingParent  dispatchFling: mTotalScrollY:" + mTotalScrollY);
		float velocityY = this.velocityY;
		this.velocityY = 0;
		flingDistance = getSplineFlingDistance((int) velocityY);
		Logd(TAG, "NestedScrollingParent  fling: velocityY =" + velocityY + ",mScrollYWhenPreFling:" + mScrollYWhenPreFling
				+ ",flingDistance:" + flingDistance);
		float flingY = (float) ((flingDistance - mScrollYWhenPreFling) * velocityY / flingDistance);
		int subVelocity = getVelocityWithDistance(mScrollYWhenPreFling);
		int subV = getVelocityWithDistance(-mTotalScrollRange);
		int subVelocity2 = getVelocityWithDistance(flingDistance - mScrollYWhenPreFling);
		int sumVelocity = getVelocityWithDistance(flingDistance);
		Logd(TAG, "NestedScrollingParent: fling:  subVelocity:" + subVelocity + ",subVelocity2:" + subVelocity2 + ",sumVelocity:" + sumVelocity);
		boolean isOverScroll = false;
		if (subVelocity2 > (subV + 1)) {//2495
			isOverScroll = true;
		}
		Logd(TAG, "NestedScrollingParent: fling:  flingY:" + flingY + ",mTotalScrollRangeV:" + subV);
		fling(child, target, -Math.abs(subVelocity2), isOverScroll,true);
	}

	@Override
	public boolean onNestedFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target,
								 float velocityX, float velocityY, boolean consumed) {
		Loge(TAG, "NestedScrollingParent,onNestedFling: fling: velocityY = [" + velocityY + "], consumed = [" + consumed + "]");
		if (!isCurrentView(target)) return true;
		if (consumed) {
			if (velocityY < 0) {
				this.velocityY = velocityY;
			}
		}
		return true;
	}

	/**
	 * {@link android.support.design.widget.HeaderBehavior}中fling()
	 *
	 * @param layout
	 * @param target
	 * @param velocityY
	 * @param isOverScroll
	 * @return
	 */
	final boolean fling(AppBarLayout layout, View target, float velocityY, boolean isOverScroll,boolean isDispatch) {
		if (!isCurrentView(target)) return false;
		Logd(TAG, "fling: velocityY:" + velocityY + ",mCurrentOffset:" + mCurrentOffset + ",mTotalScrollY:" + mTotalScrollY);
		if (mFlingRunnable != null) {
			layout.removeCallbacks(mFlingRunnable);
			mFlingRunnable = null;
		}
		if (mScroller == null) {
			mScroller = new OverScroller(layout.getContext(), sQuinticInterpolator);
		}
		mScroller.fling(0, 0, 0, Math.round(velocityY)
				, 0, 0, -mTotalScrollRange, mTotalScrollRange);
		boolean canScroll = mScroller.computeScrollOffset();
		if (canScroll) {
			mFlingRunnable = new FlingRunnable(layout, target, velocityY, isOverScroll,isDispatch);
			ViewCompat.postOnAnimation(layout, mFlingRunnable);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@link android.support.design.widget.HeaderBehavior.FlingRunnable}
	 */
	private class FlingRunnable implements Runnable {
		private final AppBarLayout mLayout;
		private final View scrollTarget;
		private float velocityY;
		private int mLastY = 0;
		private boolean autoScroll;
		private boolean isOverScroll;
		private boolean isDispatch;

		FlingRunnable(AppBarLayout layout, View target, float velocityY,  boolean isOverScroll,boolean isDispatch) {
			mLayout = layout;
			scrollTarget = target;
			this.mLastY = 0;
			this.velocityY = velocityY;
			this.isOverScroll = isOverScroll;
			this.isDispatch = isDispatch;
		}

		@Override
		public void run() {
			if (mLayout != null && mScroller != null && mScrollTarget == scrollTarget) {
				final OverScroller mScroller = BaseBehavior.this.mScroller;
				if (mScroller.computeScrollOffset()) {
					int currY = mScroller.getCurrY();
					int y = mLastY - currY;//-7.-11,8,33
					mLastY = currY;
					Loge(TAG, "run: syncOffset: currY:" + currY + ",y:" + y + ",velocityY:" + velocityY + ",mCurrentOffset:" + mCurrentOffset);
					if (mCurrentOffset == -mTotalScrollRange && mOnFlingListener != null && !autoScroll && velocityY > 0&&isDispatch) {
						autoScroll = true;
						mOnFlingListener.onStartFling(scrollTarget, velocityY);
					} else {
						if (y != 0) {
							setTopAndBottomOffset(y);
						} else {
							if (isOverScroll) {
								isOverScroll = false;
								int dy = 0;
								if (velocityY > 0) {
									dy = (currY - mTotalScrollRange) / 2;
								} else {
									dy = (currY + mTotalScrollRange) / 2;
								}
								Logd(TAG, "run: fling: dy:" + dy);
								if (dy != 0) {
									setTopAndBottomOffset(dy);
								}
							}
						}
					}
					ViewCompat.postOnAnimation(mLayout, this);
				} else {
					Logd(TAG, "run: fling:  isFling=false");
					autoScroll = false;
					isOverScroll = false;
				}
			} else {
				if (mScroller != null && !mScroller.isFinished()) {
					mScroller.abortAnimation();
				}
			}
		}
	}

	/**
	 * {@link OverScroller}中getSplineFlingDistance
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
		double l = Math.log(Math.abs(distance) / (mFlingFriction * mPhysicalCoeff)) * decelMinusOne / DECELERATION_RATE;
		int velocity = (int) (Math.exp(l) * (mFlingFriction * mPhysicalCoeff) / INFLEXION);
		return velocity;
	}

	private double getSplineDeceleration(int velocity) {
		return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
	}

	protected void syncOffset(View view, int newOffset) {
		if (!isCurrentView(view)) return;
		Logd(TAG, "syncOffset:   newOffset:" + newOffset
				+ ",isNestedPreScroll：" + isNestedPreScroll + ",mCurrentOffset:" + mCurrentOffset);
		if (isNestedPreScroll) {
			isNestedPreScroll = false;
			return;
		}
		if (mCurrentOffset == -mTotalScrollRange && newOffset < 0) {
			return;
		} else if (mCurrentOffset == 0 && newOffset > 0) {
			return;
		}
		setTopAndBottomOffset(newOffset);
	}

	private boolean isCurrentView(View view) {
		if (mScrollTarget != view) {
			return false;
		}
		return true;
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
		int height = this.mAppBarLayout.getHeight();
		int minimumHeight = this.mAppBarLayout.getMinimumHeight();
		mTotalScrollRange = height - minimumHeight;
		Logd(TAG, "init: mTotalScrollRange:" + mTotalScrollRange + ",height:" + height + ",minimumHeight:" + minimumHeight);
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

	/**
	 * see to {@link android.support.design.widget.ViewOffsetBehavior} and
	 * {@link android.support.design.widget.ViewOffsetHelper}
	 */
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
			Logd(TAG, "onViewLayout: " + mCurrentOffset);
			mLayoutTop = mView.getTop();
			// And offset it as needed
//			updateOffsets();
			if (mCurrentOffset != 0) {//底部上啦事件冲突
				setTopAndBottomOffset(mCurrentOffset);
			}
		}

		private void updateOffsets() {
			int offset = mOffsetTop;
			int top = mView.getTop();
			Logw(TAG, "updateOffsets: top:" + top + ",mLayoutTop:" + mLayoutTop + ",offset:" + offset);
			int newOffset;
			if (top + offset > 0) {
				offsetTopAndBottom(mView, -top);
				newOffset = 0;
			} else if (top == -mTotalScrollRange) {
				if (offset > 0) {
					offsetTopAndBottom(mView, offset);
					newOffset = top + offset;
				} else {
					newOffset = -mTotalScrollRange;
				}
			} else if (top + offset < -mTotalScrollRange) {
				offset = -mTotalScrollRange - top;
				offsetTopAndBottom(mView, offset);
				newOffset = -mTotalScrollRange;
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