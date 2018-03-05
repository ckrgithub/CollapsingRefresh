package com.ckr.smoothappbarlayout.base;

import android.view.View;

/**
 * Created by PC大佬 on 2018/3/3.
 */

public interface OnFlingCallBack {
	void onFlingFinished(float velocityY, int dy , View target);
}
