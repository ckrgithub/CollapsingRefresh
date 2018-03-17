
package com.ckr.smoothappbarlayout;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import com.ckr.smoothappbarlayout.listener.OnFlingListener;
import com.ckr.smoothappbarlayout.listener.OnSmoothScrollListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.scwang.smartrefresh.util.LogUtil.Logd;
import static com.scwang.smartrefresh.util.LogUtil.Loge;


/**
 * Created by PC大佬 on 2018/2/9.
 */
@CoordinatorLayout.DefaultBehavior(SmoothAppBarLayout.SmoothBehavior.class)
public class SmoothAppBarLayout extends AppBarLayout implements OnSmoothScrollListener {
	protected final List<WeakReference<OnOffsetChangedListener>> mOffsetChangedListeners = new ArrayList<>();
	private SmoothBehavior smoothBehavior;

	public SmoothAppBarLayout(Context context) {
		super(context);
	}

	public SmoothAppBarLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	public void addOnOffsetChangedListener(OnOffsetChangedListener listener) {
		super.addOnOffsetChangedListener(listener);
		int i = 0;
		for (int z = this.mOffsetChangedListeners.size(); i < z; ++i) {
			WeakReference ref = (WeakReference) this.mOffsetChangedListeners.get(i);
			if (ref != null && ref.get() == listener) {
				return;
			}
		}
		this.mOffsetChangedListeners.add(new WeakReference(listener));
	}

	@Override
	public void removeOnOffsetChangedListener(OnOffsetChangedListener listener) {
		super.removeOnOffsetChangedListener(listener);
		Iterator i = mOffsetChangedListeners.iterator();
		while (true) {
			OnOffsetChangedListener item;
			do {
				if (!i.hasNext()) {
					return;
				}
				WeakReference ref = (WeakReference) i.next();
				item = (OnOffsetChangedListener) ref.get();
			} while (item != listener && item != null);
			i.remove();
		}
	}

	@Override
	public void setScrollTarget(View target) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.setScrollTarget(target);
	}

	@Override
	public void onScrollChanged(View view, int y, int dy) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onScrollChanged(view, y, dy);
	}

	@Override
	public int getCurrentOffset() {
		if (smoothBehavior == null) {
			initBehavior();
		}
		return smoothBehavior.getCurrentOffset();
	}

	@Override
	public int getTotalCollapsedRange() {
		if (smoothBehavior == null) {
			initBehavior();
		}
		return smoothBehavior.getTotalCollapsedRange();
	}

	@Override
	public void onScrollValueChanged(View view, int scrollY, boolean onStartNestedFling) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onScrollValueChanged(view, scrollY, onStartNestedFling);
	}

	@Override
	public void onStartFling(View view, float velocityY) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onStartFling(view, velocityY);
	}

	@Override
	public void onDispatchFling(View view, int mScrollState) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onDispatchFling(view, mScrollState);
	}

	@Override
	public void setOnFlingListener(OnFlingListener onFlingListener) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.setOnFlingListener(onFlingListener);
	}

	private void initBehavior() {
		CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
		this.smoothBehavior = (SmoothBehavior) params.getBehavior();
	}

	public static class SmoothBehavior extends BaseBehavior {
		private static final String TAG = "SmoothBehavior";

		@Override
		public void setScrollTarget(View target) {
			if (mScrollTarget != target) {
				mScrollTarget = target;
			}
		}

		@Override
		public void onScrollChanged(View view, int y, int dy) {
			if (view == mScrollTarget && dy != 0) {
				Logd(TAG, "onScrollChanged: dy:" + dy + ",y:" + y + ",mCurrentOffset:" + mCurrentOffset);
				int translationOffset = Math.max(-mTotalScrollRange, -dy);
				Loge(TAG, "onScrollChanged: translationOffset:" + translationOffset);
				syncOffset(view, translationOffset);
			}
		}

		@Override
		public int getTotalCollapsedRange() {
			return mTotalScrollRange;
		}

		@Override
		public int getCurrentOffset() {
			return mCurrentOffset;
		}

		@Override
		public void onScrollValueChanged(View view, int scrollY, boolean onStartNestedFling) {
			if (mScrollTarget != view) {
				return;
			}
			mTotalScrollY = scrollY;
			if (onStartNestedFling) {
				mScrollYWhenPreFling = scrollY;
			}
		}

		@Override
		public void onStartFling(View view, float velocityY) {
			fling(mAppBarLayout, view, velocityY, false);
		}

		@Override
		public void onDispatchFling(View view, int mScrollState) {
			if (mScrollTarget != view) {
				return;
			}
			if (mScrollState == 0) {
				dispatchFling(mAppBarLayout, mScrollTarget);
			}
		}

		@Override
		public void setOnFlingListener(OnFlingListener onFlingListener) {
			this.mOnFlingListener = onFlingListener;
		}
	}

}