package com.ckr.smoothappbarlayout.utils;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class Utils {

	public static int getStatusBarSize(Context context) {
		int statusBarSize = 0;
		if (context != null) {
			int id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
			if (id > 0) {
				statusBarSize = context.getResources().getDimensionPixelSize(id);
			}
		}
		return statusBarSize;
	}

	public static boolean canScrollUp(View targetView) {
		if (android.os.Build.VERSION.SDK_INT < 14) {
			if (targetView instanceof AbsListView) {
				final AbsListView absListView = (AbsListView) targetView;
				return absListView.getChildCount() > 0
						&& (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
						.getTop() < absListView.getPaddingTop());
			} else {
				return targetView.getScrollY() > 0;
			}
		} else {
			return targetView.canScrollVertically(-1);
		}
	}
}
