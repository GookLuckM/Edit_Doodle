package cn.hzw.doodle.util;

import android.content.Context;
import android.util.TypedValue;

public class DimenUtils {

    public static int dp2px(Context context, int dp){
            return (int) (context.getResources().getDisplayMetrics().density * dp );
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
