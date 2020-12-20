package cn.hzw.doodle;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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

    @Override
    public void resetBounds(Rect rect) {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        /*if (textRect != null) {
            rect.set(textRect);
        } else {
            mPaint.getTextBounds(mText, 0, mText.length(), rect);
            rect.offset(0, rect.height());
        }*/

        int screenWdith = Resources.getSystem().getDisplayMetrics().widthPixels;
        int padding = (int) (Resources.getSystem().getDisplayMetrics().density * 12);
        int margin = (int) (Resources.getSystem().getDisplayMetrics().density * 17);
        int maxWidth = Math.round(screenWdith - (padding * 2) - margin * 2);
        layout = new StaticLayout(mText, (TextPaint) mPaint, screenWdith, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        /*int width = 0;
        for (int i = 0; i < layout.getLineCount(); i++) {
            width = (int) (Math.max(width, layout.getLineWidth(i)) + 0.5f);
        }*/
        //rect = new Rect(0,0,width,layout.getHeight());
        rect.set(0,0,screenWdith,layout.getHeight());
        rect.offset(0, rect.height());
    }

    @Override
    public void doDraw(Canvas canvas) {
        getColor().config(this, mPaint);
        mPaint.setTextSize(getSize());
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.translate(0, getBounds().height() / getScale());
        if (layout == null) {
            canvas.drawText(mText, 0, 0, mPaint);
        } else {
            layout.draw(canvas);
        }
        canvas.restore();
    }


    @Override
    public void insert(ViewGroup viewGroup) {
        TextView textView = new TextView(viewGroup.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        /*android:paddingTop="@dimen/dp_4"
        android:paddingBottom="@dimen/dp_4"
        android:paddingLeft="@dimen/dp_12"
        android:paddingRight="@dimen/dp_12"*/
        int paddingTop = DimenUtils.dp2px(viewGroup.getContext(), 4);
        int paddingLeft = DimenUtils.dp2px(viewGroup.getContext(), 12);
        textView.setPadding(paddingLeft, paddingTop, paddingLeft, paddingTop);
        textView.setLayoutParams(params);
        textView.setText(mText);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getSize());
        DoodleColor doodleColor = (DoodleColor) getColor();
        textView.setTextColor(doodleColor.getColor());
        viewGroup.addView(textView);
    }
}


