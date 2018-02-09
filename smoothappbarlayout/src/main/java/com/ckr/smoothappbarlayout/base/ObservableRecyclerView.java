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

package com.ckr.smoothappbarlayout.base;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.ckr.smoothappbarlayout.R;


/**
 * Created by henrytao on 2/3/16.
 */
public class ObservableRecyclerView implements Observer {
    private static final int tag = 0;
    public static final int HEADER_VIEW_POSITION = 0;

    public static ObservableRecyclerView newInstance(@NonNull RecyclerView recyclerView, OnSmoothScrollListener onScrollListener) {
        ObservableRecyclerView observable = new ObservableRecyclerView(recyclerView);
        observable.setOnScrollListener(onScrollListener);
        return observable;
    }

    private OnSmoothScrollListener mOnScrollListener;

    private RecyclerView mRecyclerView;

    protected ObservableRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        if (mRecyclerView.getTag(R.id.tag_observable_view) == null) {
            mRecyclerView.setTag(R.id.tag_observable_view, true);
            init();
        }
    }

    @Override
    public View getView() {
        return mRecyclerView;
    }

    @Override
    public void setOnScrollListener(OnSmoothScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    int scrollY;

    private void init() {
        scrollY = 0;
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mOnScrollListener != null) {
                    scrollY += dy;
                    Log.d("test", "onNestedPreScroll: scrollY:"+scrollY);
                    mOnScrollListener.onScrollChanged(recyclerView,
                            recyclerView.computeHorizontalScrollOffset(), scrollY,
                            dx, dy,
                            recyclerView.getLayoutManager().findViewByPosition(HEADER_VIEW_POSITION) != null);
                }
            }
        });

        mRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                onAdapterChanged();
            }
        });
    }

    private void onAdapterChanged() {
        if (mOnScrollListener != null) {
            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            mOnScrollListener.onScrollChanged(mRecyclerView,
                    mRecyclerView.computeHorizontalScrollOffset(), mRecyclerView.computeVerticalScrollOffset(),
                    0, 0,
                    layoutManager != null && layoutManager.findViewByPosition(HEADER_VIEW_POSITION) != null);
        }
    }
}
