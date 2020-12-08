package cn.hzw.doodle.util;

import android.content.Context;
import android.util.TypedValue;

public class DimenUtils {

    public static int dpToPx(Context context,int dp){
        return TypedValue.complexToDimensionPixelOffset(dp,context.getResources().getDisplayMetrics());
    }
}
