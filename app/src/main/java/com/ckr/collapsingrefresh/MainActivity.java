package com.ckr.collapsingrefresh;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ckr.collapsingrefresh.view.MainFragment;

import butterknife.BindView;

/**
 * Created by PC大佬 on 2018/2/9.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.container, MainFragment.newInstance(), MainFragment.class.getName())
                    .commit();
        } else {
            fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag(MainFragment.class.getName()))
                    .commit();
        }
    }
}
