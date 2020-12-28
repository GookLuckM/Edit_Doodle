package cn.hzw.doodle;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.util.DimenUtils;

/**
 * 文字item
 * Created by huangziwei on 2017/3/16.
 */

public class DoodleText extends DoodleRotatableItemBase {

    private Rect mRect = new Rect();
    private final Paint mPaint = new TextPaint();
    private String mText;
    private boolean isShowTextBg;
    private Rect textRect;
    private StaticLayout layout;
    private Paint bgPaint = new Paint();
    private int alignmentMode;


    public DoodleText(IDoodle doodle, String text, float size, IDoodleColor color, float x, float y, boolean isShowTextBg) {
        super(doodle, -doodle.getDoodleRotation(), x, y);
        this.isShowTextBg = isShowTextBg;
        textRect = doodle.getTextRect();
        setPen(DoodlePen.TEXT);
        mText = text;
        setSize(size);
        setColor(color);
        setLocation(x, y);
    }

    public DoodleText(IDoodle doodle, String text, float size, IDoodleColor color, float x, float y, boolean isShowTextBg,int alignmentMode) {
        super(doodle, -doodle.getDoodleRotation(), x, y);
        this.isShowTextBg = isShowTextBg;
        textRect = doodle.getTextRect();
        this.alignmentMode = alignmentMode;
        setPen(DoodlePen.TEXT);
        mText = text;
        setSize(size);
        setColor(color);
        setLocation(x, y);
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
        resetBounds(mRect);
        setPivotX(getLocation().x + mRect.width() / 2);
        setPivotY(getLocation().y + mRect.height() / 2);
        resetBoundsScaled(getBounds());

        refresh();
    }

    public void setAlignmentMode(){
        this.alignmentMode = alignmentMode;
        refresh();
    }

    @Override
    public void resetBounds(Rect rect) {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        int screenWdith = Resources.getSystem().getDisplayMetrics().widthPixels;
        int padding = (int) (Resources.getSystem().getDisplayMetrics().density * 12);
        int margin = (int) (Resources.getSystem().getDisplayMetrics().density * 17);
        int maxWidth = Math.round(screenWdith - (padding * 2) - margin * 2);
        float v = mPaint.measureText(mText);
        if (v < maxWidth){
            maxWidth = (int) (v + 0.5f);
        }

        Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
        switch (alignmentMode){
            case 0:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
            case 1:
                alignment = Layout.Alignment.ALIGN_CENTER;
                break;
            case 2:
                alignment = Layout.Alignment.ALIGN_OPPOSITE;
                break;
        }

        layout = new StaticLayout(mText, (TextPaint) mPaint, maxWidth, alignment, 1.0F, 0.0F, false);
        rect.set(0,0,maxWidth,layout.getHeight());
        rect.offset(0, rect.height());
        mRect.set(rect);
    }

    @Override
    public void doDraw(Canvas canvas) {

        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        if (isShowTextBg){
            RectF tempRect = new RectF();
            tempRect.left = mRect.left - 8*getDoodle().getUnitSize() * getScale();
            tempRect.top = mRect.top - 8*getDoodle().getUnitSize() * getScale();
            tempRect.right = mRect.right + 8*getDoodle().getUnitSize() * getScale();
            tempRect.bottom = mRect.bottom + 8*getDoodle().getUnitSize() * getScale();
            bgPaint.setColor(getColor().getColor());
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(tempRect, 9*getDoodle().getUnitSize(), 9*getDoodle().getUnitSize(), bgPaint);
            mPaint.setColor(Color.WHITE);
        }else{
            getColor().config(this, mPaint);
        }
        canvas.translate(0, getBounds().height() / getScale());
        if (layout == null) {
            canvas.drawText(mText, 0, 0, mPaint);
        } else {
            layout.draw(canvas);
        }
        canvas.restore();
    }


}


