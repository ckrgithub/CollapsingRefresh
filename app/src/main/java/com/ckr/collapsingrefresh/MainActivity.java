package com.ckr.collapsingrefresh;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.ckr.collapsingrefresh.view.ViewPagerFragment;

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
                    .add(R.id.container, ViewPagerFragment.newInstance(), ViewPagerFragment.class.getName())
                    .commit();
        } else {
            fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag(ViewPagerFragment.class.getName()))
                    .commit();
        }
    }
}
