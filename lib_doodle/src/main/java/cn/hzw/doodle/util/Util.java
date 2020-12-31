package cn.hzw.doodle.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import java.io.Closeable;
import java.lang.reflect.Field;

/**
 * Created by huangziwei on 16-3-8.
 */
public class Util {

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int dp2px(Context context, float dp) {
        return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    public static void saveProperty(SharedPreferences sharedPreferences, String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void saveProperty(SharedPreferences sharedPreferences, String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void saveProperty(SharedPreferences sharedPreferences, String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void clearProperties(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static void setToolbarPaddingTop(Activity activity, View view, int padingTop) {
        int statusBarHeight = getStatusBarHeight(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            params.setMasrgins(0, dip2px(activity, padingTop) + statusBarHeight, 0, 0);
            view.setPadding(0, DimenUtils.dp2px(activity, padingTop) + statusBarHeight, 0, 0);
        } else {
            view.setPadding(0, DimenUtils.dp2px(activity, padingTop), 0, 0);
        }

    }

    /**
     * 获取状态栏高度
     *
     * @param activity
     * @return
     */
    public static int getStatusBarHeight(Activity activity) {
        int statusBarHeight = 0;
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = activity.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
            statusBarHeight = 75;
        }

        return statusBarHeight;
    }
}
