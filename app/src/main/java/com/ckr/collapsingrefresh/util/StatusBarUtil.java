package com.ckr.collapsingrefresh.util;

import android.content.Context;

import java.lang.reflect.Field;


/**
 * Created by PC大佬 on 2018/2/9.
 */
public class StatusBarUtil {

    /**
     * @param context
     * @return int
     * @Title: getStatusBarHeight
     * @Description: 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }


}
