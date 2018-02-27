package com.ckr.collapsingrefresh.view;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.widget.CustomViewPager;
import com.ckr.collapsingrefresh.widget.MyFragmentPagerAdapter;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends BaseFragment implements ViewPager.OnPageChangeListener {
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    private FragmentManager fragmentManager;
    private ArrayList<BaseFragment> fragmentList;
    private static final String[] TITLES = {"改造", "原生"};
    private int mCurrentPage;
    private Bundle saveState;

    public static MainFragment newInstance() {

        Bundle args = new Bundle();

        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_main;
    }

    @Override
    protected void init() {
        initFragment();
        initView();
    }
    private void initFragment() {
        int length = TITLES.length;
        fragmentManager = getChildFragmentManager();
        fragmentList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            String name = makeFragmentName(R.id.viewPager, i);
            BaseFragment fragment = (BaseFragment) fragmentManager.findFragmentByTag(name);
            if (fragment == null) {
                if (i == 0) {
                    fragmentList.add(ViewPagerFragment.newInstance());
                } else if (i == 1) {
                    fragmentList.add(NativeFragment.newInstance());
                }
            } else {
                fragmentList.add(fragment);
            }
        }
    }
    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
    private void initView() {
        viewPager.addOnPageChangeListener(this);
        viewPager.setAdapter(new MyFragmentPagerAdapter(fragmentManager, fragmentList, TITLES));
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
