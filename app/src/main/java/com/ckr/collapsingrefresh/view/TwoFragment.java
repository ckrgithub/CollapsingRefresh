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
import com.scwang.smartrefresh.listener.OnOffsetListener;
import com.scwang.smartrefresh.listener.OnRefreshLoadmoreListener;
import com.scwang.smartrefresh.util.RefreshLog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;

import static com.scwang.smartrefresh.util.RefreshLog.Logd;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class TwoFragment extends BaseFragment implements OnRefreshLoadmoreListener, AppBarLayout.OnOffsetChangedListener, OnOffsetListener {
	private static final String TAG = "TwoFragment";
	@BindView(R.id.recyclerView)
	SmoothRecyclerView recyclerView;
	@BindView(R.id.refreshLayout)
	SmartRefreshLayout smartRefreshLayout;
	@BindDimen(R.dimen.size_5)
	int paddingSize;
	private MyAdapter mAdapter;
	static OnSmoothScrollListener scrollListener;
	private boolean isVisible;
	private Handler handler = new Handler(Looper.myLooper());
	private int verticalOffset;

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
	protected void init() {
		RefreshLog.Logd(TAG, "init: target:" + recyclerView);
		scrollListener.addOnOffsetChangedListener(this);
		recyclerView.setOnSmoothScrollListener(scrollListener);
		smartRefreshLayout.setOnRefreshLoadmoreListener(this);
		smartRefreshLayout.setOnCollapsingListener(this);
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
			for (int i = 0; i < 12; i++) {
				AlbumList clone = (AlbumList) albumList.clone();
				clone.setUserName("item  " + i);
				if (i % 2 == 0) {
					clone.setDrawableId(R.mipmap.banner2);
					albumList.setType(0);
				} else {
					clone.setDrawableId(R.mipmap.banner);
					albumList.setType(1);
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
		RefreshLog.Logd(TAG, "refreshFragment: isVisible:" + isVisible);
	}

	@Override
	public void onRefresh(RefreshLayout refreshlayout) {
		if (handler != null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (smartRefreshLayout != null) {
						smartRefreshLayout.finishRefresh();
					}
				}
			}, 2000);
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
					}
				}
			}, 2000);
		}
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
		return scrollListener == null ? 0 : scrollListener.getTotalRange();
	}

	@Override
	public int getCurrentOffset() {
		return verticalOffset;
	}
}
