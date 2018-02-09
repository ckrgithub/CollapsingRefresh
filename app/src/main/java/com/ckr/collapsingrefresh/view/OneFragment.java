package com.ckr.collapsingrefresh.view;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.model.AlbumList;
import com.ckr.collapsingrefresh.view.adapter.MyAdapter;
import com.ckr.smoothappbarlayout.SmoothRecyclerView;
import com.ckr.smoothappbarlayout.base.LogUtil;
import com.ckr.smoothappbarlayout.base.OnSmoothScrollListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class OneFragment extends BaseFragment {
    private static final String TAG = "OneFragment";
    @BindView(R.id.recyclerView)
    SmoothRecyclerView recyclerView;
    private MyAdapter mAdapter;
    @BindDimen(R.dimen.size_5)
    int paddingSize;
    static OnSmoothScrollListener scrollListener;
    private boolean isVisible;

    public static OneFragment newInstance(OnSmoothScrollListener onScrollListener) {
        scrollListener = onScrollListener;
        Bundle args = new Bundle();
        OneFragment fragment = new OneFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_base;
    }

    @Override
    protected void init() {
        LogUtil.Logd(TAG, "init: target:"+recyclerView);
        recyclerView.setOnSmoothScrollListener(scrollListener);
        setAdapter();
    }

    protected void setAdapter() {
        mAdapter = new MyAdapter(getContext());
        LinearLayoutManager layout = new LinearLayoutManager(getContext());
        layout.setSmoothScrollbarEnabled(true);
        layout.setAutoMeasureEnabled(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(layout);
        recyclerView.setAdapter(mAdapter);
        initData();
    }

    private void initData() {
        List<AlbumList> datas = new ArrayList<>();
        AlbumList albumList = new AlbumList();
        albumList.setTitle("风继续吹");
        try {
            for (int i = 0; i < 15; i++) {
                AlbumList clone = (AlbumList) albumList.clone();
                clone.setUserName("item  " + i);
                if (i % 2 == 0) {
                    clone.setDrawableId(R.mipmap.banner);
                } else {
                    clone.setDrawableId(R.mipmap.banner2);
                }
                datas.add(clone);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        mAdapter.updateAll(datas);
    }

    @Override
    protected void onVisible() {
        isVisible = true;
    }

    @Override
    protected void onInvisible() {
        isVisible=false;
    }

    @Override
    public void refreshFragment() {
        if (isVisible) {
            scrollListener.setScrollTarget(recyclerView);
            recyclerView.setCurrentScrollY();
        }
    }
}
