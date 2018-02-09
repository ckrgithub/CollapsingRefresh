package com.ckr.collapsingrefresh.util;

import android.content.Context;
import android.util.DisplayMetrics;

import com.ckr.collapsingrefresh.BaseApplication;

import java.lang.reflect.Field;


/**
 * TODO<屏幕分辨率工具类>
 *
 * @author cjl
 * @data: 2015年8月23日 下午4:23:50
 * @version: V1.0
 */
public class ScreenUtil {

    /**
     * 屏幕宽高
     *
     * @return
     */
    private static int getScreenSize(boolean width) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = BaseApplication.getInstance().getApplicationContext()
                .getResources().getDisplayMetrics();
        if (width) {
            return dm.widthPixels;
        } else {
            return dm.heightPixels;
        }
    }

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

    /**
     * @param @return 设定文件
     * @return int 返回类型
     * @throws
     * @Title: getScreenWidth
     * @Description: 获取手机屏幕的宽度
     */
    public static int getScreenWidth() {
        return getScreenSize(true);
    }

    /**
     * @param @return 设定文件
     * @return int 返回类型
     * @throws
     * @Title: getScreenHeight
     * @Description: 获取手机屏幕的高度
     */
    public static int getScreenHeight() {
        return getScreenSize(false);
    }

    /**
     * 根据手机分辨率将dp转为px单位
     */
    public static int dp2px(float dpValue) {
        final float scale = BaseApplication.getInstance().getResources()
                .getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dp(float pxValue) {
        final float scale = BaseApplication.getInstance().getResources()
                .getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


}
