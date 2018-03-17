
package com.ckr.smoothappbarlayout.listener;

import android.support.design.widget.AppBarLayout;

/**
 * Created by PC大佬 on 2018/2/9.
 */
public interface OnSmoothScrollListener extends OnScrollListener {
	void addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener listener);

	void removeOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener listener);


}
