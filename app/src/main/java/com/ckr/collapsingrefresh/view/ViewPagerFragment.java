package com.ckr.collapsingrefresh.view;


import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.util.ScreenUtil;
import com.ckr.collapsingrefresh.widget.MyFragmentPagerAdapter;
import com.ckr.smoothappbarlayout.SmoothAppBarLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindView;

import static com.ckr.smoothappbarlayout.base.LogUtil.Logd;

/**
 * Created by PC大佬 on 2018/2/9.
 */
public class ViewPagerFragment extends BaseFragment implements AppBarLayout.OnOffsetChangedListener, ViewPager.OnPageChangeListener {
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.statusBarPaddingView)
    View statusBarPaddingView;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.appBarLayout)
    SmoothAppBarLayout appBarLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindColor(R.color.color_red)
    int tabIndicatorColor;
    @BindDimen(R.dimen.tab_indicator_height)
    int tabIndicatorHeight;
    @BindColor(R.color.tab_text_color)
    int tabTextColor;
    @BindColor(R.color.tab_text_color_selected)
    int tabTextColor2;
    @BindArray(R.array.tab_title)
    String[] tabTitles;
    private int totalScrollRange;
    private List<BaseFragment> fragmentList;
    private int totalHeight;

    public static ViewPagerFragment newInstance() {
        Bundle args = new Bundle();
        ViewPagerFragment fragment = new ViewPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_view_pager;
    }

    @Override
    protected void init() {
        initView();
        initFragment();
        initTabLayout();
    }

    private void initView() {
        int minHeight = (int) getResources().getDimension(R.dimen.size_78);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int statusBarHeight = ScreenUtil.getStatusBarHeight(getContext());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            layoutParams.topMargin = statusBarHeight;
            toolbar.setLayoutParams(layoutParams);
            ViewGroup.LayoutParams params = statusBarPaddingView.getLayoutParams();
            params.height = statusBarHeight;
            statusBarPaddingView.setLayoutParams(params);
            statusBarPaddingView.setVisibility(View.VISIBLE);
            minHeight = minHeight + statusBarHeight;
            appBarLayout.setMinimumHeight(minHeight);
        } else {
        }
        this.totalHeight = (int) getResources().getDimension(R.dimen.mine_info_height);
        totalScrollRange = totalHeight - minHeight;
//        appBarLayout.addOnOffsetChangedListener(this);
    }

    private void initFragment() {
        fragmentList = new ArrayList<>();
        fragmentList.add(OneFragment.newInstance(appBarLayout));
        fragmentList.add(TwoFragment.newInstance(appBarLayout));
    }

    private void initTabLayout() {
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabTextColors(tabTextColor, tabTextColor2);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setSelectedTabIndicatorColor(tabIndicatorColor);
        tabLayout.setSelectedTabIndicatorHeight(tabIndicatorHeight);
        tabLayout.addTab(tabLayout.newTab().setText(tabTitles[0]), true);
        viewPager.setAdapter(new MyFragmentPagerAdapter(getChildFragmentManager(), fragmentList, tabTitles));
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(this);
    }

    private static final String TAG = "ViewPagerFragment";

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        Logd(TAG, "onOffsetChanged: vertical:" + verticalOffset);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        BaseFragment baseFragment = fragmentList.get(position);
        baseFragment.refreshFragment();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
