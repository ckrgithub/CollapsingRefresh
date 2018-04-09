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
import com.ckr.smoothappbarlayout.SmoothRecyclerView;
import com.ckr.smoothappbarlayout.listener.OnSmoothScrollListener;
import com.scwang.smartrefresh.SmartRefreshLayout;
import com.scwang.smartrefresh.api.RefreshLayout;
import com.ckr.smoothappbarlayout.listener.OnOffsetListener;
import com.scwang.smartrefresh.listener.OnRefreshLoadmoreListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;

import static com.ckr.smoothappbarlayout.util.RefreshLog.Logd;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class OneFragment extends BaseFragment implements OnRefreshLoadmoreListener, AppBarLayout.OnOffsetChangedListener, OnOffsetListener {
	private static final String TAG = "OneFragment";
	private static final int DELAY_MILLIS =10000;
	@BindView(R.id.recyclerView)
	SmoothRecyclerView recyclerView;
	@BindView(R.id.refreshLayout)
	SmartRefreshLayout smartRefreshLayout;
	@BindDimen(R.dimen.size_5)
	int paddingSize;
	private MyAdapter mAdapter;
	static OnSmoothScrollListener mOnSmoothScrollListener;
	private boolean isVisible;
	private Handler handler = new Handler(Looper.myLooper());
	private int verticalOffset;
	private int num;


	public static OneFragment newInstance(OnSmoothScrollListener onScrollListener) {
		mOnSmoothScrollListener = onScrollListener;
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
		if (mOnSmoothScrollListener != null) {
			mOnSmoothScrollListener.removeOnOffsetChangedListener(this);
		}
	}

	@Override
	protected int getContentLayoutId() {
		return R.layout.fragment_base;
	}

	@Override
	protected void init() {
		Logd(TAG, "init: target:" + recyclerView);
		mOnSmoothScrollListener.addOnOffsetChangedListener(this);
		recyclerView.setOnSmoothScrollListener(mOnSmoothScrollListener);
		smartRefreshLayout.setOnCollapsingListener(this);
		smartRefreshLayout.setOnRefreshLoadmoreListener(this);
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
					clone.setType(0);
				} else {
					clone.setDrawableId(R.mipmap.banner2);
					clone.setType(1);
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
	}

	@Override
	public void onRefresh(RefreshLayout refreshlayout) {
		if (handler != null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (smartRefreshLayout != null) {
						smartRefreshLayout.finishRefresh();
						refreshUi();
					}
				}
			}, DELAY_MILLIS);
		}
	}

	@Override
	public void onLoadmore(RefreshLayout refreshlayout) {
		if (handler != null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (smartRefreshLayout != null) {
						smartRefreshLayout.finishLoadmore();
						loadMoreUi();
					}
				}
			}, DELAY_MILLIS);
		}
	}

	private void refreshUi() {
		AlbumList albumList = new AlbumList();
		albumList.setTitle("风继续吹");
		if (num % 2 == 0) {
			albumList.setDrawableId(R.mipmap.banner2);
			albumList.setType(1);
		} else {
			albumList.setDrawableId(R.mipmap.banner);
			albumList.setType(0);
		}
		albumList.setUserName("refresh  " + num);
		num++;
		mAdapter.updateItem(0, albumList);
	}

	private void loadMoreUi() {
		AlbumList albumList = new AlbumList();
		albumList.setTitle("风继续吹");
		if (num % 2 == 0) {
			albumList.setDrawableId(R.mipmap.banner);
			albumList.setType(0);
		} else {
			albumList.setDrawableId(R.mipmap.banner2);
			albumList.setType(1);
		}
		albumList.setUserName("loadMore  " + num);
		num++;
		mAdapter.updateItem(albumList);
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
	public int getTotalRange() {
		return mOnSmoothScrollListener == null ? 0 : mOnSmoothScrollListener.getTotalRange();
	}

	@Override
	public int getCurrentOffset() {
		return verticalOffset;
	}
}
