package cn.forward.androids.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * 直接在布局文件设置字体颜色遮罩和背景selector
 * Created by Administrator on 2017/1/1.
 */

public class SRelativeLayout extends RelativeLayout {


    public SRelativeLayout(Context context) {
        this(context, null);
    }

    public SRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs);
    }

    private void init(AttributeSet attrs) {

        SelectorAttrs.obtainsAttrs(getContext(), this, attrs);
    }

}