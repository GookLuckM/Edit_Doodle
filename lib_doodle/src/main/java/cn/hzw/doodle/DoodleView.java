/*
  MIT License

  Copyright (c) 2018 huangziwei

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 *
 */

package cn.hzw.doodle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;
import android.os.AsyncTask;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodlePen;
import cn.hzw.doodle.core.IDoodleShape;
import cn.hzw.doodle.core.IDoodleTouchDetector;
import cn.hzw.doodle.util.DimenUtils;
import cn.hzw.doodle.util.ImageUtils;
import cn.hzw.doodle.util.LogUtil;

import static cn.hzw.doodle.util.DrawUtil.rotatePoint;

/**
 * 涂鸦框架
 * Created by huangziwei on 2016/9/3.
 */
public class DoodleView extends FrameLayout implements IDoodle {

    public static final String TAG = "DoodleView";
    public final static float MAX_SCALE = 5f; // 最大缩放倍数
    public final static float MIN_SCALE = 1f; // 最小缩放倍数
    public final static int DEFAULT_SIZE = 7; // 默认画笔大小

    public static final int ERROR_INIT = -1;
    public static final int ERROR_SAVE = -2;

    private static final int FLAG_RESET_BACKGROUND = 1 << 1;
    public static final int FLAG_DRAW_PENDINGS_TO_BACKGROUND = 1 << 2;
    private static final int FLAG_REFRESH_BACKGROUND = 1 << 3;

    private IDoodleListener mDoodleListener;

    private final Bitmap mBitmap; // 当前涂鸦的原图

    private float mCenterScale; // 图片适应屏幕时的缩放倍数
    private int mCenterHeight, mCenterWidth;// 图片适应屏幕时的大小（View窗口坐标系上的大小）
    private float mCentreTranX, mCentreTranY;// 图片在适应屏幕时，位于居中位置的偏移（View窗口坐标系上的偏移）

    private int mDoodleRotateDegree = 0; // 相对于初始图片旋转的角度
    private float mRotateScale = 1;  // 在旋转后适应屏幕时的缩放倍数
    private float mRotateTranX, mRotateTranY; // 旋转后适应屏幕居中时的偏移


    private float mScale = 1f; // 在适应屏幕时的缩放基础上的缩放倍数 （ 图片真实的缩放倍数为 mCenterScale*mScale ）
    private float mTransX = 0, mTransY = 0; // 图片在适应屏幕且处于居中位置的基础上的偏移量（ 图片真实偏移量为mCentreTranX + mTransX，View窗口坐标系上的偏移）
    private float mMinScale = MIN_SCALE; // 最小缩放倍数
    private float mMaxScale = MAX_SCALE; // 最大缩放倍数

    private float mSize;
    private int mSizeIndex;
    private IDoodleColor mColor; // 画笔底色



    private boolean isJustDrawOriginal; // 是否只绘制原图

    private boolean mIsDrawableOutside = false; // 触摸时，图片区域外是否绘制涂鸦轨迹
    private boolean mReady = false;

    // 保存涂鸦操作，便于撤销
    private List<IDoodleItem> mItemStack = new ArrayList<>();
    private List<IDoodleItem> mShapeStack = new ArrayList<>();
    private List<IDoodleItem> mMosaicItemStack = new ArrayList<>();
    private List<IDoodleItem> mTextItemStack = new ArrayList<>();
    private List<IDoodleItem> mMosaicRedoItemStack = new ArrayList<>();
    private List<IDoodleItem> mTextRedoItemStack = new ArrayList<>();
    private List<IDoodleItem> mUnDoItemStack = new ArrayList<>();
    private List<IDoodleItem> mReDoItemStack = new ArrayList<>();

    private IDoodlePen mPen;
    private IDoodleShape mShape;

    private RectF mCropRect;
    private RectF mPreCropRect;
    private Rect mPreBitmapRect;


    private boolean mIsScrollingDoodle = false; // 是否正在滑动，只要用于标志触摸时才显示放大镜
    private boolean mIsScaleDoodle = false; // 是否正在滑动，只要用于标志触摸时才显示放大镜

    private float mDoodleSizeUnit = 1; // 长度单位，不同大小的图片的长度单位不一样。该单位的意义同dp的作用类似，独立于图片之外的单位长度


    // 手势相关
    private IDoodleTouchDetector mDefaultTouchDetector;
    private Map<IDoodlePen, IDoodleTouchDetector> mTouchDetectorMap = new HashMap<>();


    private BackgroundView mBackgroundView;
    private ForegroundView mForegroundView;
    private ShapeView shapeView;
    private TextView textView;



    private RectF mDoodleBound = new RectF();
    private PointF mTempPoint = new PointF();



    private boolean mIsEditMode = false; //是否是编辑模式，可移动缩放涂鸦
    private boolean mIsSaving = false;


    /**
     * Whether or not to optimize drawing, it is suggested to open, which can optimize the drawing speed and performance.
     * Note: When item is selected for editing after opening, it will be drawn at the top level, and not at the corresponding level until editing is completed.
     * 是否优化绘制，建议开启，可优化绘制速度和性能.
     * 注意：开启后item被选中编辑时时会绘制在最上面一层，直到结束编辑后才绘制在相应层级
     **/
    private final boolean mOptimizeDrawing; // 涂鸦及时绘制在图片上，优化性能
    private List<IDoodleItem> mItemStackOnViewCanvas = new ArrayList<>(); // 这些item绘制在View的画布上，而不是在图片Bitmap.比如正在创建或选中的item
    private List<IDoodleItem> mShapeStackOnViewCanvas = new ArrayList<>(); // 这些item绘制在View的画布上，而不是在图片Bitmap.比如正在创建或选中的item
    private List<IDoodleItem> mMosaicItemStackOnViewCanvas = new ArrayList<>(); // 这些item绘制在View的画布上，而不是在图片Bitmap.比如正在创建或选中的item
    private List<IDoodleItem> mTextItemStackOnViewCanvas = new ArrayList<>(); // 这些item绘制在View的画布上，而不是在图片Bitmap.比如正在创建或选中的item
    private List<IDoodleItem> mPendingItemsDrawToBitmap = new ArrayList<>();
    private List<IDoodleItem> mMosaicPendingItemsDrawToBitmap = new ArrayList<>();
    private Bitmap mDoodleBitmap;
    private int mFlags = 0;
    private Canvas mDoodleBitmapCanvas;

    private Rect mBitmapCropRect;



    public DoodleView(Context context, Bitmap bitmap, IDoodleListener listener) {
        this(context, bitmap, false, listener, null);
    }


    public DoodleView(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener) {
        this(context, bitmap, optimizeDrawing, listener, null);
    }

    /**
     * 如果开启
     *
     * @param context
     * @param bitmap
     * @param optimizeDrawing 是否优化绘制，开启后涂鸦会及时绘制在图片上，以此优化绘制速度和性能.
     *                        如果开启了优化绘制，当绘制或编辑某个item时需要调用 {@link #markItemToOptimizeDrawing(IDoodleItem)}，无需再调用{@link #addItem(IDoodleItem)}.
     *                        另外结束时需要调用对应的 {@link #notifyItemFinishedDrawing(IDoodleItem)}。
     *                        {@link #mOptimizeDrawing}
     * @param listener
     * @param defaultDetector 默认手势识别器
     */
    public DoodleView(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener, IDoodleTouchDetector defaultDetector) {
        super(context);
        setClipChildren(false);

        mBitmap = bitmap;
        if (mBitmap.getConfig() != Bitmap.Config.RGB_565) {
            // 如果位图包含透明度，则可能会导致橡皮擦无法对透明部分进行擦除
            LogUtil.w(TAG, "the bitmap may contain alpha, which will cause eraser don't work well.");
        }
        mDoodleListener = listener;
        if (mDoodleListener == null) {
            throw new RuntimeException("IDoodleListener is null!!!");
        }
        if (mBitmap == null) {
            throw new RuntimeException("Bitmap is null!!!");
        }

        mOptimizeDrawing = optimizeDrawing;

        mScale = 1f;

        mDefaultTouchDetector = defaultDetector;

        mBackgroundView = new BackgroundView(context);
        mForegroundView = new ForegroundView(context);
        shapeView = new ShapeView(context);
        textView = new TextView(context);
        addView(mBackgroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mForegroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(shapeView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(textView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
        if (!mReady) {
            mDoodleListener.onReady(this);
            mReady = true;
        }
    }

    private Matrix mTouchEventMatrix = new Matrix();
    private OnTouchListener mOnTouchListener;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchListener != null) {
            if (mOnTouchListener.onTouch(this, event)) {
                return true;
            }
        }

        // 把事件转发给innerView，避免在区域外不可点击
        MotionEvent transformedEvent = MotionEvent.obtain(event);
//        final float offsetX = mForegroundView.getScrollX() - mForegroundView.getLeft();
//        final float offsetY = mForegroundView.getScrollY() - mForegroundView.getTop();
//        transformedEvent.offsetLocation(offsetX, offsetY);
        mTouchEventMatrix.reset();
        mTouchEventMatrix.setRotate(-mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
        transformedEvent.transform(mTouchEventMatrix);
        boolean handled = mForegroundView.onTouchEvent(transformedEvent);
        transformedEvent.recycle();

        return handled;
    }


    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mOnTouchListener = l;
        super.setOnTouchListener(l);
    }

    private void init() {// 不用resize preview
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        if (nw > nh) {
            mCenterScale = 1 / nw;
            mCenterWidth = getWidth();
            mCenterHeight = (int) (h * mCenterScale);
        } else {
            mCenterScale = 1 / nh;
            mCenterWidth = (int) (w * mCenterScale);
            mCenterHeight = getHeight();
        }
        // 使图片居中
        mCentreTranX = (getWidth() - mCenterWidth) / 2f;
        mCentreTranY = (getHeight() - mCenterHeight) / 2f;


        mDoodleSizeUnit = DimenUtils.dp2px(getContext(), 1) / mCenterScale;

        if (!mReady) { // 只有初始化时才需要设置画笔大小
            mSize = DEFAULT_SIZE * mDoodleSizeUnit;
        }
        // 居中适应屏幕
        mTransX = mTransY = 0;
        mScale = 1;

        initDoodleBitmap();

        refreshWithBackground();
    }

    private void initDoodleBitmap() {
        if (!mOptimizeDrawing) {
            return;
        }

        if (mDoodleBitmap != null) {
            mDoodleBitmap.recycle();
        }
        mDoodleBitmap = mBitmap.copy(mBitmap.getConfig(), true);
        mDoodleBitmapCanvas = new Canvas(mDoodleBitmap);
    }


    private void resetDoodleBitmap() {
        RectF doodleBound = getDoodleBound();
        if (mBitmapCropRect == null) {
            mBitmapCropRect = new Rect();
        }
        if (mCropRect != null && !mCropRect.isEmpty()) {
            switch (mDoodleRotateDegree) {
                case 0:
                    mBitmapCropRect.left = (int) ((mCropRect.left - doodleBound.left) / getAllScale());
                    mBitmapCropRect.top = (int) ((mCropRect.top - doodleBound.top) / getAllScale());
                    mBitmapCropRect.right = (int) ((mCropRect.right - doodleBound.left) / getAllScale());
                    mBitmapCropRect.bottom = (int) ((mCropRect.bottom - doodleBound.top) / getAllScale());
                    break;
                case 90:
                    mBitmapCropRect.left = (int) ((mCropRect.top - doodleBound.top) / getAllScale());
                    mBitmapCropRect.top = (int) ((doodleBound.right - mCropRect.right) / getAllScale());
                    mBitmapCropRect.right = (int) (mBitmapCropRect.left + mCropRect.height() / getAllScale());
                    mBitmapCropRect.bottom = (int) (mBitmapCropRect.top + mCropRect.width() / getAllScale());
                    break;
                case 180:
                    mBitmapCropRect.left = (int) ((doodleBound.right - mCropRect.right) / getAllScale());
                    mBitmapCropRect.top = (int) ((doodleBound.bottom - mCropRect.bottom) / getAllScale());
                    mBitmapCropRect.right = (int) (mBitmapCropRect.left + mCropRect.width() / getAllScale());
                    mBitmapCropRect.bottom = (int) (mBitmapCropRect.top + mCropRect.height() / getAllScale());
                    break;
                case 270:
                    mBitmapCropRect.left = (int) ((doodleBound.bottom - mCropRect.bottom) / getAllScale());
                    mBitmapCropRect.top = (int) ((mCropRect.left - doodleBound.left) / getAllScale());
                    mBitmapCropRect.right = (int) (mBitmapCropRect.left + mCropRect.height() / getAllScale());
                    mBitmapCropRect.bottom = (int) (mBitmapCropRect.top + mCropRect.width() / getAllScale());
                    break;
            }

        } else {
            mBitmapCropRect.left = (int) (toX(doodleBound.left));
            mBitmapCropRect.right = (int) (toX(doodleBound.right));
            mBitmapCropRect.top = (int) (toY(doodleBound.top));
            mBitmapCropRect.bottom = (int) (toY(doodleBound.bottom));
        }
    }


    /**
     * 获取当前图片在View坐标系中的矩型区域
     *
     * @return
     */
    public RectF getDoodleBound() {
        float width = mCenterWidth * mRotateScale * mScale;
        float height = mCenterHeight * mRotateScale * mScale;
        if (mDoodleRotateDegree % 90 == 0) { // 对0,90,180，270度旋转做简化计算
            if (mDoodleRotateDegree == 0) {
                mTempPoint.x = toTouchX(0);
                mTempPoint.y = toTouchY(0);
            } else if (mDoodleRotateDegree == 90) {
                mTempPoint.x = toTouchX(0);
                mTempPoint.y = toTouchY(mBitmap.getHeight());
                float t = width;
                width = height;
                height = t;
            } else if (mDoodleRotateDegree == 180) {
                mTempPoint.x = toTouchX(mBitmap.getWidth());
                mTempPoint.y = toTouchY(mBitmap.getHeight());
            } else if (mDoodleRotateDegree == 270) {
                mTempPoint.x = toTouchX(mBitmap.getWidth());
                mTempPoint.y = toTouchY(0);
                float t = width;
                width = height;
                height = t;
            }
            rotatePoint(mTempPoint, mDoodleRotateDegree, mTempPoint.x, mTempPoint.y, getWidth() / 2, getHeight() / 2);
            mDoodleBound.set(mTempPoint.x, mTempPoint.y, mTempPoint.x + width, mTempPoint.y + height);
        } else {
            // 转换成屏幕坐标
            // 左上
            float ltX = toTouchX(0);
            float ltY = toTouchY(0);
            //右下
            float rbX = toTouchX(mBitmap.getWidth());
            float rbY = toTouchY(mBitmap.getHeight());
            // 左下
            float lbX = toTouchX(0);
            float lbY = toTouchY(mBitmap.getHeight());
            //右上
            float rtX = toTouchX(mBitmap.getWidth());
            float rtY = toTouchY(0);

            //转换到View坐标系
            rotatePoint(mTempPoint, mDoodleRotateDegree, ltX, ltY, getCentreTranX() + getTranslationX() + width / 2, getCentreTranY() + getTranslationY() + height / 2);
            ltX = mTempPoint.x;
            ltY = mTempPoint.y;
            rotatePoint(mTempPoint, mDoodleRotateDegree, rbX, rbY, getCentreTranX() + getTranslationX() + width / 2, getCentreTranY() + getTranslationY() + height / 2);
            rbX = mTempPoint.x;
            rbY = mTempPoint.y;
            rotatePoint(mTempPoint, mDoodleRotateDegree, lbX, lbY, getCentreTranX() + getTranslationX() + width / 2, getCentreTranY() + getTranslationY() + height / 2);
            lbX = mTempPoint.x;
            lbY = mTempPoint.y;
            rotatePoint(mTempPoint, mDoodleRotateDegree, rtX, rtY, getCentreTranX() + getTranslationX() + width / 2, getCentreTranY() + getTranslationY() + height / 2);
            rtX = mTempPoint.x;
            rtY = mTempPoint.y;

            mDoodleBound.left = Math.min(Math.min(ltX, rbX), Math.min(lbX, rtX));
            mDoodleBound.top = Math.min(Math.min(ltY, rbY), Math.min(lbY, rtY));
            mDoodleBound.right = Math.max(Math.max(ltX, rbX), Math.max(lbX, rtX));
            mDoodleBound.bottom = Math.max(Math.max(ltY, rbY), Math.max(lbY, rtY));
        }
        return mDoodleBound;
    }



    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mBitmap.isRecycled()) {
            return;
        }

        if (hasFlag(FLAG_RESET_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_RESET_BACKGROUND");
            clearFlag(FLAG_RESET_BACKGROUND);
            clearFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);
            clearFlag(FLAG_REFRESH_BACKGROUND);
            refreshDoodleBitmap(false);
            mPendingItemsDrawToBitmap.clear();
            mBackgroundView.invalidate();
        } else if (hasFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_DRAW_PENDINGS_TO_BACKGROUND");
            clearFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);
            clearFlag(FLAG_REFRESH_BACKGROUND);
            drawToDoodleBitmap(mMosaicPendingItemsDrawToBitmap);
            drawToDoodleBitmap(mPendingItemsDrawToBitmap);
            mMosaicPendingItemsDrawToBitmap.clear();
            mPendingItemsDrawToBitmap.clear();
            mBackgroundView.invalidate();
        } else if (hasFlag(FLAG_REFRESH_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_REFRESH_BACKGROUND");
            clearFlag(FLAG_REFRESH_BACKGROUND);
            mBackgroundView.invalidate();
        }

        int count = canvas.save();
        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);

    }

    private boolean hasFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    public void addFlag(int flag) {
        mFlags = mFlags | flag;
    }

    private void clearFlag(int flag) {
        mFlags = mFlags & ~flag;
    }

    public float getAllScale() {
        return mCenterScale * mRotateScale * mScale;
    }

    public float getAllTranX() {
        return mCentreTranX + mRotateTranX + mTransX;
    }

    public float getAllTranY() {
        return mCentreTranY + mRotateTranY + mTransY;
    }

    /**
     * 将屏幕触摸坐标x转换成在图片中的坐标
     */
    public final float toX(float touchX) {
        return (touchX - getAllTranX()) / getAllScale();
    }

    /**
     * 将屏幕触摸坐标y转换成在图片中的坐标
     */
    public final float toY(float touchY) {
        return (touchY - getAllTranY()) / getAllScale();
    }

    /**
     * 将图片坐标x转换成屏幕触摸坐标
     */
    public final float toTouchX(float x) {
        return x * getAllScale() + getAllTranX();
    }

    /**
     * 将图片坐标y转换成屏幕触摸坐标
     */
    public final float toTouchY(float y) {
        return y * getAllScale() + getAllTranY();
    }

    /**
     * 坐标换算
     * （公式由toX()中的公式推算出）
     *
     * @param touchX  触摸坐标
     * @param doodleX 在涂鸦图片中的坐标
     * @return 偏移量
     */
    public final float toTransX(float touchX, float doodleX) {
        return -doodleX * getAllScale() + touchX - mCentreTranX - mRotateTranX;
    }

    public final float toTransY(float touchY, float doodleY) {
        return -doodleY * getAllScale() + touchY - mCentreTranY - mRotateTranY;
    }

    /**
     * 根据画笔绑定手势识别器
     *
     * @param pen
     * @param detector
     */
    public void bindTouchDetector(IDoodlePen pen, IDoodleTouchDetector detector) {
        if (pen == null) {
            return;
        }
        mTouchDetectorMap.put(pen, detector);
    }

    /**
     * 获取画笔绑定的手势识别器
     *
     * @param pen
     */
    public IDoodleTouchDetector getDefaultTouchDetector(IDoodlePen pen) {
        return mTouchDetectorMap.get(pen);
    }

    /**
     * 移除指定画笔的手势识别器
     *
     * @param pen
     */
    public void removeTouchDetector(IDoodlePen pen) {
        if (pen == null) {
            return;
        }
        mTouchDetectorMap.remove(pen);
    }

    /**
     * 设置默认手势识别器
     *
     * @param touchGestureDetector
     */
    public void setDefaultTouchDetector(IDoodleTouchDetector touchGestureDetector) {
        mDefaultTouchDetector = touchGestureDetector;
    }

    /**
     * 默认手势识别器
     *
     * @return
     */
    public IDoodleTouchDetector getDefaultTouchDetector() {
        return mDefaultTouchDetector;
    }

    private void drawToDoodleBitmap(List<IDoodleItem> items) {
        if (!mOptimizeDrawing) {
            return;
        }

        for (IDoodleItem item : items) {
            item.draw(mDoodleBitmapCanvas);
        }
    }

    private void refreshDoodleBitmap(boolean drawAll) {
        if (!mOptimizeDrawing) {
            return;
        }

        initDoodleBitmap();
        List<IDoodleItem> items = null;
        List<IDoodleItem> mosaicItems = null;
        if (drawAll) {
            items = mItemStack;
            mosaicItems = mMosaicItemStack;
        } else {
            items = new ArrayList<>(mItemStack);
            mosaicItems = new ArrayList<>(mMosaicItemStack);
            items.removeAll(mItemStackOnViewCanvas);
            mosaicItems.removeAll(mMosaicItemStackOnViewCanvas);
        }

        for (IDoodleItem item : mosaicItems) {
            item.draw(mDoodleBitmapCanvas);
        }

        for (IDoodleItem item : items) {
            item.draw(mDoodleBitmapCanvas);
        }

    }

    private void refreshWithBackground() {
        addFlag(FLAG_REFRESH_BACKGROUND);
        refresh();
    }

    // ========================= api ================================

    @Override
    public void invalidate() {
        refresh();
    }

    @Override
    public void refresh() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
            mForegroundView.invalidate();
            shapeView.invalidate();
            textView.invalidate();
        } else {
            super.postInvalidate();
            mForegroundView.postInvalidate();
            shapeView.postInvalidate();
            textView.postInvalidate();
        }
    }

    @Override
    public int getDoodleRotation() {
        return mDoodleRotateDegree;
    }

    @Override
    public void setDoodleCropRect(RectF rect, int type) {
        if (rect != null) {
            //EditPhotoActivity onDone方法调用后,确定裁剪时传入cropRect
            if (mCropRect == null) {
                this.mCropRect = new RectF(rect);
            } else {
                mCropRect.set(rect);
            }
            resetDoodleBitmap();
        } else {
            if (type == 1) {
                //重新进入裁剪界面,需要显示全部图片
                // 由于可能取消裁剪 因为记录上次裁剪范围
                mPreCropRect = new RectF(mCropRect);
                mPreBitmapRect = new Rect(mBitmapCropRect);
                mCropRect = null;
                mBitmapCropRect = null;
            } else {
                //裁剪未保存 点击X退出时调用
                //恢复到上次裁剪的状态
                mCropRect = new RectF(mPreCropRect);
                mBitmapCropRect = new Rect(mPreBitmapRect);
            }
        }

    }

    @Override
    public RectF getDoodleCropRect() {
        return mCropRect;
    }

    /**
     * 相对于初始图片旋转的角度
     *
     * @param degree positive degree means rotate right, negative degree means rotate left
     */

    @Override
    public void setDoodleRotation(int degree) {
        mDoodleRotateDegree = degree;
        mDoodleRotateDegree = mDoodleRotateDegree % 360;
        if (mDoodleRotateDegree < 0) {
            mDoodleRotateDegree = 360 + mDoodleRotateDegree;
        }


        // 居中
        RectF rectF = getDoodleBound();
        int w = (int) (rectF.width() / getAllScale());
        int h = (int) (rectF.height() / getAllScale());
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        float scale;
        float tx, ty;
        if (nw > nh) {
            scale = 1 / nw;
        } else {
            scale = 1 / nh;
        }

        int pivotX = mBitmap.getWidth() / 2;
        int pivotY = mBitmap.getHeight() / 2;

        mTransX = mTransY = 0;
        mRotateTranX = mRotateTranY = 0;
        this.mScale = 1;
        mRotateScale = 1;
        float touchX = toTouchX(pivotX);
        float touchY = toTouchY(pivotY);
        mRotateScale = scale / mCenterScale;

        // 缩放后，偏移图片，以产生围绕某个点缩放的效果
        tx = toTransX(touchX, pivotX);
        ty = toTransY(touchY, pivotY);

        mRotateTranX = tx;
        mRotateTranY = ty;

        refreshWithBackground();
    }

    public boolean isOptimizeDrawing() {
        return mOptimizeDrawing;
    }

    /**
     * 标志item绘制在View的画布上，而不是在图片Bitmap. 比如正创建或选中的item. 结束绘制时应调用 {@link #notifyItemFinishedDrawing(IDoodleItem)}
     * 仅在开启优化绘制（mOptimizeDrawing=true）时生效
     *
     * @param item
     */
    public void markItemToOptimizeDrawing(IDoodleItem item) {
        if (!mOptimizeDrawing) {
            return;
        }

        if (mItemStackOnViewCanvas.contains(item) || mMosaicItemStackOnViewCanvas.contains(item) || mTextItemStackOnViewCanvas.contains(item)) {
            throw new RuntimeException("The item has been added");
        }
        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            mItemStackOnViewCanvas.add(item);
            mUnDoItemStack.add(item);
        } else if (item.getPen().equals(DoodlePen.BRUSH)) {
            if (!mShapeStack.contains(item)) {
                mShapeStack.add(item);
            }
            if (!mShapeStackOnViewCanvas.contains(item)) {
                mShapeStackOnViewCanvas.add(item);
            }
            if (!mUnDoItemStack.contains(item)) {
                mUnDoItemStack.add(item);
            }
        } else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicItemStackOnViewCanvas.add(item);
        } else if (item.getPen().equals(DoodlePen.TEXT)) {
            mTextItemStack.add(item);
        }

        if (mItemStack.contains(item) || mMosaicItemStack.contains(item)) {
            addFlag(FLAG_RESET_BACKGROUND);
        }

        refresh();
    }

    /**
     * 把item从View画布中移除并绘制在涂鸦图片上. 对应 {@link #notifyItemFinishedDrawing(IDoodleItem)}
     *
     * @param item
     */
    public void notifyItemFinishedDrawing(IDoodleItem item) {
        if (!mOptimizeDrawing) {
            return;
        }
        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            if (mItemStackOnViewCanvas.remove(item)) {
                if (mItemStack.contains(item)) {
                    addFlag(FLAG_RESET_BACKGROUND);
                } else {
                    addItem(item);
                    return;
                }
            }
        }  else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            if (mMosaicItemStackOnViewCanvas.remove(item)) {
                if (mMosaicItemStack.contains(item)) {
                    addFlag(FLAG_RESET_BACKGROUND);
                } else {
                    addItem(item);
                    return;
                }
            }
        } else if (item.getPen().equals(DoodlePen.TEXT)) {
            if (mTextItemStackOnViewCanvas.remove(item)) {
                addItem(item);
                return;
            }
        }

        refresh();
    }

    /**
     * 保存, 回调DoodleListener.onSaved()的线程和调用save()的线程相同
     */
    @SuppressLint("StaticFieldLeak")
    @Override
    public void save() {
        if (mIsSaving) {
            return;
        }

        mIsSaving = true;

        new AsyncTask<Void, Void, Bitmap>() {

            @SuppressLint("WrongThread")
            @Override
            protected Bitmap doInBackground(Void... voids) {
                Bitmap savedBitmap = null;

                /*if (mOptimizeDrawing) {
                    refreshDoodleBitmap(true);
                    if (mBitmapCropRect == null) {
                        resetDoodleBitmap();
                    }
                    savedBitmap = mDoodleBitmap;

                } else {*/
                savedBitmap = mBitmap.copy(mBitmap.getConfig(), true);
                Canvas canvas = new Canvas(savedBitmap);
                    /*if (mBitmapCropRect != null && !mBitmapCropRect.isEmpty()) {
                        canvas.clipRect(mBitmapCropRect);
                    }*/
                for (IDoodleItem item : mMosaicItemStack) {
                    item.draw(canvas);
                }

                for (IDoodleItem item : mItemStack) {
                    item.draw(canvas);
                }

                for (IDoodleItem item : mShapeStack) {
                    item.draw(canvas);
                }

                for (IDoodleItem item : mTextItemStack) {
                    item.draw(canvas);
                }

                savedBitmap = ImageUtils.rotate(savedBitmap, mDoodleRotateDegree, true);
                if (mBitmapCropRect != null && !mBitmapCropRect.isEmpty()) {
                    Rect tempRect = new Rect();
                    switch (getDoodleRotation()) {
                        case 0:
                            tempRect.left = mBitmapCropRect.left;
                            tempRect.top = mBitmapCropRect.top;
                            tempRect.right = mBitmapCropRect.right;
                            tempRect.bottom = mBitmapCropRect.bottom;
                            break;
                        case 90:
                            tempRect.left = savedBitmap.getWidth() - mBitmapCropRect.bottom;
                            tempRect.top =   mBitmapCropRect.left;
                            tempRect.right = savedBitmap.getWidth() - mBitmapCropRect.top;
                            tempRect.bottom = mBitmapCropRect.right;
                            break;
                        case 180:
                            tempRect.left = savedBitmap.getWidth() - mBitmapCropRect.right;
                            tempRect.top =  savedBitmap.getHeight() -  mBitmapCropRect.bottom;
                            tempRect.right = savedBitmap.getWidth() - mBitmapCropRect.left;
                            tempRect.bottom = savedBitmap.getHeight() -  mBitmapCropRect.top;
                            break;
                        case 270:
                            tempRect.left = mBitmapCropRect.top;
                            tempRect.top =  savedBitmap.getHeight() -  mBitmapCropRect.right;
                            tempRect.right = mBitmapCropRect.bottom;
                            tempRect.bottom = savedBitmap.getHeight() -  mBitmapCropRect.left;
                            break;

                    }

                    savedBitmap = Bitmap.createBitmap(savedBitmap,tempRect.left,tempRect.top,tempRect.width(),tempRect.height());
                }



                return savedBitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mDoodleListener.onSaved(DoodleView.this, bitmap, new Runnable() {
                    @Override
                    public void run() {
                        mIsSaving = false;
                        if (mOptimizeDrawing) {
                            refreshDoodleBitmap(false);
                        }
                        refresh();
                    }
                });
            }
        }.execute();
    }


    @Override
    public boolean undo() {
        List<IDoodleItem> list = new ArrayList<>(mUnDoItemStack);
        for (int i = list.size() - 1; i >= 0; i--) {
            IDoodleItem item = list.get(i);
            removeItem(item);
            mReDoItemStack.add(0, item);
            break;
        }
        return true;

    }

    @Override
    public boolean mosaicUndo() {
        List<IDoodleItem> list = new ArrayList<>(mMosaicItemStack);
        if (mMosaicItemStackOnViewCanvas.size() > 0) {
            list.addAll(mMosaicItemStackOnViewCanvas);
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            IDoodleItem item = list.get(i);
            removeItem(item);
            mMosaicRedoItemStack.add(0, item);
            break;
        }
        return true;

    }


    @Override
    public void cleanDoodle() {
        if (mItemStackOnViewCanvas.size() > 0 || mShapeStackOnViewCanvas.size() > 0) {
            for (IDoodleItem item : mItemStackOnViewCanvas) {
               //mUnDoItemStack.remove(item);
                mItemStack.remove(item);
                //mReDoItemStack.remove(item);
            }
            for (IDoodleItem item : mShapeStackOnViewCanvas) {
                //mUnDoItemStack.remove(item);
                mShapeStack.remove(item);
                //mReDoItemStack.remove(item);
            }
            mItemStackOnViewCanvas.clear();
            mShapeStackOnViewCanvas.clear();
            addFlag(FLAG_RESET_BACKGROUND);
            //refresh();
        }

    }

    public void cleanShapeDrawingStack(){
        mShapeStackOnViewCanvas.clear();
    }

    @Override
    public void cleanMosaic() {
        if (mMosaicItemStackOnViewCanvas.size() > 0) {
            for (IDoodleItem item : mMosaicItemStackOnViewCanvas) {
                mMosaicItemStack.remove(item);
                mMosaicRedoItemStack.remove(item);
            }
            mMosaicItemStackOnViewCanvas.clear();
            addFlag(FLAG_RESET_BACKGROUND);
            refresh();
        }
    }


    @Override
    public boolean redo() {
        if (mReDoItemStack.isEmpty()) {
            return false;
        }
        Iterator<IDoodleItem> iterator = mReDoItemStack.iterator();
        while (iterator.hasNext()) {
            IDoodleItem item = iterator.next();
            iterator.remove();
            redoItemInner(item);
            break;
        }
        return true;
    }

    @Override
    public boolean mosaicRedo() {
        if (mMosaicRedoItemStack.isEmpty()) {
            return false;
        }
        Iterator<IDoodleItem> iterator = mMosaicRedoItemStack.iterator();
        while (iterator.hasNext()) {
            IDoodleItem item = iterator.next();
            iterator.remove();
            redoItemInner(item);
            break;
        }
        return true;
    }


    /**
     * 只绘制原图
     *
     * @param justDrawOriginal
     */
    @Override
    public void setShowOriginal(boolean justDrawOriginal) {
        isJustDrawOriginal = justDrawOriginal;
        refreshWithBackground();
    }

    @Override
    public boolean isShowOriginal() {
        return isJustDrawOriginal;
    }

    /**
     * 设置画笔底色
     *
     * @param color
     */
    @Override
    public void setColor(IDoodleColor color) {
        mColor = color;
        //refresh();
    }


    @Override
    public IDoodleColor getColor() {
        return mColor;
    }

    /**
     * 围绕某个点缩放
     * 图片真实的缩放倍数为 mCenterScale*mScale
     *
     * @param scale
     * @param pivotX 缩放的中心点
     * @param pivotY
     */
    @Override
    public void setDoodleScale(float scale, float pivotX, float pivotY) {
        if (scale < mMinScale) {
            scale = mMinScale;
        } else if (scale > mMaxScale) {
            scale = mMaxScale;
        }

        float touchX = toTouchX(pivotX);
        float touchY = toTouchY(pivotY);
        this.mScale = scale;

        // 缩放后，偏移图片，以产生围绕某个点缩放的效果
        mTransX = toTransX(touchX, pivotX);
        mTransY = toTransY(touchY, pivotY);


        addFlag(FLAG_REFRESH_BACKGROUND);
        refresh();
    }

    public void setDoodleScaleAndTrans(float scale, float transX, float transY){
        if (scale < mMinScale) {
            scale = mMinScale;
        } else if (scale > mMaxScale) {
            scale = mMaxScale;
        }

        this.mScale = scale;

        mTransX = transX;
        mTransY = transY;

        addFlag(FLAG_REFRESH_BACKGROUND);
        refresh();
    }





    @Override
    public float getDoodleScale() {
        return mScale;
    }

    /**
     * 设置画笔
     *
     * @param pen
     */
    @Override
    public void setPen(IDoodlePen pen) {
        if (pen == null) {
            throw new RuntimeException("Pen can't be null");
        }
        IDoodlePen old = mPen;
        mPen = pen;
        //refresh();
    }

    @Override
    public IDoodlePen getPen() {
        return mPen;
    }

    /**
     * 设置画笔形状
     *
     * @param shape
     */
    @Override
    public void setShape(IDoodleShape shape) {
        if (shape == null) {
            throw new RuntimeException("Shape can't be null");
        }
        mShape = shape;
        //refresh();
    }

    @Override
    public IDoodleShape getShape() {
        return mShape;
    }

    @Override
    public void setDoodleTranslation(float transX, float transY) {
        LogUtil.d(TAG, "setDoodleTranslation");
        mTransX = transX;
        mTransY = transY;
        refreshWithBackground();
    }


    /**
     * 设置图片G偏移
     *
     * @param transX
     */
    @Override
    public void setDoodleTranslationX(float transX) {
        this.mTransX = transX;
        refreshWithBackground();
    }


    @Override
    public float getDoodleTranslationX() {
        return mTransX;
    }


    @Override
    public void setDoodleTranslationY(float transY) {
        this.mTransY = transY;
        refreshWithBackground();
    }


    @Override
    public float getDoodleTranslationY() {
        return mTransY;
    }


    @Override
    public void setSize(float paintSize, int index) {
        mSize = paintSize;
        if (index != -1) {
            mSizeIndex = index;
        }
        //refresh();
    }

    @Override
    public float getSize() {
        return mSize;
    }

    @Override
    public int getSizeIndex() {
        return mSizeIndex;
    }

    /**
     * 触摸时，图片区域外是否绘制涂鸦轨迹
     *
     * @param isDrawableOutside
     */
    @Override
    public void setIsDrawableOutside(boolean isDrawableOutside) {
        mIsDrawableOutside = isDrawableOutside;
    }

    /**
     * 触摸时，图片区域外是否绘制涂鸦轨迹
     */
    @Override
    public boolean isDrawableOutside() {
        return mIsDrawableOutside;
    }




    /**
     * 是否正在滚动涂鸦，只要用于标志触摸时才显示放大镜
     *
     * @return
     */
    public boolean isScrollingDoodle() {
        return mIsScrollingDoodle;
    }

    /**
     * @param scrollingDoodle 是否正在滚动，即是否发生滑动事件。用于放大镜的显示，放大镜只在滑动时显示
     */
    public void setScrollingDoodle(boolean scrollingDoodle) {
        mIsScrollingDoodle = scrollingDoodle;
        refresh();
    }

    public void setIsScaleDoodle(boolean isScaleDoodle) {
        mIsScaleDoodle = isScaleDoodle;
    }


    public boolean isScaleDoodle() {
        return mIsScaleDoodle;
    }

    @Override
    public void topItem(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            mItemStack.remove(item);
            mItemStack.add(item);
        } else if (item.getPen().equals(DoodlePen.BRUSH)) {
            mShapeStack.remove(item);
            mShapeStack.add(item);
        } else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicItemStack.remove(item);
            mMosaicItemStack.add(item);
        } else if (item.getPen().equals(DoodlePen.TEXT)) {
            mTextItemStack.remove(item);
            mTextItemStack.add(item);
        }

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    @Override
    public void bottomItem(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }
        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            mItemStack.remove(item);
            mItemStack.add(0, item);
        }
        if (item.getPen().equals(DoodlePen.BRUSH)) {
            mShapeStack.remove(item);
            mShapeStack.add(0, item);
        } else if (item.getPen().equals(DoodlePen.MOSAIC)) {
            mMosaicItemStack.remove(item);
            mMosaicItemStack.add(0, item);
        } else if (item.getPen().equals(DoodlePen.TEXT)) {
            mTextItemStack.remove(item);
            mTextItemStack.add(0, item);
        }

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    @Override
    public void setDoodleMinScale(float minScale) {
        mMinScale = minScale;
        //setDoodleScale(mScale, 0, 0);
    }

    @Override
    public float getDoodleMinScale() {
        return mMinScale;
    }

    @Override
    public void setDoodleMaxScale(float maxScale) {
        mMaxScale = maxScale;
        //setDoodleScale(mScale, 0, 0);
    }

    @Override
    public float getDoodleMaxScale() {
        return mMaxScale;
    }

    @Override
    public float getUnitSize() {
        return mDoodleSizeUnit;
    }

    @Override
    public void addItem(IDoodleItem item) {
        addItemInner(item);
        if (item.getPen().equals(DoodlePen.BRUSH) || item.getPen().equals(DoodlePen.ERASER)) {
            mReDoItemStack.clear();
        } else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicRedoItemStack.clear();
        }
    }

    private void addItemInner(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        if (this != item.getDoodle()) {
            throw new RuntimeException("the object Doodle is illegal");
        }
        if (mItemStack.contains(item) || mMosaicItemStack.contains(item) || mTextItemStack.contains(item)) {
            throw new RuntimeException("the item has been added");
        }

        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            mItemStack.add(item);
            mPendingItemsDrawToBitmap.add(item);
        } else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicItemStack.add(item);
            mMosaicPendingItemsDrawToBitmap.add(item);
        } else if (item.getPen().equals(DoodlePen.TEXT)) {
            if (!mTextItemStack.contains(item)) {
                mTextItemStack.add(item);
            }
        }
        item.onAdd();

        addFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);

        refresh();
    }

    private void redoItemInner(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        if (this != item.getDoodle()) {
            throw new RuntimeException("the object Doodle is illegal");
        }
        if (mItemStackOnViewCanvas.contains(item) || mShapeStackOnViewCanvas.contains(item) || mMosaicItemStackOnViewCanvas.contains(item)) {
            throw new RuntimeException("the item has been added");
        }
        if ((item.getPen().equals(DoodlePen.BRUSH) && item.getShape().equals(DoodleShape.HAND_WRITE)) || item.getPen().equals(DoodlePen.ERASER)) {
            mItemStackOnViewCanvas.add(item);
            mUnDoItemStack.add(item);
        } else if (item.getPen().equals(DoodlePen.BRUSH)) {
            if (!mShapeStack.contains(item)) {
                mShapeStack.add(item);
            }
            mShapeStackOnViewCanvas.add(item);
            mUnDoItemStack.add(item);
        } else if (item.getPen().equals(DoodlePen.MOSAIC) || item.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicItemStackOnViewCanvas.add(item);
        }
        item.onAdd();

        addFlag(FLAG_REFRESH_BACKGROUND);

        refresh();
    }

    @Override
    public void removeItem(IDoodleItem doodleItem) {
        if ((doodleItem.getPen().equals(DoodlePen.BRUSH) && doodleItem.getShape().equals(DoodleShape.HAND_WRITE)) || doodleItem.getPen().equals(DoodlePen.ERASER)) {
            mItemStackOnViewCanvas.remove(doodleItem);
            mPendingItemsDrawToBitmap.remove(doodleItem);
            mItemStack.remove(doodleItem);
            mUnDoItemStack.remove(doodleItem);
        } else if (doodleItem.getPen().equals(DoodlePen.BRUSH)) {
            mShapeStackOnViewCanvas.remove(doodleItem);
            mShapeStack.remove(doodleItem);
            mUnDoItemStack.remove(doodleItem);
        } else if (doodleItem.getPen().equals(DoodlePen.MOSAIC) || doodleItem.getPen().equals(DoodlePen.MOSAIC_ERASER)) {
            mMosaicItemStackOnViewCanvas.remove(doodleItem);
            mMosaicPendingItemsDrawToBitmap.remove(doodleItem);
            mMosaicItemStack.remove(doodleItem);
        } else if (doodleItem.getPen().equals(DoodlePen.TEXT)) {
            mTextItemStackOnViewCanvas.remove(doodleItem);
            mTextItemStack.remove(doodleItem);
        }
        doodleItem.onRemove();

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    @Override
    public int getItemCount() {
        return mItemStack.size();
    }

    @Override
    public List<IDoodleItem> getAllItem() {
        return new ArrayList<>(mItemStack);
    }


    public List<IDoodleItem> getUnDoItem() {
        return new ArrayList<>(mUnDoItemStack);
    }

    public List<IDoodleItem> getShapeItem() {
        return new ArrayList<>(mShapeStack);
    }

    @Override
    public List<IDoodleItem> getTextItem() {
        return new ArrayList<>(mTextItemStack);
    }

    @Override
    public List<IDoodleItem> getMosaicItem() {
        return new ArrayList<>(mMosaicItemStack);
    }

    @Override
    public List<IDoodleItem> getMosaicBeforeDrawItem() {
        return new ArrayList<>(mMosaicItemStackOnViewCanvas);
    }

    @Override
    public List<IDoodleItem> getDoodleBeforeDrawItem() {
        return new ArrayList<>(mItemStackOnViewCanvas);
    }


    @Override
    public int getRedoItemCount() {
        return mReDoItemStack.size();
    }


    @Override
    public List<IDoodleItem> getAllRedoItem() {
        ArrayList<IDoodleItem> iDoodleItems = new ArrayList<>(mReDoItemStack);
        return iDoodleItems;
    }

    @Override
    public List<IDoodleItem> getMosaicRedoItem() {
        return new ArrayList<>(mMosaicRedoItemStack);
    }

    @Override
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    public Bitmap getDoodleBitmap() {
        return mBitmap;
    }

    public int getCenterWidth() {
        return mCenterWidth;
    }

    public int getCenterHeight() {
        return mCenterHeight;
    }

    public float getCenterScale() {
        return mCenterScale;
    }

    public float getCentreTranX() {
        return mCentreTranX;
    }

    public float getCentreTranY() {
        return mCentreTranY;
    }

    public float getRotateScale() {
        return mRotateScale;
    }

    public float getRotateTranX() {
        return mRotateTranX;
    }

    public float getRotateTranY() {
        return mRotateTranY;
    }




    /**
     * 是否为编辑模式
     *
     * @return
     */
    public boolean isEditMode() {
        return mIsEditMode;
    }

    public void setEditMode(boolean editMode) {
        mIsEditMode = editMode;
        refresh();
    }

    // 背景图层，只在背景发生变化时绘制， 用于绘制原始图片或在优化绘制时的非编辑状态的item
    private class BackgroundView extends View {

        public BackgroundView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (LogUtil.sIsLog) {
                LogUtil.d(TAG, "BackgroundView>>onDraw");
            }
            int count = canvas.save();
            canvas.rotate(mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
            doDraw(canvas);

            canvas.restoreToCount(count);
        }

        private void doDraw(Canvas canvas) {
            float left = getAllTranX();
            float top = getAllTranY();

            // 画布和图片共用一个坐标系，只需要处理屏幕坐标系到图片（画布）坐标系的映射关系
            canvas.translate(left, top); // 偏移画布
            float scale = getAllScale();
            canvas.scale(scale, scale); // 缩放画布

            if (isJustDrawOriginal) { // 只绘制原图
                canvas.drawBitmap(mBitmap, 0, 0, null);
                return;
            }

            Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;
            if (mCropRect != null && !mCropRect.isEmpty()) {
                canvas.clipRect(mBitmapCropRect);
            }
            // 绘制涂鸦后的图片
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

    }


    // 前景图层，每次刷新都会绘制，用于绘制正在创建或选中的item
    private class ForegroundView extends View {
        public ForegroundView(Context context) {
            super(context);

            // 关闭硬件加速，某些绘图操作不支持硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        public boolean onTouchEvent(MotionEvent event) {
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
        }
    }

    // 前景图层，每次刷新都会绘制，用于绘制正在创建或选中的item
    private class ShapeView extends View {
        public ShapeView(Context context) {
            super(context);

            // 关闭硬件加速，某些绘图操作不支持硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        public boolean onTouchEvent(MotionEvent event) {
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

            List<IDoodleItem> shapeItems = new ArrayList<>(mShapeStack);
            boolean canvasClipped = false;
            if (!mIsDrawableOutside) { // 裁剪绘制区域为图片区域
                canvasClipped = true;
                if (mCropRect != null && !mCropRect.isEmpty()) {
                    canvas.clipRect(mBitmapCropRect);
                } else {
                    canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                }
            }


            for (IDoodleItem item : shapeItems) {
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


            // draw at the top
            for (IDoodleItem item : shapeItems) {
                if (!item.isNeedClipOutside()) { // 1.不需要裁剪
                    if (canvasClipped) {
                        canvas.restore();
                    }
                    item.drawAtTheTop(canvas);
                    if (canvasClipped) { // 2.恢复裁剪
                        canvas.save();
                        if (mCropRect != null && !mCropRect.isEmpty()) {
                            canvas.clipRect(mBitmapCropRect);
                        } else {
                            canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        }

                    }
                } else {
                    item.drawAtTheTop(canvas);
                }
            }
            canvas.restoreToCount(saveCount);

            if (mPen != null) {
                mPen.drawHelpers(canvas, DoodleView.this);
            }
            if (mShape != null) {
                mShape.drawHelpers(canvas, DoodleView.this);
            }
        }
    }

    // 前景图层，每次刷新都会绘制，用于绘制正在创建或选中的item
    private class TextView extends View {
        public TextView(Context context) {
            super(context);

            // 关闭硬件加速，某些绘图操作不支持硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        public boolean onTouchEvent(MotionEvent event) {
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

            List<IDoodleItem> textItems = new ArrayList<>(mTextItemStack);

            boolean canvasClipped = false;
            if (!mIsDrawableOutside) { // 裁剪绘制区域为图片区域
                canvasClipped = true;
                if (mCropRect != null && !mCropRect.isEmpty()) {
                    canvas.clipRect(mBitmapCropRect);
                } else {
                    canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                }
            }


            for (IDoodleItem item : textItems) {
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


            // draw at the top
            for (IDoodleItem item : textItems) {
                if (!item.isNeedClipOutside()) { // 1.不需要裁剪
                    if (canvasClipped) {
                        canvas.restore();
                    }
                    item.drawAtTheTop(canvas);
                    if (canvasClipped) { // 2.恢复裁剪
                        canvas.save();
                        if (mCropRect != null && !mCropRect.isEmpty()) {
                            canvas.clipRect(mBitmapCropRect);
                        } else {
                            canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        }

                    }
                } else {
                    item.drawAtTheTop(canvas);
                }
            }
            canvas.restoreToCount(saveCount);

            if (mPen != null) {
                mPen.drawHelpers(canvas, DoodleView.this);
            }
            if (mShape != null) {
                mShape.drawHelpers(canvas, DoodleView.this);
            }
        }
    }
}
