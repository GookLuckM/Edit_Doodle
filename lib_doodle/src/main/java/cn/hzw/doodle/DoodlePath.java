package cn.hzw.doodle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.WeakHashMap;

import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.core.IDoodlePen;
import cn.hzw.doodle.util.DrawUtil;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * 涂鸦轨迹
 * Created by huangziwei on 2017/3/16.
 */

public class DoodlePath extends DoodleRotatableItemBase {

    public static final int MOSAIC_LEVEL_1 = 5;
    public static final int MOSAIC_LEVEL_2 = 20;
    public static final int MOSAIC_LEVEL_3 = 50;

    private final Path mPath = new Path(); // 画笔的路径
    private final Path mOriginPath = new Path();

    private PointF mSxy = new PointF(); // 映射后的起始坐标，（手指点击）
    private PointF mDxy = new PointF(); // 映射后的终止坐标，（手指抬起）

    private Paint mPaint = new Paint();

    private CopyLocation mCopyLocation;

    private final Matrix mTransform = new Matrix();
    private Rect mRect = new Rect();
    private Matrix mBitmapColorMatrix = new Matrix();
    private int mSizeIndex;

    public DoodlePath(IDoodle doodle) {
        super(doodle, 0, 0, 0);// 这里默认item旋转角度为0
    }

    public DoodlePath(IDoodle doodle, DoodlePaintAttrs attrs) {
        super(doodle, attrs, 0, 0, 0);
    }

    public void updateXY(float sx, float sy, float dx, float dy) {
        mSxy.set(sx, sy);
        mDxy.set(dx, dy);
        mOriginPath.reset();

        if (DoodleShape.ARROW.equals(getShape())) {
            updateArrowPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        } else if (DoodleShape.LINE.equals(getShape())) {
            updateLinePath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        } else if (DoodleShape.FILL_CIRCLE.equals(getShape()) || DoodleShape.HOLLOW_CIRCLE.equals(getShape())) {
            updateCirclePath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        } else if (DoodleShape.FILL_RECT.equals(getShape()) || DoodleShape.HOLLOW_RECT.equals(getShape())) {
            updateRectPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        }

        adjustPath(true);
    }

    public void updatePath(Path path) {
        mOriginPath.reset();
        this.mOriginPath.addPath(path);
        adjustPath(true);
    }

    public CopyLocation getCopyLocation() {
        return mCopyLocation;
    }

    public Path getPath() {
        return mPath;
    }

    private PointF getDxy() {
        return mDxy;
    }

    private PointF getSxy() {
        return mSxy;
    }

    public static DoodlePath toShape(IDoodle doodle, float sx, float sy, float dx, float dy) {
        DoodlePath path = new DoodlePath(doodle);
        path.setPen(doodle.getPen().copy());
        path.setShape(doodle.getShape().copy());
        path.setSize(doodle.getSize()/ doodle.getDoodleScale());
        path.setColor(doodle.getColor().copy());

        path.updateXY(sx, sy, dx, dy);
        if (path.getPen() == DoodlePen.COPY) {
            if (doodle instanceof DoodleView) {
                path.mCopyLocation = DoodlePen.COPY.getCopyLocation().copy();
            }
        }
        return path;
    }

    public static DoodlePath toPath(IDoodle doodle, Path p) {
        if (doodle.getPen() != null) {
            DoodlePath path = new DoodlePath(doodle);
            path.setPen(doodle.getPen().copy());
            path.setShape(doodle.getShape().copy());
            path.setSize(doodle.getSize() / doodle.getDoodleScale());
            path.setColor(doodle.getColor().copy());

            path.updatePath(p);
            if (doodle instanceof DoodleView) {
                path.mCopyLocation = DoodlePen.COPY.getCopyLocation().copy();
            } else {
                path.mCopyLocation = null;
            }
            return path;
        }
        return null;
    }

    @Override
    protected void doDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setStrokeWidth(getSize());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);

        getPen().config(this, mPaint);
        getColor().config(this, mPaint);
        getShape().config(this, mPaint);

        canvas.drawPath(getPath(), mPaint);
    }

    private RectF mBound = new RectF();

    private void resetLocationBounds(Rect rect) {
        if (mOriginPath == null) {
            return;
        }

        int diff = (int) (getSize() / 2 + 0.5f);
        mOriginPath.computeBounds(mBound, false);
        if (getShape() == DoodleShape.ARROW || getShape() == DoodleShape.FILL_CIRCLE || getShape() == DoodleShape.FILL_RECT) {
            diff = (int) getDoodle().getUnitSize();
        }
        rect.set((int) (mBound.left - diff), (int) (mBound.top - diff), (int) (mBound.right + diff), (int) (mBound.bottom + diff));
    }

    @Override
    protected void resetBounds(Rect rect) {
        resetLocationBounds(rect);
        rect.set(0, 0, rect.width(), rect.height());
    }

    @Override
    public boolean isDoodleEditable() {
        if (getPen() == DoodlePen.ERASER || getPen() == DoodlePen.MOSAIC_ERASER) { // eraser is not editable
            return false;
        }

        return super.isDoodleEditable();
    }

    //---------计算Path
    private Path mArrowTrianglePath;

    private void updateArrowPath(Path path, float touchDownX, float touchDownY, float touchX, float touchY, float size) {
        PointF beginPoint = new PointF(touchDownX, touchDownY);
        PointF endPoint = new PointF(touchX, touchY);
        //箭头的角度
        float arrowAngle = (float) (70 * Math.PI / 180.f);
        //箭头腰长
        float arrowWaistLength = 34f * getDoodle().getUnitSize();
        //箭头空白地儿宽度
        float arrowGapWidth = 10f * getDoodle().getUnitSize();
        //起点圆角半径
        float radius = 1.7f * getDoodle().getUnitSize();
        int index = getDoodle().getSizeIndex();
        if (index == 0) {
            arrowWaistLength = 20f * getDoodle().getUnitSize();
            arrowGapWidth = 6f * getDoodle().getUnitSize();
            radius = 1f * getDoodle().getUnitSize();
        } else if (index == 1) {
            arrowWaistLength = 26f * getDoodle().getUnitSize();
            arrowGapWidth = 8f * getDoodle().getUnitSize();
            radius = 1.4f * getDoodle().getUnitSize();
        } else if (index == 2) {
            arrowWaistLength = 34f * getDoodle().getUnitSize();
            arrowGapWidth = 10f * getDoodle().getUnitSize();
            radius = 1.7f * getDoodle().getUnitSize() ;
        } else if (index == 3) {
            arrowWaistLength = 48f * getDoodle().getUnitSize();
            arrowGapWidth = 14f * getDoodle().getUnitSize();
            radius = 2.4f * getDoodle().getUnitSize();
        } else if (index == 4) {
            arrowWaistLength = 66f * getDoodle().getUnitSize();
            arrowGapWidth = 20f * getDoodle().getUnitSize();
            radius = 3.5f * getDoodle().getUnitSize();
        }

        PointF point2 = new PointF(0, 0);//左边内角
        PointF point3 = new PointF(0, 0);//左边外角
        PointF point4 = endPoint;//终点
        PointF point5 = new PointF(0, 0);//右边外角
        PointF point6 = new PointF(0, 0);//右边内角

        float lineAngle = angleBetweenStartPoint(beginPoint, endPoint);
        //箭头底边长
        float arrowBottomLength = (float) (arrowWaistLength * sin(arrowAngle / 2f) * 2);

        //箭头垂直高度
        float arrowVerticalLenght = arrowWaistLength * (float) cos(arrowAngle / 2f);

        //图形最小长度
        float minLength = 12 * getDoodle().getUnitSize() + arrowVerticalLenght;

        if (distanceBetweenStartPoint(beginPoint, endPoint) < minLength) {
            //尾巴长度小于这个长度
            if (endPoint.x > beginPoint.x) {
                endPoint = new PointF((float)(beginPoint.x + minLength *  cos(lineAngle)), (float)(beginPoint.y - minLength *  sin(lineAngle)));
            } else {
                endPoint = new PointF((float)(beginPoint.x - minLength * cos(lineAngle)), (float)(beginPoint.y + minLength * sin(lineAngle)));
            }
            point4 = endPoint;
        }

        //箭头垂直中心点
        PointF arrowGapCenter;
        if (endPoint.x > beginPoint.x) {
            arrowGapCenter = new PointF((float)(endPoint.x - arrowVerticalLenght * cos(lineAngle)), (float)(endPoint.y + arrowVerticalLenght * sin(lineAngle)));
        } else {
            arrowGapCenter = new PointF((float) (endPoint.x + arrowVerticalLenght * cos(lineAngle)), (float)(endPoint.y - arrowVerticalLenght * sin(lineAngle)));
        }
        //两个箭头左右尖端
        point5 = new PointF((float)(arrowGapCenter.x + arrowBottomLength / 2.f *sin(lineAngle)), (float)(arrowGapCenter.y + arrowBottomLength / 2.f * cos(lineAngle)));
        point3 = new PointF((float)(arrowGapCenter.x - arrowBottomLength / 2.f * sin(lineAngle)), (float)(arrowGapCenter.y - arrowBottomLength / 2.f * cos(lineAngle)));

        //两个箭头内角
        point6 = new PointF((float)(arrowGapCenter.x + arrowGapWidth / 2.f * sin(lineAngle)), (float)(arrowGapCenter.y + arrowGapWidth / 2.f * cos(lineAngle)));
        point2 = new PointF((float)(arrowGapCenter.x - arrowGapWidth / 2.f * sin(lineAngle)), (float)(arrowGapCenter.y - arrowGapWidth / 2.f * cos(lineAngle)));
        if (mArrowTrianglePath == null) {
            mArrowTrianglePath = new Path();
        }
        mArrowTrianglePath.reset();

        //path.lineWidth = 1;
        //底部半圆弧形
        if (endPoint.x > beginPoint.x) {
            PointF arcCenter = new PointF(beginPoint.x + radius * (float) cos(lineAngle), beginPoint.y - radius * (float) sin(lineAngle));
            //mArrowTrianglePath.addArc();
            RectF rectF = new RectF(arcCenter.x - radius,arcCenter.y - radius , arcCenter.x+radius,arcCenter.y+radius);
            mArrowTrianglePath.arcTo(rectF, (float) (Math.PI * 2 - lineAngle), (float) (Math.PI * 2 - lineAngle + Math.PI),false);
            //[path addArcWithCenter:arcCenter radius:M_PI startAngle:M_PI_2 - lineAngle endAngle:M_PI_2 - lineAngle + M_PI clockwise:YES];
        } else {
            PointF arcCenter = new PointF(beginPoint.x - radius * (float)cos(lineAngle), beginPoint.y + radius * (float)sin(lineAngle));
            RectF rectF = new RectF(arcCenter.x - radius,arcCenter.y - radius , arcCenter.x+radius,arcCenter.y+radius);
            mArrowTrianglePath.arcTo(rectF, (float) (Math.PI * 2 - lineAngle), (float) (Math.PI * 2 - lineAngle + Math.PI),false);
            //[path addArcWithCenter:arcCenter radius:M_PI startAngle:M_PI_2 - lineAngle endAngle:M_PI_2 - lineAngle + M_PI clockwise:NO];
        }

        mArrowTrianglePath.moveTo(touchDownX, touchDownY);
        mArrowTrianglePath.lineTo(point2.x, point2.y);
        mArrowTrianglePath.lineTo(point3.x, point3.y);
        mArrowTrianglePath.lineTo(point4.x, point4.y);
        mArrowTrianglePath.lineTo(point5.x, point5.y);
        mArrowTrianglePath.lineTo(point6.x, point6.y);
        mArrowTrianglePath.close();
        path.addPath(mArrowTrianglePath);
    }


    private float distanceBetweenStartPoint(PointF startPoint, PointF endPoint) {
        float xDist = (endPoint.x - startPoint.x);
        float yDist = (endPoint.y - startPoint.y);
        return (float) Math.sqrt((xDist * xDist) + (yDist * yDist));
    }

    private float angleBetweenStartPoint(PointF startPoint, PointF endPoint) {
        float height = endPoint.y - startPoint.y;
        float width = startPoint.x - endPoint.x;
        float rads = (float) Math.atan(height / width);
        return rads;
    }

    private void updateLinePath(Path path, float sx, float sy, float ex, float ey, float size) {
        path.moveTo(sx, sy);
        path.lineTo(ex, ey);
    }

    private void updateCirclePath(Path path, float sx, float sy, float dx, float dy, float size) {
        float radius = (float) Math.sqrt((sx - dx) * (sx - dx) + (sy - dy) * (sy - dy));
        path.addCircle(sx, sy, radius, Path.Direction.CCW);

    }

    private void updateRectPath(Path path, float sx, float sy, float dx, float dy, float size) {
        // 保证　左上角　与　右下角　的对应关系
        if (sx < dx) {
            if (sy < dy) {
                path.addRect(sx, sy, dx, dy, Path.Direction.CCW);
            } else {
                path.addRect(sx, dy, dx, sy, Path.Direction.CCW);
            }
        } else {
            if (sy < dy) {
                path.addRect(dx, sy, sx, dy, Path.Direction.CCW);
            } else {
                path.addRect(dx, dy, sx, sy, Path.Direction.CCW);
            }
        }
    }

    private static WeakHashMap<IDoodle, HashMap<Integer, Bitmap>> sMosaicBitmapMap = new WeakHashMap<>();

    public static DoodleColor getMosaicColor(IDoodle doodle, int level) {
        HashMap<Integer, Bitmap> map = sMosaicBitmapMap.get(doodle);
        if (map == null) {
            map = new HashMap<>();
            sMosaicBitmapMap.put(doodle, map);
        }

        int w = doodle.getBitmap().getWidth() / level;
        int h = doodle.getBitmap().getHeight() / level;

        Bitmap mosaicBitmap = map.get(level);
        if (mosaicBitmap == null) {
           mosaicBitmap = Bitmap.createScaledBitmap(doodle.getBitmap(), w, h, false);
        }
        DoodleColor doodleColor = new DoodleColor(mosaicBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        doodleColor.setLevel(level);
        return doodleColor;
    }

    @Override
    public void setLocation(float x, float y, boolean changePivot) {
        super.setLocation(x, y, changePivot);
        adjustMosaic();
    }

    @Override
    public void setColor(IDoodleColor color) {
        super.setColor(color);
        if (getPen() == DoodlePen.MOSAIC) {
            setLocation(getLocation().x, getLocation().y, false);
        }
        adjustPath(false);
    }


    @Override
    public void setSize(float size) {
        super.setSize(size);
        if (mTransform == null) {
            return;
        }

        if (DoodleShape.ARROW.equals(getShape())) {
            mOriginPath.reset();
            updateArrowPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        }

        adjustPath(false);
    }


    public void setIndex(int sizeIndex){
        if (sizeIndex != -1){
            mSizeIndex = sizeIndex;
        }
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);
        adjustMosaic();
    }



    private void adjustMosaic() {
        if (getPen() == DoodlePen.MOSAIC
                && getColor() instanceof DoodleColor) {
            DoodleColor doodleColor = ((DoodleColor) getColor());
            Matrix matrix = doodleColor.getMatrix();
            matrix.reset();
            matrix.preScale(1 / getScale(), 1 / getScale(), getPivotX(), getPivotY()); // restore scale
            matrix.preTranslate(-getLocation().x * getScale(), -getLocation().y * getScale());
            matrix.preRotate(-getItemRotate(), getPivotX(), getPivotY());
            matrix.preScale(doodleColor.getLevel(), doodleColor.getLevel());
            doodleColor.setMatrix(matrix);
            refresh();
        }
    }

    @Override
    public void setItemRotate(float textRotate) {
        super.setItemRotate(textRotate);
        adjustMosaic();
    }

    private void adjustPath(boolean changePivot) {
        resetLocationBounds(mRect);
        mPath.reset();
        this.mPath.addPath(mOriginPath);
        mTransform.reset();
        mTransform.setTranslate(-mRect.left, -mRect.top);
        mPath.transform(mTransform);
        if (changePivot) {
            setPivotX(mRect.left + mRect.width() / 2);
            setPivotY(mRect.top + mRect.height() / 2);
            setLocation(mRect.left, mRect.top, false);
        }

        if ((getColor() instanceof DoodleColor)) {
            DoodleColor color = (DoodleColor) getColor();
            if (color.getType() == DoodleColor.Type.BITMAP && color.getBitmap() != null) {
                mBitmapColorMatrix.reset();

                if (getPen() == DoodlePen.MOSAIC) {
                    adjustMosaic();
                    return;
                } else {
                    if (getPen() == DoodlePen.COPY) {
                        // 根据旋转值获取正确的旋转底图
                        float transXSpan = 0, transYSpan = 0;
                        CopyLocation copyLocation = getCopyLocation();
                        // 仿制时需要偏移图片
                        if (copyLocation != null) {
                            transXSpan = copyLocation.getTouchStartX() - copyLocation.getCopyStartX();
                            transYSpan = copyLocation.getTouchStartY() - copyLocation.getCopyStartY();
                        }
                        resetLocationBounds(mRect);
                        mBitmapColorMatrix.setTranslate(transXSpan - mRect.left, transYSpan - mRect.top);
                    } else {
                        mBitmapColorMatrix.setTranslate(-mRect.left, -mRect.top);
                    }

                    int level = color.getLevel();
                    mBitmapColorMatrix.preScale(level, level);
                    color.setMatrix(mBitmapColorMatrix);
                    refresh();
                    return;
                }
            }
        }

        refresh();
    }
}

