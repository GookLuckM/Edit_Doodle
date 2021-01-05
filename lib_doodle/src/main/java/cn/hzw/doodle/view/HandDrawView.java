package cn.hzw.doodle.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.hzw.doodle.DoodleView;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleTouchDetector;

class HandDrawView extends View {


    public HandDrawView(Context context) {
        super(context);

        // 关闭硬件加速，某些绘图操作不支持硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /*public boolean onTouchEvent(MotionEvent event) {
        // 綁定的识别器
        IDoodleTouchDetector detector = mTouchDetectorMap.get(mPen);
        if (detector != null) {
            return detector.onTouchEvent(event);
        }
        // 默认识别器
        if (mDefaultTouchDetector != null) {
            return mDefaultTouchDetector.onTouchEvent(event);
        }
        return false;
    }

    protected void onDraw(Canvas canvas) {
        int count = canvas.save();
        canvas.rotate(mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
        doDraw(canvas);
        canvas.restoreToCount(count);
    }

    private void doDraw(Canvas canvas) {
        if (isJustDrawOriginal) { // 只绘制原图
            return;
        }

        float left = getAllTranX();
        float top = getAllTranY();

        // 画布和图片共用一个坐标系，只需要处理屏幕坐标系到图片（画布）坐标系的映射关系
        canvas.translate(left, top); // 偏移画布
        float scale = getAllScale();
        canvas.scale(scale, scale); // 缩放画布

        Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;

        if (mCropRect != null && !mCropRect.isEmpty()) {
            canvas.clipRect(mBitmapCropRect);
        }

        int saveCount = canvas.save(); // 1
        List<IDoodleItem> items = new ArrayList<>(mItemStack);
        List<IDoodleItem> mosaicItems = new ArrayList<>(mMosaicItemStack);
        //List<IDoodleItem> textItems = new ArrayList<>(mTextItemStack);
        if (mOptimizeDrawing) {
            items = mItemStackOnViewCanvas;
            mosaicItems = mMosaicItemStackOnViewCanvas;
            //textItems = mTextItemStackOnViewCanvas;

        }
        boolean canvasClipped = false;
        if (!mIsDrawableOutside) { // 裁剪绘制区域为图片区域
            canvasClipped = true;
            if (mCropRect != null && !mCropRect.isEmpty()) {
                canvas.clipRect(mBitmapCropRect);
            } else {
                canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            }
        }

        for (IDoodleItem item : mosaicItems) {
            if (!item.isNeedClipOutside()) { // 1.不需要裁剪
                if (canvasClipped) {
                    canvas.restore();
                }
                item.draw(canvas);
                if (canvasClipped) { // 2.恢复裁剪
                    canvas.save();
                    if (mCropRect != null && !mCropRect.isEmpty()) {
                        canvas.clipRect(mBitmapCropRect);
                    } else {
                        canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }

                }
            } else {
                item.draw(canvas);
            }
        }


        for (IDoodleItem item : items) {
            if (!item.isNeedClipOutside()) { // 1.不需要裁剪
                if (canvasClipped) {
                    canvas.restore();
                }
                item.draw(canvas);
                if (canvasClipped) { // 2.恢复裁剪
                    canvas.save();
                    if (mCropRect != null && !mCropRect.isEmpty()) {
                        canvas.clipRect(mBitmapCropRect);
                    } else {
                        canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }

                }
            } else {
                item.draw(canvas);
            }
        }


        if (mPen != null) {
            mPen.drawHelpers(canvas, DoodleView.this);
        }
        if (mShape != null) {
            mShape.drawHelpers(canvas, DoodleView.this);
        }
    }*/
}

