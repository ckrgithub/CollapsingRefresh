
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
	protected int mScrollYWhenFling;//mScrollTarget开始fling时的滚动距离
	private boolean mIsOnInit = false;
	protected boolean canDragHeader = true;
	private Runnable mFlingRunnable;
	private OverScroller mScroller;

	private ViewOffsetHelper mViewOffsetHelper;
	private int mTempTopBottomOffset = 0;
	protected int mTotalScrollRange;
	protected int mCurrentOffset;
	private float velocityY;//即将fling时的velocity
	private boolean isNestedPreScroll;//防止跳动

	protected OnFlingListener mOnFlingListener;
	private double mFlingDistance;

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
		final float ppi = child.getResources().getDisplayMetrics().density * 160.0f;
		mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
				* 39.37f // inch/meter
				* ppi
				* 0.84f; // look and feel tuning
	}

	/**
	 * see to {@link android.support.design.widget.HeaderBehavior}
	 *
	 * @param parent
	 * @param child
	 * @param ev
	 * @return
	 */
	@Override
	public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
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
				if (canDragHeader && parent.isPointInChildBounds(child, x, y)) {
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

	private void ensureVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	/**
	 * see to {@link android.support.design.widget.HeaderBehavior}
	 *
	 * @param parent
	 * @param child
	 * @param ev
	 * @return
	 */
	@Override
	public boolean onTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
		if (mTouchSlop < 0) {
			mTouchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
		}
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();

				if (parent.isPointInChildBounds(child, x, y) && canDragHeader) {
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
					syncOffset(dy);
				}
				break;
			}
			case MotionEvent.ACTION_UP:
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(ev);
					mVelocityTracker.computeCurrentVelocity(VELOCITY_UNITS);
					float yvel = -mVelocityTracker.getYVelocity(mActivePointerId);
					Logd(TAG, "onTouchEvent: yvel:" + yvel);
					fling(mAppBarLayout, mScrollTarget, yvel, false, false);
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
		Logw(TAG, "NestedScrollingParent,onNestedPreScroll: dy:" + dy + ",mTotalScrollY:" + mTotalScrollY + ",mCurrentOffset:" + mCurrentOffset);
		if (dy != 0) {
			if (dy < 0) {
				// We're scrolling down
				if (!isCurrentScrollTarget(target)) return;
				isNestedPreScroll = true;
			} else {
				// We're scrolling up
				if (!isCurrentScrollTarget(target)) return;
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

	@Override
	public boolean onNestedFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target,
								 float velocityX, float velocityY, boolean consumed) {
		Loge(TAG, "NestedScrollingParent,onNestedFling: fling: velocityY = [" + velocityY + "], consumed = [" + consumed + "]");
		if (!isCurrentScrollTarget(target)) return true;
		if (consumed) {
			if (velocityY < 0) {
				this.velocityY = velocityY;
			}
		}
		return true;
	}

	protected void dispatchFling(AppBarLayout child, View target) {
		if (!isCurrentScrollTarget(target)) return;
		if (this.velocityY == 0) {
			return;
		}
		float velocityY = this.velocityY;
		this.velocityY = 0;
		mFlingDistance = getSplineFlingDistance((int) velocityY);
		Logd(TAG, "fling: velocityY =" + velocityY + ",mScrollYWhenFling:" + mScrollYWhenFling
				+ ",mFlingDistance:" + mFlingDistance);
		int subV = getVelocityByDistance(-mTotalScrollRange);
		int curVelocity = getVelocityByDistance(mFlingDistance - mScrollYWhenFling);
		Logd(TAG, "fling:  curVelocity:" + curVelocity + ",subV:" + subV);
		boolean isOverScroll = false;
		if (curVelocity > (subV + 1)) {//2495
			isOverScroll = true;
		}
		fling(child, target, -Math.abs(curVelocity), isOverScroll, false);
	}

	/**
	 * see to {@link OverScroller}
	 *
	 * @param velocity
	 * @return
	 */
	private double getSplineFlingDistance(int velocity) {
		final double l = getSplineDeceleration(velocity);
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
	}

	private int getVelocityByDistance(double distance) {
		final double decelMinusOne = DECELERATION_RATE - 1.0;
		double l = Math.log(Math.abs(distance) / (mFlingFriction * mPhysicalCoeff)) * decelMinusOne / DECELERATION_RATE;
		int velocity = (int) (Math.exp(l) * (mFlingFriction * mPhysicalCoeff) / INFLEXION);
		return velocity;
	}

	private double getSplineDeceleration(int velocity) {
		return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
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
	final boolean fling(AppBarLayout layout, View target, float velocityY, boolean isOverScroll, boolean isDispatch) {
		if (!isCurrentScrollTarget(target)) return false;
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
			mFlingRunnable = new FlingRunnable(layout, target, velocityY, isOverScroll, isDispatch);
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

		FlingRunnable(AppBarLayout layout, View target, float velocityY, boolean isOverScroll, boolean isDispatch) {
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
					int y = mLastY - currY;
					mLastY = currY;
					Loge(TAG, "run: syncOffset: currY:" + currY + ",y:" + y + ",velocityY:" + velocityY + ",mCurrentOffset:" + mCurrentOffset);
					if (mCurrentOffset == -mTotalScrollRange && mOnFlingListener != null && !autoScroll && velocityY > 0 && isDispatch) {

						autoScroll = true;
						mOnFlingListener.onStartFling(scrollTarget, velocityY);
					} else {
						if (y != 0) {
							syncTopAndBottomOffset(y);
						} else {
							if (isOverScroll) {
								isOverScroll = false;
								int dy = 0;
								if (velocityY > 0) {
									dy = (currY - mTotalScrollRange) / 2 - 3;
								} else {
									dy = (currY + mTotalScrollRange) / 2 + 3;
								}
								Logd(TAG, "run: fling: dy:" + dy);
								syncTopAndBottomOffset(dy);
							}
						}
					}
					ViewCompat.postOnAnimation(mLayout, this);
				} else {
					Logd(TAG, "run: fling:  isFling=false");
					autoScroll = false;
					isOverScroll = false;
				}
			}
		}
	}

	public void syncOffset(View view, int newOffset) {
		if (!isCurrentScrollTarget(view)) return;
		if (newOffset == 0) {
			return;
		}
		newOffset = Math.max(-mTotalScrollRange, -newOffset);
		Logd(TAG, "syncOffset:  newOffset:" + newOffset + ",isNestedPreScroll：" + isNestedPreScroll);
		if (isNestedPreScroll) {
			isNestedPreScroll = false;
			return;
		}
		syncTopAndBottomOffset(newOffset);
	}

	protected void syncOffset(int newOffset) {
		if (newOffset == 0) {
			return;
		}
		newOffset = Math.max(-mTotalScrollRange, -newOffset);
		Logd(TAG, "syncOffset:  newOffset:" + newOffset + ",isNestedPreScroll：" + isNestedPreScroll);
		if (isNestedPreScroll) {
			isNestedPreScroll = false;
			return;
		}
		syncTopAndBottomOffset(newOffset);
	}

	protected boolean isCurrentScrollTarget(View target) {
		if (mScrollTarget != target) {
			return false;
		}
		return true;
	}

	private void syncTopAndBottomOffset(int offset) {
		if (mCurrentOffset == -mTotalScrollRange && offset < 0) {
			return;
		} else if (mCurrentOffset == 0 && offset > 0) {
			return;
		}else if (offset==0){
			return;
		}
		setTopAndBottomOffset(offset);
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
		//Logd(TAG, "layoutChild: getMeasureHeight=1743:1920-239dp+432(maxScrollOffset)+36dp");
		mTotalScrollRange = getTotalScrollRange(child);
	}

	private int getTotalScrollRange(AppBarLayout child) {
		int height = child.getHeight();
		int minimumHeight = child.getMinimumHeight();
		Logd(TAG, "getTotalScrollRange:  height:" + height + ",minimumHeight:" + minimumHeight);
		return height - minimumHeight;
	}

	protected void dispatchOffsetChanged(AppBarLayout layout, int translationOffset) {
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
		private int mOffsetTop;
		private int curOffset;

		public ViewOffsetHelper(View view) {
			mView = view;
		}

		public void onViewLayout() {
			// And offset it as needed
			if (mCurrentOffset != 0) {//底部上啦事件冲突
				setTopAndBottomOffset(mCurrentOffset);
			}
		}

		private void updateOffsets() {
			int offset = mOffsetTop;
			int top = mView.getTop();
			Logw(TAG, "updateOffsets: top:" + top + ",offset:" + offset);
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
			Logd(TAG, "updateOffsets: newOffset:" + newOffset + ",offset:" + offset);
			if (newOffset != curOffset) {
				curOffset = newOffset;
				dispatchOffsetChanged(mAppBarLayout, newOffset);
			}
		}

		public boolean setTopAndBottomOffset(int offset) {
			mOffsetTop = offset;
			updateOffsets();
			return true;
		}
	}
}