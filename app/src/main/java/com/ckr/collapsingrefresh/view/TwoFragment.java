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
 * Created by PC大佬 on 2017/11/1.
 */

public class TwoFragment extends BaseFragment {
    private static final String TAG = "TwoFragment";
    @BindView(R.id.recyclerView)
    SmoothRecyclerView recyclerView;
    private MyAdapter mAdapter;
    @BindDimen(R.dimen.size_5)
    int paddingSize;
    static OnSmoothScrollListener scrollListener;
    private boolean isVisible;

    public static TwoFragment newInstance(OnSmoothScrollListener onScrollListener) {
        scrollListener = onScrollListener;
        Bundle args = new Bundle();

        TwoFragment fragment = new TwoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_base;
    }


    @Override
    protected void init() {
        recyclerView.setOnSmoothScrollListener(scrollListener);
        LogUtil.Logd(TAG, "init: target:" + recyclerView);
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
        albumList.setTitle("约定");
        try {
            for (int i = 0; i < 10; i++) {
                AlbumList clone = (AlbumList) albumList.clone();
                clone.setUserName("item  " + i);
                if (i % 2 == 0) {
                    clone.setDrawableId(R.mipmap.banner2);
                } else {
                    clone.setDrawableId(R.mipmap.banner);
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
        isVisible = false;
    }

    @Override
    public void refreshFragment() {
        LogUtil.Logd(TAG, "refreshFragment: isVisible:" + isVisible);
        if (isVisible) {
            scrollListener.setScrollTarget(recyclerView);
            recyclerView.setCurrentScrollY();
        }
    }
}
