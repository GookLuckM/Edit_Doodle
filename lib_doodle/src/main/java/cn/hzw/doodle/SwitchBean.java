package cn.hzw.doodle;

import android.graphics.RectF;

/**
 * maoning
 * 2020-12-31 02:09
 */
public class SwitchBean {

    public SwitchBean() {
    }

    public SwitchBean(float scale, float transX, float transY, RectF rectF) {
        this.scale = scale;
        mTransX = transX;
        mTransY = transY;
        mRectF = rectF;
    }

    public float scale;
    public float mTransX;
    public float mTransY;
    public RectF mRectF;
}
