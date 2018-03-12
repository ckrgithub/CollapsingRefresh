package com.ckr.collapsingrefresh.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.adapter.MyAdapter;
import com.ckr.collapsingrefresh.model.AlbumList;
import com.ckr.smartrefresh.SmartRefreshLayout;
import com.ckr.smartrefresh.api.RefreshLayout;
import com.ckr.smartrefresh.listener.OnRefreshLoadmoreListener;
import com.ckr.smoothappbarlayout.SmoothRecyclerView;
import com.ckr.smoothappbarlayout.base.OnSmartListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;

import static com.ckr.smoothappbarlayout.base.LogUtil.Logd;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class OneFragment extends BaseFragment implements OnRefreshLoadmoreListener, AppBarLayout.OnOffsetChangedListener, SmartRefreshLayout.OnPullListener {
    private static final String TAG = "OneFragment";
    @BindView(R.id.recyclerView)
    SmoothRecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout smartRefreshLayout;
    @BindDimen(R.dimen.size_5)
    int paddingSize;
    private MyAdapter mAdapter;
    static OnSmartListener scrollListener;
    private boolean isVisible;
    private static Handler handler = new Handler(Looper.myLooper());
    private int verticalOffset;


    public static OneFragment newInstance(OnSmartListener onScrollListener) {
        scrollListener = onScrollListener;
        Bundle args = new Bundle();
        OneFragment fragment = new OneFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler = null;
        }
        if (scrollListener != null) {
            scrollListener.removeOnOffsetChangedListener(this);
        }
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_base;
    }

    @Override
    protected void init() {
        Logd(TAG, "init: target:"+recyclerView);
        scrollListener.addOnOffsetChangedListener(this);
        recyclerView.setOnSmoothScrollListener(scrollListener);
        smartRefreshLayout.setOnRefreshLoadmoreListener(this);
        smartRefreshLayout.setOnPullListener(this);
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
            for (int i = 0; i < 2; i++) {
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

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                smartRefreshLayout.finishRefresh();
            }
        }, 2000);
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                smartRefreshLayout.finishLoadmore();
            }
        }, 2000);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        this.verticalOffset = verticalOffset;
        Logd(TAG, "onOffsetChanged: verticalOffset:" + this.verticalOffset);
        if (verticalOffset != 0) {
            boolean enableRefresh = smartRefreshLayout.isEnableRefresh();
            if (enableRefresh) {
                smartRefreshLayout.setEnableRefresh(false);
            }
        } else {
            smartRefreshLayout.setEnableRefresh(true);
        }
    }

    @Override
    public int getCurrentOffset() {
        return verticalOffset;
    }
}
