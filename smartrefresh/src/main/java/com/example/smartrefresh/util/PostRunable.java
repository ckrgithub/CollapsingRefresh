package com.example.smartrefresh.util;

import android.util.Log;

import java.lang.ref.WeakReference;

public class PostRunable implements Runnable {
    private WeakReference<Runnable> runnableWeakReference = null;
    public PostRunable(Runnable runnable) {
        this.runnableWeakReference = new WeakReference<>(runnable);
    }
    @Override
    public void run() {
        Runnable runnable = runnableWeakReference.get();
        Log.d("SmartRefreshLayout", "run,runnable: "+runnable);
        if (runnable != null) {
            runnable.run();
        }
        runnableWeakReference = null;
    }
}