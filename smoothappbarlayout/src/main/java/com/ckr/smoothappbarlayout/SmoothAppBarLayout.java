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
import android.util.AttributeSet;
import android.view.View;

import com.ckr.smoothappbarlayout.listener.OnFlingListener;
import com.ckr.smoothappbarlayout.listener.OnSmartListener;

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
public class SmoothAppBarLayout extends AppBarLayout implements OnSmartListener {
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
	public void setCurrentScrollY(int scrollY) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.setCurrentScrollY(scrollY);
	}

	@Override
	public void onScrollChanged(View view, int x, int y, int dx, int dy, boolean accuracy) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onScrollChanged(view, x, y, dx, dy, accuracy);
	}

	@Override
	public int getCurrentOffset() {
		if (smoothBehavior == null) {
			initBehavior();
		}
		return smoothBehavior.getCurrentOffset();
	}

	@Override
	public int getTotalRange() {
		if (smoothBehavior == null) {
			initBehavior();
		}
		return smoothBehavior.getTotalRange();
	}

	@Override
	public void onScrollValueChanged(View view,int scrollY,boolean onStartNestedFling) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onScrollValueChanged(view,scrollY,onStartNestedFling);
	}

	@Override
	public void onFlingFinished(View view,float velocityY) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onFlingFinished(view,velocityY);
	}

	@Override
	public void onDispatchFling(View view, int mScrollState) {
		if (smoothBehavior == null) {
			initBehavior();
		}
		smoothBehavior.onDispatchFling(view,mScrollState);
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
			mScrollTarget = target;
		}

		@Override
		public void onScrollChanged(View view, int x, int y, int dx, int dy, boolean accuracy) {
			if (view == mScrollTarget) {
				Logd(TAG, "onScrollChanged: dy:" + dy + ",y:" + y + ",mCurrentOffset:" + mCurrentOffset);
				int translationOffset = Math.max(-mTotalScrollRange, -dy );
				Loge(TAG, "onScrollChanged: translationOffset:" + translationOffset);
				syncOffset(view,translationOffset, y);
			}
		}

		@Override
		public int getTotalRange() {
			return mTotalScrollRange;
		}

		@Override
		public int getCurrentOffset() {
			return mCurrentOffset;
		}

		@Override
		public void onScrollValueChanged(View view,int scrollY,boolean onStartNestedFling) {
			if (mScrollTarget!=view) {
				return;
			}
			mTotalScrollY = scrollY;
			if (onStartNestedFling) {
				flagScrollY=scrollY;
			}
		}

		@Override
		public void onFlingFinished(View view,float velocityY) {
			fling(mAppBarLayout, view,velocityY, false);
		}

		@Override
		public void onDispatchFling(View view, int mScrollState) {
			if (mScrollTarget!=view) {
				return;
			}
			if (mScrollState==0) {
				dispatchFling(mAppBarLayout,mScrollTarget);
			}
		}

		@Override
		public void setOnFlingListener(OnFlingListener onFlingListener) {
			this.mOnFlingListener =onFlingListener;
		}
	}

}