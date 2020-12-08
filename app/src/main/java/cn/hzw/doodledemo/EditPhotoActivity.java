package cn.hzw.doodledemo;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.StatusBarUtil;
import cn.forward.androids.utils.Util;
import cn.hzw.doodle.DoodleActivity;
import cn.hzw.doodle.DoodleBitmap;
import cn.hzw.doodle.DoodleColor;
import cn.hzw.doodle.DoodleOnTouchGestureListener;
import cn.hzw.doodle.DoodleParams;
import cn.hzw.doodle.DoodlePath;
import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleShape;
import cn.hzw.doodle.DoodleText;
import cn.hzw.doodle.DoodleTouchDetector;
import cn.hzw.doodle.DoodleView;
import cn.hzw.doodle.IDoodleListener;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleItemListener;
import cn.hzw.doodle.core.IDoodlePen;
import cn.hzw.doodle.core.IDoodleSelectableItem;
import cn.hzw.doodle.core.IDoodleShape;
import cn.hzw.doodle.core.IDoodleTouchDetector;
import cn.hzw.doodle.dialog.ColorPickerDialog;
import cn.hzw.doodle.dialog.DialogController;

public class EditPhotoActivity extends Activity implements View.OnClickListener {

    private static final int EDIT_TEXT_REQUEST_CODE = 9999;

    public static final String KEY_PARAMS = "key_doodle_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";
    public static final String NONE = "none";
    public static final String MODE_SCRAWL = "mode_scrawl";
    public static final String MODE_TEXT = "mode_text";
    public static final String MODE_CROP = "mode_crop";
    public static final String MODE_MOSAIC = "mode_mosaic";

    private String[] colorArr;

    public final static int DEFAULT_MOSAIC_SIZE = 20; // 默认马赛克大小
    public final static int DEFAULT_TEXT_SIZE = 28; // 默认文字大小

    public static final int RESULT_ERROR = -111; // 出现错误

    private DoodleParams mDoodleParams;
    private String mImagePath;

    private FrameLayout mFrameLayout;
    private IDoodle mDoodle;
    private DoodleView mDoodleView;


    private String CURRENT_MODE = NONE;
    private int selectedColor = Color.TRANSPARENT;
    private int textSelectedColor = Color.WHITE;

    private Map<IDoodlePen, Float> mPenSizeMap = new HashMap<>(); //保存每个画笔对应的最新大小
    private RelativeLayout rlMosaic;
    private RelativeLayout rlScrawlColor;
    private RadioGroup tabMode;
    private RadioGroup rgColor;
    private RadioGroup rgMosaic;


    private int mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3;
    ;

    // 触摸屏幕超过一定时间才判断为需要隐藏设置面板
    private Runnable mHideDelayRunnable;
    // 触摸屏幕超过一定时间才判断为需要显示设置面板
    private Runnable mShowDelayRunnable;
    private AlphaAnimation mViewShowAnimation, mViewHideAnimation; // view隐藏和显示时用到的渐变动画

    private DoodleOnTouchGestureListener mTouchGestureListener;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PARAMS, mDoodleParams);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        mDoodleParams = savedInstanceState.getParcelable(KEY_PARAMS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarTranslucent(this, true, false);
        if (mDoodleParams == null) {
            mDoodleParams = getIntent().getExtras().getParcelable(KEY_PARAMS);
        }
        if (mDoodleParams == null) {
            LogUtil.e("TAG", "mDoodleParams is null!");
            this.finish();
            return;
        }

        mImagePath = mDoodleParams.mImagePath;
        if (mImagePath == null) {
            LogUtil.e("TAG", "mImagePath is null!");
            this.finish();
            return;
        }

        LogUtil.d("TAG", mImagePath);
        if (mDoodleParams.mIsFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        Bitmap bitmap = ImageUtils.createBitmapFromPath(mImagePath, this);
        if (bitmap == null) {
            LogUtil.e("TAG", "bitmap is null!");
            this.finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_photo);
        colorArr = getResources().getStringArray(R.array.color_arr);
        mFrameLayout = (FrameLayout) findViewById(cn.hzw.doodle.R.id.doodle_container);

        /*
        Whether or not to optimize drawing, it is suggested to open, which can optimize the drawing speed and performance.
        Note: When item is selected for editing after opening, it will be drawn at the top level, and not at the corresponding level until editing is completed.
        是否优化绘制，建议开启，可优化绘制速度和性能.
        注意：开启后item被选中编辑时时会绘制在最上面一层，直到结束编辑后才绘制在相应层级
         */
        mDoodle = mDoodleView = new DoodleViewWrapper(this, bitmap, mDoodleParams.mOptimizeDrawing, new IDoodleListener() {
            @Override
            public void onSaved(IDoodle doodle, Bitmap bitmap, Runnable callback) { // 保存图片为jpg格式
                File doodleFile = null;
                File file = null;
                String savePath = mDoodleParams.mSavePath;
                boolean isDir = mDoodleParams.mSavePathIsDir;
                if (TextUtils.isEmpty(savePath)) {
                    File dcimFile = new File(Environment.getExternalStorageDirectory(), "DCIM");
                    doodleFile = new File(dcimFile, "Doodle");
                    //　保存的路径
                    file = new File(doodleFile, System.currentTimeMillis() + ".jpg");
                } else {
                    if (isDir) {
                        doodleFile = new File(savePath);
                        //　保存的路径
                        file = new File(doodleFile, System.currentTimeMillis() + ".jpg");
                    } else {
                        file = new File(savePath);
                        doodleFile = file.getParentFile();
                    }
                }
                doodleFile.mkdirs();

                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    ImageUtils.addImage(getContentResolver(), file.getAbsolutePath());
                    Intent intent = new Intent();
                    intent.putExtra(KEY_IMAGE_PATH, file.getAbsolutePath());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    onError(DoodleView.ERROR_SAVE, e.getMessage());
                } finally {
                    Util.closeQuietly(outputStream);
                    callback.run();
                }
            }

            public void onError(int i, String msg) {
                setResult(RESULT_ERROR);
                finish();
            }

            @Override
            public void onReady(IDoodle doodle) {
                //mEditSizeSeekBar.setMax(Math.min(mDoodleView.getWidth(), mDoodleView.getHeight()));

                float size = mDoodleParams.mPaintUnitSize > 0 ? mDoodleParams.mPaintUnitSize * mDoodle.getUnitSize() : 0;
                if (size <= 0) {
                    size = mDoodleParams.mPaintPixelSize > 0 ? mDoodleParams.mPaintPixelSize : mDoodle.getSize();
                }
                // 设置初始值
                mDoodle.setSize(size);
                // 选择画笔
                mDoodle.setPen(DoodlePen.BRUSH);
                mDoodle.setShape(DoodleShape.HAND_WRITE);
                mDoodle.setColor(new DoodleColor(mDoodleParams.mPaintColor));
                mDoodle.setZoomerScale(mDoodleParams.mZoomerScale);
                mTouchGestureListener.setSupportScaleItem(mDoodleParams.mSupportScaleItem);


            }
        });

        initView();


        mDoodleView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 隐藏设置面板
                if (mDoodleParams.mChangePanelVisibilityDelay > 0) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            rlMosaic.removeCallbacks(mHideDelayRunnable);
                            rlMosaic.removeCallbacks(mShowDelayRunnable);
                            rlScrawlColor.removeCallbacks(mHideDelayRunnable);
                            rlScrawlColor.removeCallbacks(mShowDelayRunnable);
                            tabMode.removeCallbacks(mHideDelayRunnable);
                            tabMode.removeCallbacks(mShowDelayRunnable);
                            //触摸屏幕超过一定时间才判断为需要隐藏设置面板
                            tabMode.postDelayed(mHideDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            rlScrawlColor.postDelayed(mHideDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            rlMosaic.postDelayed(mHideDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            rlMosaic.removeCallbacks(mHideDelayRunnable);
                            rlMosaic.removeCallbacks(mShowDelayRunnable);
                            rlScrawlColor.removeCallbacks(mHideDelayRunnable);
                            rlScrawlColor.removeCallbacks(mShowDelayRunnable);
                            tabMode.removeCallbacks(mHideDelayRunnable);
                            tabMode.removeCallbacks(mShowDelayRunnable);
                            //离开屏幕超过一定时间才判断为需要显示设置面板
                            tabMode.postDelayed(mShowDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            rlScrawlColor.postDelayed(mShowDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            rlMosaic.postDelayed(mShowDelayRunnable, mDoodleParams.mChangePanelVisibilityDelay);
                            break;
                    }
                }

                return false;
            }
        });

        mTouchGestureListener = new DoodleOnTouchGestureListener(mDoodleView, new DoodleOnTouchGestureListener.ISelectionListener() {
            // save states before being selected
            IDoodlePen mLastPen = null;
            IDoodleColor mLastColor = null;
            Float mSize = null;

            IDoodleItemListener mIDoodleItemListener = new IDoodleItemListener() {
                @Override
                public void onPropertyChanged(int property) {
                    if (mTouchGestureListener.getSelectedItem() == null) {
                        return;
                    }
                    /*if (property == IDoodleItemListener.PROPERTY_SCALE) {
                        mItemScaleTextView.setText(
                                (int) (mTouchGestureListener.getSelectedItem().getScale() * 100 + 0.5f) + "%");
                    }*/
                }
            };

            @Override
            public void onSelectedItem(IDoodle doodle, IDoodleSelectableItem selectableItem, boolean selected) {
                if (selected) {
                    if (mLastPen == null) {
                        mLastPen = mDoodle.getPen();
                    }
                    if (mLastColor == null) {
                        mLastColor = mDoodle.getColor();
                    }
                    if (mSize == null) {
                        mSize = mDoodle.getSize();
                    }
                    mDoodleView.setEditMode(true);
                    mDoodle.setPen(selectableItem.getPen());
                    mDoodle.setColor(selectableItem.getColor());
                    mDoodle.setSize(selectableItem.getSize());
                    selectableItem.addItemListener(mIDoodleItemListener);
                } else {
                    selectableItem.removeItemListener(mIDoodleItemListener);
                    mDoodleView.setEditMode(false);
                    if (mTouchGestureListener.getSelectedItem() == null) { // nothing is selected. 当前没有选中任何一个item
                        if (mLastPen != null) {
                            mDoodle.setPen(mLastPen);
                            mLastPen = null;
                        }
                        if (mLastColor != null) {
                            mDoodle.setColor(mLastColor);
                            mLastColor = null;
                        }
                        if (mSize != null) {
                            mDoodle.setSize(mSize);
                            mSize = null;
                        }
                    }
                }
            }

            @Override
            public void onCreateSelectableItem(IDoodle doodle, float x, float y) {
                if (mDoodle.getPen() == DoodlePen.TEXT) {
                    createDoodleText(null, x, y, "", false);
                }

            }
        }) {
            @Override
            public void setSupportScaleItem(boolean supportScaleItem) {
                super.setSupportScaleItem(supportScaleItem);
            }
        };


        IDoodleTouchDetector detector = new DoodleTouchDetector(getApplicationContext(), mTouchGestureListener);
        mDoodleView.setDefaultTouchDetector(detector);

        mDoodle.setIsDrawableOutside(mDoodleParams.mIsDrawableOutside);
        mFrameLayout.addView(mDoodleView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDoodle.setDoodleMinScale(mDoodleParams.mMinScale);
        mDoodle.setDoodleMaxScale(mDoodleParams.mMaxScale);

    }

    public void initView() {
        rlMosaic = findViewById(R.id.rl_mosaic);
        rlScrawlColor = findViewById(R.id.rl_scrawl_color);
        tabMode = findViewById(R.id.tab_group);
        rgColor = findViewById(R.id.rg_scrawl_color);
        rgMosaic = findViewById(R.id.rg_mosaic);


        mViewShowAnimation = new AlphaAnimation(0, 1);
        mViewShowAnimation.setDuration(150);
        mViewHideAnimation = new AlphaAnimation(1, 0);
        mViewHideAnimation.setDuration(150);
        mHideDelayRunnable = new Runnable() {
            public void run() {
                hideView(tabMode);
                hideView(rlMosaic);
                hideView(rlScrawlColor);
            }

        };
        mShowDelayRunnable = new Runnable() {
            public void run() {
                showView(tabMode);
                if (MODE_SCRAWL.equals(CURRENT_MODE)) {
                    showView(rlScrawlColor);
                }
                if (MODE_MOSAIC.equals(CURRENT_MODE)) {
                    showView(rlMosaic);
                }


            }
        };

        tabMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_scrawl:
                        CURRENT_MODE = MODE_SCRAWL;
                        mDoodle.setPen(DoodlePen.BRUSH);
                        mDoodle.setShape(DoodleShape.HAND_WRITE);
                        rlScrawlColor.setVisibility(View.VISIBLE);
                        rlMosaic.setVisibility(View.GONE);
                        resetBitmap(true);
                        break;
                    case R.id.rb_text:
                        CURRENT_MODE = MODE_TEXT;
                        rlScrawlColor.setVisibility(View.GONE);
                        rlMosaic.setVisibility(View.GONE);
                        mDoodle.setPen(DoodlePen.TEXT);
                        startActivityForResult(new Intent(EditPhotoActivity.this, AddTextActivity.class), EDIT_TEXT_REQUEST_CODE);
                        break;
                    case R.id.rb_crop:
                        CURRENT_MODE = MODE_CROP;
                        rlScrawlColor.setVisibility(View.GONE);
                        rlMosaic.setVisibility(View.GONE);
                        resetBitmap(false);
                        break;
                    case R.id.rb_mosaic:
                        CURRENT_MODE = MODE_MOSAIC;
                        mDoodle.setPen(DoodlePen.MOSAIC);
                        rlScrawlColor.setVisibility(View.GONE);
                        rlMosaic.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });


        rgColor.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_scrawl_grey:
                        selectedColor = Color.parseColor(colorArr[0]);
                        break;
                    case R.id.rb_scrawl_black:
                        selectedColor = Color.parseColor(colorArr[1]);
                        break;
                    case R.id.rb_scrawl_red:
                        selectedColor = Color.parseColor(colorArr[2]);
                        break;
                    case R.id.rb_scrawl_yellow:
                        selectedColor = Color.parseColor(colorArr[3]);
                        break;
                    case R.id.rb_scrawl_green:
                        selectedColor = Color.parseColor(colorArr[4]);
                        break;
                    case R.id.rb_scrawl_blue:
                        selectedColor = Color.parseColor(colorArr[5]);
                        break;
                    case R.id.rb_scrawl_purple:
                        selectedColor = Color.parseColor(colorArr[6]);
                        break;

                }
                mDoodle.setColor(new DoodleColor(selectedColor));
            }
        });


        rgMosaic.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_large_mosaic:

                        mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3;
                        mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
                        if (mTouchGestureListener.getSelectedItem() != null) {
                            mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
                        }
                        break;
                    case R.id.rb_small_mosaic:
                        mMosaicLevel = DoodlePath.MOSAIC_LEVEL_2;
                        mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
                        if (mTouchGestureListener.getSelectedItem() != null) {
                            mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
                        }
                        break;

                }
            }
        });

        ImageView ivScrawlBack = findViewById(R.id.iv_scrawl_back);
        ivScrawlBack.setOnClickListener(this);
        ImageView ivMosaicBack = findViewById(R.id.iv_mosaic_back);
        ivMosaicBack.setOnClickListener(this);
        TextView tvDone = findViewById(R.id.tv_done);
        tvDone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_scrawl_back) {
            mDoodle.undo();
        } else if (v.getId() == R.id.iv_mosaic_back) {
            mDoodle.undo();
        } else if (v.getId() == R.id.tv_done) {
            mDoodle.save();
        } /*else if (v.getId() == R.id.doodle_btn_back) {
            if (mDoodle.getAllItem() == null || mDoodle.getItemCount() == 0) {
                finish();
                return;
            }
            if (!(DoodleParams.getDialogInterceptor() != null
                    && DoodleParams.getDialogInterceptor().onShow(EditPhotoActivity.this, mDoodle, DoodleParams.DialogType.SAVE))) {
                DialogController.showMsgDialog(EditPhotoActivity.this, getString(cn.hzw.doodle.R.string.doodle_saving_picture), null, getString(cn.hzw.doodle.R.string.doodle_cancel),
                        getString(cn.hzw.doodle.R.string.doodle_save), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDoodle.save();
                            }
                        }, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
            }
        } *//*else if (v.getId() == cn.hzw.doodle.R.id.doodle_btn_rotate) {
            // 旋转图片
            if (mRotateAnimator == null) {
                mRotateAnimator = new ValueAnimator();
                mRotateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (int) animation.getAnimatedValue();
                        mDoodle.setDoodleRotation(value);
                    }
                });
                mRotateAnimator.setDuration(250);
            }
            if (mRotateAnimator.isRunning()) {
                return;
            }
            mRotateAnimator.setIntValues(mDoodle.getDoodleRotation(), mDoodle.getDoodleRotation() + 90);
            mRotateAnimator.start();
        } */
    }


    /**
     * 包裹DoodleView，监听相应的设置接口，以改变UI状态
     */
    private class DoodleViewWrapper extends DoodleView {

        public DoodleViewWrapper(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener) {
            super(context, bitmap, optimizeDrawing, listener);
        }

        private Map<IDoodlePen, Integer> mBtnPenIds = new HashMap<>();

        {
            mBtnPenIds.put(DoodlePen.BRUSH, cn.hzw.doodle.R.id.btn_pen_hand);
            mBtnPenIds.put(DoodlePen.MOSAIC, cn.hzw.doodle.R.id.btn_pen_mosaic);
            mBtnPenIds.put(DoodlePen.TEXT, cn.hzw.doodle.R.id.btn_pen_text);
        }

        @Override
        public void setPen(IDoodlePen pen) {
            IDoodlePen oldPen = getPen();
            super.setPen(pen);

            if (pen == DoodlePen.TEXT) {
                rlMosaic.setVisibility(GONE);
                rlScrawlColor.setVisibility(GONE);
            } else if (pen == DoodlePen.MOSAIC) {
                rlMosaic.setVisibility(VISIBLE);
                rlScrawlColor.setVisibility(GONE);
            } else if (pen == DoodlePen.BRUSH) {
                rlMosaic.setVisibility(View.GONE);
                rlScrawlColor.setVisibility(View.GONE);
            }

            /*if (mTouchGestureListener.getSelectedItem() == null) {
                mPenSizeMap.put(oldPen, getSize()); // save
                Float size = mPenSizeMap.get(pen); // restore
                if (size != null) {
                    mDoodle.setSize(size);
                }
                if (isEditMode()) {
                    rlScrawlColor.setVisibility(GONE);
                    rlMosaic.setVisibility(GONE);
                    tabMode.setVisibility(GONE);

                }
            } else {
                //mShapeContainer.setVisibility(GONE);
                return;
            }*/
            if (pen == DoodlePen.BRUSH) {
                mDoodle.setSize(DoodleView.DEFAULT_SIZE);
                mDoodle.setColor(new DoodleColor(selectedColor));
            } else if (pen == DoodlePen.MOSAIC) {
                mDoodle.setSize(DEFAULT_MOSAIC_SIZE * mDoodle.getUnitSize());
                mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
            } else if (pen == DoodlePen.TEXT) {
                mDoodle.setSize(getResources().getDimensionPixelSize(R.dimen.sp_28) / mDoodleView.getAllScale());
                mDoodle.setColor(new DoodleColor(textSelectedColor));
            } /*else if (pen == DoodlePen.BITMAP) {
                Drawable colorBg = mBtnColor.getBackground();
                if (colorBg instanceof ColorDrawable) {
                    mDoodle.setColor(new DoodleColor(((ColorDrawable) colorBg).getColor()));
                } else {
                    mDoodle.setColor(new DoodleColor(((BitmapDrawable) colorBg).getBitmap()));
                }
            }*/
        }


        @Override
        public void setShape(IDoodleShape shape) {
            super.setShape(shape);
        }


        @Override
        public void setSize(float paintSize) {
            super.setSize(paintSize);

            if (mTouchGestureListener.getSelectedItem() != null) {
                mTouchGestureListener.getSelectedItem().setSize(getSize());
            }
        }

        @Override
        public void setColor(IDoodleColor color) {
            IDoodlePen pen = getPen();
            super.setColor(color);

            DoodleColor doodleColor = null;
            if (color instanceof DoodleColor) {
                doodleColor = (DoodleColor) color;
            }
            if (doodleColor != null) {

                if (mTouchGestureListener.getSelectedItem() != null) {
                    mTouchGestureListener.getSelectedItem().setColor(getColor().copy());
                }
            }

        }

        @Override
        public void enableZoomer(boolean enable) {
            super.enableZoomer(enable);

        }

        @Override
        public boolean undo() {
            mTouchGestureListener.setSelectedItem(null);
            boolean res = super.undo();
            return res;
        }

        @Override
        public void clear() {
            super.clear();
            mTouchGestureListener.setSelectedItem(null);
        }

        @Override
        public void addItem(IDoodleItem item) {
            super.addItem(item);
        }

        Boolean mLastIsDrawableOutside = null;

        @Override
        public void setEditMode(boolean editMode) {
            if (editMode == isEditMode()) {
                return;
            }

            super.setEditMode(editMode);
            if (editMode) {
                Toast.makeText(getApplicationContext(), cn.hzw.doodle.R.string.doodle_edit_mode, Toast.LENGTH_SHORT).show();
                mLastIsDrawableOutside = mDoodle.isDrawableOutside(); // save
                mDoodle.setIsDrawableOutside(true);
                rlMosaic.setVisibility(GONE);
                rlScrawlColor.setVisibility(GONE);
                tabMode.setVisibility(GONE);

            } else {
                if (mLastIsDrawableOutside != null) { // restore
                    mDoodle.setIsDrawableOutside(mLastIsDrawableOutside);
                }
                mTouchGestureListener.center(); // center picture
                if (mTouchGestureListener.getSelectedItem() == null) { // restore
                    setPen(getPen());
                }

                switch (CURRENT_MODE) {
                    case MODE_SCRAWL:
                        rlScrawlColor.setVisibility(View.VISIBLE);
                        break;
                    case MODE_MOSAIC:
                        rlMosaic.setVisibility(View.VISIBLE);
                        break;
                }
                mTouchGestureListener.setSelectedItem(null);
                tabMode.setVisibility(View.VISIBLE);
            }
        }

    }


    // 添加文字
    private void createDoodleText(final DoodleText doodleText, final float x, final float y, String text, boolean isShowTextbg) {
        if (isFinishing()) {
            return;
        }

        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (doodleText == null) {
            IDoodleSelectableItem item = new DoodleText(mDoodle, text, mDoodle.getSize(), mDoodle.getColor().copy(), x, y, isShowTextbg);
            mDoodle.addItem(item);
            mTouchGestureListener.setSelectedItem(item);
        } else {
            doodleText.setText(text);
        }
        mDoodle.refresh();

        if (doodleText == null) {
            tabMode.removeCallbacks(mHideDelayRunnable);
        }

    }


    private void showView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.clearAnimation();
        view.startAnimation(mViewShowAnimation);
        view.setVisibility(View.VISIBLE);
    }

    private void hideView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        view.clearAnimation();
        view.startAnimation(mViewHideAnimation);
        view.setVisibility(View.GONE);
    }

    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param params      涂鸦参数
     * @param requestCode startActivityForResult的请求码
     * @see DoodleParams
     */
    public static void startActivityForResult(Activity activity, DoodleParams params, int requestCode) {
        Intent intent = new Intent(activity, EditPhotoActivity.class);
        intent.putExtra(KEY_PARAMS, params);
        activity.startActivityForResult(intent, requestCode);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_TEXT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String text = data.getStringExtra(AddTextActivity.RESULT_TEXT);
                textSelectedColor = data.getIntExtra(AddTextActivity.RESULT_COLOR, Color.parseColor(colorArr[0]));
                boolean isShowTextBg = data.getBooleanExtra(AddTextActivity.RESULT_IS_DRAW_TEXT_BG, false);
                Rect resultTextRect = data.getParcelableExtra(AddTextActivity.RESULT_RECT);
                mDoodle.setColor(new DoodleColor(textSelectedColor));
                mDoodle.setTextRect(resultTextRect);
                mDoodle.setIsDrawTextBg(isShowTextBg);
                createDoodleText(null, -1, -1, text, isShowTextBg);
            }
        }
    }


    public void resetBitmap(boolean isScale) {
        mDoodleView.setIsRest(false);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mDoodleView.getLayoutParams();
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (isScale) {
            int switchScreenHeight = (int) (screenHeight * 0.79 + 0.5f);
            layoutParams.height = switchScreenHeight;
        }else {
            layoutParams.height = screenHeight;
        }
        mDoodleView.setLayoutParams(layoutParams);
    }
}