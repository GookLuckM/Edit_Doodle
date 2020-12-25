package cn.hzw.doodledemo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.os.PersistableBundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.ucrop.callback.OverlayViewChangeListener;
import com.yalantis.ucrop.view.OverlayView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
import cn.hzw.doodle.util.ImageUtils;
import cn.hzw.doodle.util.LogUtil;
import cn.hzw.doodle.util.StatusBarUtil;
import cn.hzw.doodle.util.Util;

public class EditPhotoActivity extends AppCompatActivity implements View.OnClickListener, ScrawlColorsAdapter.OnColorClickListener, IEditListener {

    private static final int EDIT_TEXT_REQUEST_CODE = 9999;

    public static final String KEY_PARAMS = "key_doodle_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";
    public static final String NONE = "none";
    public static final String MODE_SCRAWL = "mode_scrawl";
    public static final String MODE_ERASER = "mode_eraser";
    public static final String MODE_TEXT = "mode_text";
    public static final String MODE_CROP = "mode_crop";
    public static final String MODE_MOSAIC = "mode_mosaic";


    public static final int RESULT_ERROR = -111; // 出现错误

    private DoodleParams mDoodleParams;
    private String mImagePath;

    private FrameLayout mFrameLayout;
    private IDoodle mDoodle;
    private DoodleView mDoodleView;


    private String CURRENT_MODE = NONE;


    private Map<IDoodlePen, Float> mPenSizeMap = new HashMap<>(); //保存每个画笔对应的最新大小


    private int mMosaicLevel;
    private int mMosaicSize;

    private DoodleOnTouchGestureListener mTouchGestureListener;
    private FragmentManager fragmentManager;

    private BaseEditFragment editFragment;
    private LinearLayout llEdit;
    private FrameLayout editLayout;
    private DoodleShape selectedShape = DoodleShape.HAND_WRITE;
    private int textSelectedColor = Color.RED;

    private int selectedColor = Color.parseColor("#FA5051");
    private int mScrawlSize;
    private OverlayView mUCropFrame;
    private float scaleAnimTransX;
    private float scaleAnimTranY;
    private float animScale;
    private int mCenterWidth;
    private int mCenterHeight;

    private int doodleOriginCenterWidth;
    private int doodleOriginCenterHeight;
    private AnimatorSet outAnimatorSet;
    private AnimatorSet inAnimatorSet;
    private float centerX;
    private float centerY;
    private float mCropTransAnimY;
    private ValueAnimator mInScaleAnimator;
    private ValueAnimator mOutScaleAnimator;
    private LinearLayout llDeleteRectView;
    private ImageView ivDelete;
    private TextView tvDelete;
    private int[] deleteLocation;
    private float originScale;
    private float originTransX;
    private float originTransY;
    private float doodleOriginCenterScale;
    private float doodleOriginTransX;
    private float doodleOriginTransY;
    private float cropScale = 1f;
    private ValueAnimator mRotateAnimator;
    private int textAlignmentMode;
    private float mSelectedRatio;
    private float cropTransX;
    private float cropTransY;
    private int screenWidth;
    private int switchScreenHeight;
    private int screenHeight;


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

        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

        mFrameLayout = (FrameLayout) findViewById(cn.hzw.doodle.R.id.doodle_container);
        fragmentManager = getSupportFragmentManager();

        initView();
        initParams();
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
                    if (!dcimFile.exists()) {
                        dcimFile.mkdir();
                    }
                    doodleFile = new File(dcimFile, "Doodle");
                    //　保存的路径`
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

                /*float size = mDoodleParams.mPaintUnitSize > 0 ? mDoodleParams.mPaintUnitSize * mDoodle.getUnitSize() : 0;
                if (size <= 0) {
                    size = mDoodleParams.mPaintPixelSize > 0 ? mDoodleParams.mPaintPixelSize : mDoodle.getSize();
                }*/
                // 设置初始值
                mDoodle.setSize(mScrawlSize);
                // 选择画笔
                mDoodle.setPen(DoodlePen.BRUSH);
                mDoodle.setShape(DoodleShape.HAND_WRITE);
                mDoodle.setColor(new DoodleColor(selectedColor));
                mDoodleView.setEditMode(true);
                mTouchGestureListener.setSupportScaleItem(mDoodleParams.mSupportScaleItem);

                doodleOriginCenterHeight = mDoodleView.getCenterHeight();
                doodleOriginCenterWidth = mDoodleView.getCenterWidth();
                doodleOriginCenterScale = mDoodleView.getCenterScale();
                doodleOriginTransX = mDoodleView.getCentreTranX();
                doodleOriginTransY = mDoodleView.getCentreTranY();

            }
        });


        mDoodleView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                refreshEditBackOrNextStatus();
                // 隐藏设置面板
                if (mDoodleParams.mChangePanelVisibilityDelay > 0) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            llEdit.setVisibility(View.GONE);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            if (editFragment == null) {
                                llEdit.setVisibility(View.VISIBLE);
                            }
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
                    mDoodle.setPen(selectableItem.getPen());
                    mDoodle.setColor(selectableItem.getColor());
                    mDoodle.setSize(selectableItem.getSize());
                    selectableItem.addItemListener(mIDoodleItemListener);
                } else {
                    selectableItem.removeItemListener(mIDoodleItemListener);
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
                    createDoodleText(null, x, y, "", false, textAlignmentMode);
                }

            }

            @Override
            public void onChangeSelectedItemLocation(IDoodleSelectableItem selectableItem) {


                if (deleteLocation != null && selectableItem.contains(mDoodleView.toX(deleteLocation[0]), mDoodleView.toY(deleteLocation[1]))) {
                    llDeleteRectView.setBackgroundResource(R.drawable.bg_delete_rect_red);
                    ivDelete.setImageResource(R.drawable.icon_delete_open);
                    tvDelete.setText("松手即可删除");
                } else {
                    llDeleteRectView.setBackgroundResource(R.drawable.bg_delete_rect);
                    ivDelete.setImageResource(R.drawable.icon_delete);
                    tvDelete.setText("拖动到此处删除");
                }
            }

            @Override
            public void showDeleteRect(IDoodleSelectableItem selectableItem, boolean isShow) {
                if (isShow) {
                    llDeleteRectView.setVisibility(View.VISIBLE);
                    deleteLocation = new int[2];
                    llDeleteRectView.getLocationOnScreen(deleteLocation);
                } else {
                    if (selectableItem.contains(mDoodleView.toX(deleteLocation[0]), mDoodleView.toY(deleteLocation[1]))) {
                        mDoodle.removeItem(selectableItem);
                    }
                    llDeleteRectView.setVisibility(View.INVISIBLE);
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
        mFrameLayout.addView(mDoodleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mDoodle.setDoodleMinScale(mDoodleParams.mMinScale);
        mDoodle.setDoodleMaxScale(mDoodleParams.mMaxScale);

    }

    private void initParams() {
        mMosaicLevel = getResources().getDimensionPixelOffset(R.dimen.dp_8);
        mMosaicSize = mMosaicLevel * 3;
        mScrawlSize = getResources().getDimensionPixelSize(R.dimen.dp_7);
    }

    public void initView() {
        editLayout = findViewById(R.id.frag_view);
        llEdit = findViewById(R.id.ll_edit);
        llDeleteRectView = findViewById(R.id.ll_delete_rect);
        ivDelete = findViewById(R.id.iv_delete);
        tvDelete = findViewById(R.id.tv_delete);

        TextView tvDone = findViewById(R.id.tv_done);
        ImageView ivScrawl = findViewById(R.id.iv_scrawl);
        ImageView ivText = findViewById(R.id.iv_text);
        ImageView ivMosaic = findViewById(R.id.iv_mosaic);
        ImageView ivCrop = findViewById(R.id.iv_crop);
       /* edit_frag = findViewById(R.id.edit_frag);
        shapeStubView = findViewById(R.id.scrawl_shape_edit);*/
        ivScrawl.setOnClickListener(this);
        ivMosaic.setOnClickListener(this);
        ivText.setOnClickListener(this);
        ivCrop.setOnClickListener(this);
        tvDone.setOnClickListener(this);



        /*CURRENT_MODE = MODE_TEXT;

        mDoodle.setPen(DoodlePen.TEXT);
        startActivityForResult(new Intent(EditPhotoActivity.this, AddTextActivity.class), EDIT_TEXT_REQUEST_CODE);


        CURRENT_MODE = MODE_CROP;
        resetBitmap(false,0);


        CURRENT_MODE = MODE_MOSAIC;
        mDoodle.setPen(DoodlePen.MOSAIC);


        mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3;
        mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
        if (mTouchGestureListener.getSelectedItem() != null) {
            mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
        }

        mMosaicLevel = DoodlePath.MOSAIC_LEVEL_2;
        mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
        if (mTouchGestureListener.getSelectedItem() != null) {
            mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
        }*/


    }


    @Override
    public void onClick(View v) {
        if (mDoodleView.isScrollingDoodle()) {
            return;
        }

        if (v.getId() == R.id.tv_done) {
            mDoodle.save();
        } else if (v.getId() == R.id.iv_scrawl) {
            if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
                mUCropFrame.setVisibility(View.GONE);
            }
            CURRENT_MODE = MODE_SCRAWL;
            mDoodleView.setEditMode(false);
            llEdit.setVisibility(View.GONE);
            showFragment(MODE_SCRAWL);
            /*if (scrawlEditView == null) {
                scrawlEditView = edit_frag.inflate();
                initScrawlEditView();
            }
            scrawlEditView.setVisibility(View.VISIBLE);
            scrawlEditView.post(new Runnable() {
                @Override
                public void run() {
                    //resetBitmap(true,scrawlEditView.getMeasuredHeight());
                    //scrawlEditView.setTranslationY(-scrawlEditView.getMeasuredHeight());
                    editViewAnimIn(scrawlEditView);
                }
            });*/


        } else if (v.getId() == R.id.iv_mosaic) {
            if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
                mUCropFrame.setVisibility(View.GONE);
            }
            CURRENT_MODE = MODE_MOSAIC;
            mDoodleView.setEditMode(false);
            llEdit.setVisibility(View.GONE);
            showFragment(MODE_MOSAIC);
        } else if (v.getId() == R.id.iv_text) {
            if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
                mUCropFrame.setVisibility(View.GONE);
            }
            CURRENT_MODE = MODE_TEXT;
            mDoodle.setPen(DoodlePen.TEXT);
            startActivityForResult(new Intent(EditPhotoActivity.this, AddTextActivity.class), EDIT_TEXT_REQUEST_CODE);
        } else if (v.getId() == R.id.iv_crop) {
            CURRENT_MODE = MODE_CROP;
            mDoodleView.setEditMode(true);
            llEdit.setVisibility(View.GONE);
            mDoodle.setDoodleCropRect(null, 1);
            showFragment(MODE_CROP);
            //onRotate();


        }/*else if (v.getId() == R.id.doodle_btn_back) {
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

    @Override
    public void onColorClick(String color) {
        selectedColor = Color.parseColor(color);
        mDoodle.setColor(new DoodleColor(Color.parseColor(color)));
    }

    @Override
    public void setColor(int color) {
        selectedColor = color;
        if (mDoodle.getPen().equals(DoodlePen.BRUSH)) {
            mDoodle.setColor(new DoodleColor(color));
        }
    }

    @Override
    public void setMode(DoodlePen doodlePen) {
        if (doodlePen.equals(DoodlePen.ERASER)) {
            CURRENT_MODE = MODE_ERASER;
            mDoodle.setShape(DoodleShape.HAND_WRITE);
        } else if (doodlePen.equals(DoodlePen.ERASER)) {
            CURRENT_MODE = MODE_SCRAWL;
            mDoodle.setShape(selectedShape);
        } else if (doodlePen.equals(DoodlePen.MOSAIC)) {
            CURRENT_MODE = MODE_MOSAIC;
            mDoodle.setShape(DoodleShape.HAND_WRITE);
        }
        mDoodle.setPen(doodlePen);
    }

    @Override
    public void setSize(int size) {
        mScrawlSize = size;
        mDoodle.setSize(size);
    }

    @Override
    public void setMosaicSize(int size) {
        mMosaicSize = size;
        mDoodle.setSize(size);
    }

    @Override
    public void setMosaicLevel(int mosaicLevel) {
        mMosaicLevel = mosaicLevel;
        mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
    }

    @Override
    public void setShape(DoodleShape doodleShape) {
        selectedShape = doodleShape;
        mDoodle.setPen(DoodlePen.BRUSH);
        mDoodle.setShape(doodleShape);
    }

    @Override
    public void onClose() {
        if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
            mUCropFrame.setVisibility(View.GONE);
        }
        switch (CURRENT_MODE) {
            case MODE_ERASER:
            case MODE_SCRAWL:
                hideFragment(MODE_SCRAWL);
                break;
            case MODE_MOSAIC:
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                mDoodleView.setDoodleCropRect(null, 2);
                hideFragment(MODE_CROP);
                break;
        }
    }

    @Override
    public void onDone() {
        switch (CURRENT_MODE) {
            case MODE_ERASER:
            case MODE_SCRAWL:
                hideFragment(MODE_SCRAWL);
                break;
            case MODE_MOSAIC:
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                hideFragment(MODE_CROP);
                if (mUCropFrame != null) {
                    RectF cropViewRect = mUCropFrame.getCropViewRect();
                    resetDoodleCropSize(cropViewRect);
                    mDoodleView.setDoodleCropRect(cropViewRect, 0);
                    if (mUCropFrame.getVisibility() == View.VISIBLE) {
                        mUCropFrame.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }

    private void resetDoodleCropSize(RectF cropViewRect) {
        RectF doodleBound = mDoodleView.getDoodleBound();
        float sw = cropViewRect.width() * 1f / doodleBound.width();
        float sh = cropViewRect.height() * 1f / doodleBound.height();
        if (sw > sh) {
            cropScale = 1f / sw;
        } else {
            cropScale = 1f / sh;
        }
    }


    public void showFragment(String showTag) {
        List<Fragment> fragments = fragmentManager.getFragments();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //fragmentTransaction.setCustomAnimations(R.anim.edit_fragment_in,R.anim.edit_fragment_out);
        for (Fragment fragment1 : fragments) {
            fragmentTransaction.hide(fragment1);
        }
        editFragment = (BaseEditFragment) getSupportFragmentManager().findFragmentByTag(showTag);
        if (editFragment == null) {
            switch (showTag) {
                case MODE_SCRAWL:
                    // 没有找到表示没有被创建过
                    editFragment = new EditScrawlFragment();
                    break;
                case MODE_MOSAIC:
                    editFragment = new EditMosaicFragment();
                    break;
                case MODE_CROP:
                    editFragment = new EditCropFragment();
                    Bitmap bitmap = mDoodle.getBitmap();
                    float w = bitmap.getWidth() * 1f;
                    float h = bitmap.getHeight() * 1f;
                    Bundle bundle = new Bundle();
                    bundle.putFloat(EditCropFragment.EXTRA_ORIGIN_RATIO, w / h);
                    editFragment.setArguments(bundle);
                    break;
            }
            editFragment.setEditListener(this);
            // 直接add
            fragmentTransaction.add(R.id.frag_view, editFragment, showTag);
        } else {
            // 找到了，表示已经被add了，所以直接show
            fragmentTransaction.show(editFragment);
        }
        fragmentTransaction.commit();
        /*editLayout.post(new Runnable() {
            @Override
            public void run() {
                editViewAnimIn(editLayout);
            }
        });*/
        editLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (editLayout.getMeasuredHeight() > 0) {
                    editViewAnimIn(editLayout);
                    editLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

    }


    public void hideFragment(String hideTag) {
        BaseEditFragment fragment = (BaseEditFragment) fragmentManager.findFragmentByTag(hideTag);
        editFragment = null;
        editViewAnimOut(editLayout, fragment);
        llEdit.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDown() {

    }

    @Override
    public void onPre() {
        mDoodle.undo();
        refreshEditBackOrNextStatus();
    }

    @Override
    public void onNext() {
        mDoodle.redo();
        refreshEditBackOrNextStatus();
    }

    @Override
    public void onCropRatioChange(float ratio) {
        if (ratio != -1f) {
            mSelectedRatio = ratio;
        }
        if (mUCropFrame != null) {
            mUCropFrame.setTargetAspectRatio(mSelectedRatio);
        }
    }

    @Override
    public void onRotate() {
        // 旋转图片
        mDoodle.setDoodleRotation(mDoodle.getDoodleRotation() + 90);

        RectF doodleBound = mDoodleView.getDoodleBound();
        changeDoodleSize(editLayout.getMeasuredHeight());
        mDoodleView.setDoodleScale(animScale, 0, 0);
        mDoodle.setDoodleTranslation(scaleAnimTransX, scaleAnimTranY);
        System.out.println(doodleBound.toString());
        if (mUCropFrame != null) {
            mUCropFrame.setOriginRect(mDoodleView.getDoodleBound());
            switch (mDoodle.getDoodleRotation()) {
                case 0:
                    mUCropFrame.setTargetAspectRatio(mDoodle.getBitmap().getWidth() * 1f / mDoodle.getBitmap().getHeight() * 1f);
                case 180:
                    mUCropFrame.setTargetAspectRatio(mDoodle.getBitmap().getWidth() * 1f / mDoodle.getBitmap().getHeight() * 1f);
                    break;
                case 90:
                    mUCropFrame.setTargetAspectRatio(mDoodle.getBitmap().getHeight() * 1f / mDoodle.getBitmap().getWidth() * 1f);
                case 270:
                    mUCropFrame.setTargetAspectRatio(mDoodle.getBitmap().getHeight() * 1f / mDoodle.getBitmap().getWidth() * 1f);
                    break;
            }
        }
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
                mDoodle.setSize(mScrawlSize);
                mDoodle.setColor(new DoodleColor(selectedColor));
            } else if (pen == DoodlePen.MOSAIC) {
                mDoodle.setSize(mMosaicSize);
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
                mDoodle.setIsDrawableOutside(false);


            } else {
                if (mLastIsDrawableOutside != null) { // restore
                    mDoodle.setIsDrawableOutside(mLastIsDrawableOutside);
                }
                mTouchGestureListener.center(); // center picture
                if (mTouchGestureListener.getSelectedItem() == null) { // restore
                    setPen(getPen());
                }


                mTouchGestureListener.setSelectedItem(null);

            }
        }

    }


    // 添加文字
    private void createDoodleText(final DoodleText doodleText, final float x,
                                  final float y, String text, boolean isShowTextBg, int alignmentMode) {
        if (isFinishing()) {
            return;
        }

        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (doodleText == null) {
            IDoodleSelectableItem item = new DoodleText(mDoodle, text, mDoodle.getSize(), mDoodle.getColor().copy(), x, y, isShowTextBg, alignmentMode);
            mDoodle.addItem(item);
            mTouchGestureListener.setSelectedItem(item);
        } else {
            doodleText.setText(text);
        }
        mDoodle.refresh();

        if (doodleText == null) {

        }

    }


    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param params      涂鸦参数
     * @param requestCode startActivityForResult的请求码
     * @see DoodleParams
     */
    public static void startActivityForResult(Activity activity, DoodleParams params,
                                              int requestCode) {
        Intent intent = new Intent(activity, EditPhotoActivity.class);
        intent.putExtra(KEY_PARAMS, params);
        activity.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onBackPressed() {
        if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
            mUCropFrame.setVisibility(View.GONE);
        }
        if (editFragment != null) {
            hideFragment(CURRENT_MODE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_TEXT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String text = data.getStringExtra(AddTextActivity.RESULT_TEXT);
                textSelectedColor = data.getIntExtra(AddTextActivity.RESULT_COLOR, Color.parseColor("#FA5051"));
                textAlignmentMode = data.getIntExtra(AddTextActivity.RESULT_ALIGNMENT, AddTextActivity.ALIGNMENT_LEFT);
                boolean isShowTextBg = data.getBooleanExtra(AddTextActivity.RESULT_IS_DRAW_TEXT_BG, false);
                Rect resultTextRect = data.getParcelableExtra(AddTextActivity.RESULT_RECT);
                mDoodle.setColor(new DoodleColor(textSelectedColor));
                mDoodle.setTextRect(resultTextRect);
                mDoodle.setIsDrawTextBg(isShowTextBg);
                createDoodleText(null, mDoodleView.getDoodleBound().left, mDoodleView.getDoodleBound().top + mDoodleView.getDoodleBound().height() / 2, text, isShowTextBg, textAlignmentMode);
            }
        }
    }

    public void editViewAnimIn(final View view) {
        changeDoodleSize(view.getMeasuredHeight());
        mDoodleView.setScreenChange(screenWidth, switchScreenHeight);
        mDoodleView.setDoodleMinScale(animScale);
        if (mInScaleAnimator == null) {
            mInScaleAnimator = new ValueAnimator();
            mInScaleAnimator.setDuration(1000);
            mInScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    float fraction = animation.getAnimatedFraction();
                    mDoodle.setDoodleScale(value, 0, 0);
                    mDoodle.setDoodleTranslation(scaleAnimTransX * fraction, scaleAnimTranY * fraction);

                }
            });
        }

        mInScaleAnimator.setFloatValues(1, animScale);


        if (inAnimatorSet == null) {
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0, -view.getMeasuredHeight()).setDuration(500);
            inAnimatorSet = new AnimatorSet();
            inAnimatorSet.playTogether(translateAnimator, mInScaleAnimator);
            //animatorSet.playTogether(translateAnimator, scaleXAnimator,scaleYAnimator,doodleTranslateAnimator);
            inAnimatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    //mDoodleView.resetBitmapLocation(1.0f, animScale * doodleOriginCenterScale, centerX, centerY, mCenterWidth, mCenterHeight);
                    //mDoodleView.setDoodleScale(1 / animScale, 0, 0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                    switch (CURRENT_MODE) {
                        case MODE_SCRAWL:
                            mDoodle.setPen(DoodlePen.BRUSH);
                            mDoodle.setColor(new DoodleColor(selectedColor));
                            mDoodle.setSize(mScrawlSize);
                            mDoodle.setShape(selectedShape);
                            break;
                        case MODE_MOSAIC:
                            mDoodle.setPen(DoodlePen.MOSAIC);
                            mDoodle.setSize(mMosaicSize);
                            mDoodle.setShape(DoodleShape.HAND_WRITE);
                            break;
                        case MODE_CROP:
                            Bitmap bitmap = mDoodle.getBitmap();
                            float w = bitmap.getWidth() * 1f;
                            float h = bitmap.getHeight() * 1f;
                            float ratio = w / h;
                            switch (mDoodle.getDoodleRotation()) {
                                case 0:
                                case 180:
                                    ratio = w / h;
                                    break;
                                case 90:
                                case 270:
                                    ratio = h / w;
                                    break;
                            }

                            initUCropFrame();
                            mUCropFrame.setTargetAspectRatio(ratio);
                            mSelectedRatio = ratio;
                            break;
                    }

                    //resetBitmap(true, view.getMeasuredHeight());

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

        }
        inAnimatorSet.start();


    }


    private void changeDoodleSize(int height) {
        RectF doodleBound = mDoodleView.getDoodleBound();

        float w = doodleBound.width();
        float h = doodleBound.height();

        switchScreenHeight = screenHeight - height - getResources().getDimensionPixelOffset(R.dimen.dp_9);


        float nw = w * 1f / screenWidth * 1f;
        float nh = h * 1f / switchScreenHeight * 1f;
        float scale = 1f;
        if (nw > nh) {
            scale = 1 / nw;
            mCenterWidth = screenWidth;
            mCenterHeight = (int) (h * scale);
        } else {
            scale = 1 / nh;
            mCenterWidth = (int) (w * scale);
            mCenterHeight = switchScreenHeight;
        }

        animScale = scale;

        if (animScale < 1){
            mDoodleView.setDoodleMinScale(animScale);
        }

        // 使图片居中
        centerX = (screenWidth - mCenterWidth) / 2f;
        centerY = (switchScreenHeight - mCenterHeight) / 2f;

        originScale = mDoodleView.getAllScale();
        switch (mDoodleView.getDoodleRotation()) {
            case 0:
                scaleAnimTransX = centerX - doodleBound.left;
                scaleAnimTranY = centerY - doodleBound.top;
                break;
            case 90:
                scaleAnimTransX = centerY - doodleBound.top;
                scaleAnimTranY = centerX - doodleBound.left;
                break;
            case 180:
                scaleAnimTransX = centerX - doodleBound.left ;
                scaleAnimTranY = centerY - doodleBound.top + height;
                break;
            case 270:
                scaleAnimTransX = centerY - doodleBound.top  + height;
                scaleAnimTranY = centerX - doodleBound.left;

                break;
        }


    }

    public void editViewAnimOut(View view, final BaseEditFragment fragment) {
        //changeDoodleSize(view.getMeasuredHeight());
        mDoodleView.setScreenChange(screenWidth, screenHeight);
        if (mOutScaleAnimator == null) {
            mOutScaleAnimator = new ValueAnimator();
            mOutScaleAnimator.setDuration(1000);
            mOutScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    float fraction = animation.getAnimatedFraction();
                    mDoodle.setDoodleScale(value, 0, 0);
                    mDoodle.setDoodleTranslation(scaleAnimTransX * (1 - fraction), scaleAnimTranY * (1 - fraction));
                }
            });
        }
        mOutScaleAnimator.setFloatValues(animScale, 1);

        if (outAnimatorSet == null) {
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", -view.getMeasuredHeight(), 0).setDuration(1000);
            outAnimatorSet = new AnimatorSet();
            outAnimatorSet.playTogether(translateAnimator, mOutScaleAnimator);
            outAnimatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    //mDoodleView.resetBitmapLocation(1.0f, doodleOriginCenterScale, doodleOriginTransX, doodleOriginTransY, doodleOriginCenterWidth, doodleOriginCenterHeight);
                    //mDoodleView.setDoodleScale(1 / animScale, 0, 0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (fragmentManager != null && fragment != null) {
                        fragmentManager.beginTransaction().hide(fragment).commit();
                        mDoodleView.setEditMode(true);
                    }
                    mDoodleView.setDoodleMinScale(1f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        outAnimatorSet.start();

    }


    private void refreshEditBackOrNextStatus() {
        if (mDoodle != null && editFragment != null) {
            List<IDoodleItem> allItem = mDoodle.getAllItem();
            List<IDoodleItem> allRedoItem = mDoodle.getAllRedoItem();
            boolean isShowBack = false;
            boolean isShowNext = false;
            for (IDoodleItem item : allItem) {
                if (item.getPen().equals(mDoodle.getPen())) {
                    isShowBack = true;
                }
            }

            for (IDoodleItem item : allRedoItem) {
                if (item.getPen().equals(mDoodle.getPen())) {
                    isShowNext = true;
                }
            }

            editFragment.refreshPreOrNextStatus(isShowBack || isShowNext, isShowBack, isShowNext, "");

        }
    }


    public void initUCropFrame() {
        if (mUCropFrame == null) {
            mUCropFrame = new OverlayView(this);
            // Overlay view options
            mUCropFrame.setFreestyleCropEnabled(true);
            mUCropFrame.setDragFrame(true);
            mUCropFrame.setDimmedColor(getResources().getColor(com.yalantis.ucrop.R.color.ucrop_color_default_dimmed));
            mUCropFrame.setCircleDimmedLayer(false);

            mUCropFrame.setShowCropFrame(true);
            mUCropFrame.setCropFrameColor(getResources().getColor(com.yalantis.ucrop.R.color.ucrop_color_default_crop_frame));
            mUCropFrame.setCropFrameStrokeWidth(getResources().getDimensionPixelSize(R.dimen.dp_3));

            mUCropFrame.setShowCropGrid(true);
            mUCropFrame.setCropGridRowCount(OverlayView.DEFAULT_CROP_GRID_ROW_COUNT);
            mUCropFrame.setCropGridColumnCount(OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT);
            mUCropFrame.setCropGridColor(getResources().getColor(com.yalantis.ucrop.R.color.ucrop_color_default_crop_grid));
            mUCropFrame.setCropGridStrokeWidth(getResources().getDimensionPixelSize(com.yalantis.ucrop.R.dimen.ucrop_default_crop_grid_stoke_width));

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            mUCropFrame.setLayoutParams(layoutParams);
            ((ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0)).addView(mUCropFrame);
            mTouchGestureListener.setOverlayView(mUCropFrame);
            mUCropFrame.setOverlayViewChangeListener(new OverlayViewChangeListener() {


                @Override
                public void onCropRectUpdated(RectF cropRect) {
                    /*int centerWidth = mDoodleView.getCenterWidth();
                    int centerHeight = mDoodleView.getCenterHeight();
                    float sw = cropRect.width() / centerWidth * 1f;
                    float sh = cropRect.height() / centerHeight * 1f;
                    if (sw > sh) {
                        mDoodleView.setDoodleMinScale(sw);
                    } else {
                        mDoodleView.setDoodleMinScale(sh);
                    }
                    resetDoodleCropLocation(cropRect);*/
                    //mDoodleView.setCurrentCropRect(cropRect);
                }

                @Override
                public void onCropRectEnd(RectF rectF) {
                    LogUtil.d("DoodleView", "onCropRectEnd");
                    RectF tempRect = new RectF(rectF);
                    mUCropFrame.setTargetAspectRatio(rectF.width() * 1f / rectF.height() * 1f);
                    RectF cropViewRect = mUCropFrame.getCropViewRect();

                    float sw = tempRect.width() * 1f / cropViewRect.width();
                    float sh = tempRect.height() * 1f / cropViewRect.height();
                    float scale = 1f;
                    if (sw > sh) {
                        scale = 1f / sw;
                    } else {
                        scale = 1f / sh;
                    }
                    cropScale = scale * mDoodleView.getDoodleScale();

                    LogUtil.d("text doodleRect: " + mDoodleView.getDoodleBound().toString());
                    LogUtil.d("text originRect: " + tempRect.toString());
                    LogUtil.d("text resultScale: " + scale);
                    LogUtil.d("text cropScale: " + cropScale);
                    LogUtil.d("text resultRect: " + cropViewRect.toString());
                    LogUtil.d("text resultCenterX: " + cropViewRect.left);
                    LogUtil.d("text resultCenterY: " + cropViewRect.top);
                    LogUtil.d("text resultWidth: " + cropViewRect.width());
                    LogUtil.d("text resultHeight: " + cropViewRect.height());
                    LogUtil.d("text centerScale: " + mDoodleView.getCenterScale());
                    LogUtil.d("text centerX: " + mDoodleView.getCentreTranX());
                    LogUtil.d("text centerY: " + mDoodleView.getCentreTranY());
                    LogUtil.d("text rotateScale: " + mDoodleView.getRotateScale());
                    LogUtil.d("text rotateX: " + mDoodleView.getRotateTranX());
                    LogUtil.d("text rotateY: " + mDoodleView.getRotateTranY());
                    LogUtil.d("text tranX: " + mDoodleView.getDoodleTranslationX());
                    LogUtil.d("text tranY: " + mDoodleView.getDoodleTranslationY());


                    LogUtil.d("text allScale: " + mDoodleView.getAllScale());
                    LogUtil.d("text allTransX: " + mDoodleView.getAllTranX());
                    LogUtil.d("text allTransY: " + mDoodleView.getAllTranY());

                    switch (mDoodleView.getDoodleRotation()){
                        case 0:
                            cropTransX = (tempRect.left - mDoodleView.getCentreTranX() - mDoodleView.getDoodleTranslationX()) * scale - cropViewRect.left + mDoodleView.getCentreTranX() ;
                            cropTransY = (tempRect.top - mDoodleView.getCentreTranY() - mDoodleView.getDoodleTranslationY()) * scale - cropViewRect.top + mDoodleView.getCentreTranY();
                            break;
                        case 90:
                            cropTransX = (tempRect.top - mDoodleView.getAllTranY()) * scale - cropViewRect.top + mDoodleView.getCentreTranY() ;
                            cropTransY = (tempRect.left - mDoodleView.getAllTranX()) * scale - cropViewRect.left + mDoodleView.getCentreTranX() ;
                            /*cropTransX = (tempRect.left - mDoodleView.getAllTranX()) * scale - cropViewRect.left + mDoodleView.getCentreTranX() ;
                            cropTransY = (tempRect.top - mDoodleView.getAllTranY()) * scale - cropViewRect.top + mDoodleView.getCentreTranY();*/
                            break;
                        case 180:
                            cropTransX = -((tempRect.left - mDoodleView.getAllTranX()) * scale - cropViewRect.left + mDoodleView.getCentreTranX() + mDoodleView.getRotateTranX());
                            cropTransY = -((tempRect.top - mDoodleView.getAllTranY()) * scale - cropViewRect.top + mDoodleView.getCentreTranY() + mDoodleView.getRotateTranY());
                            break;
                        case 270:
                            cropTransX = -((tempRect.top - mDoodleView.getAllTranY()) * scale - cropViewRect.top + mDoodleView.getCentreTranY() + mDoodleView.getRotateTranY());
                            cropTransY = -((tempRect.left - mDoodleView.getAllTranX()) * scale - cropViewRect.left + mDoodleView.getCentreTranX() + mDoodleView.getRotateTranX());
                            break;

                    }

                    mDoodle.setDoodleScale(cropScale, 0, 0);
                    mDoodle.setDoodleTranslation(-cropTransX, -cropTransY);

                }

            });
        } else {
            mUCropFrame.setVisibility(View.VISIBLE);
        }

        mUCropFrame.setOriginRect(mDoodleView.getDoodleBound());
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            if (inAnimatorSet != null) {
                inAnimatorSet.cancel();
            }

            if (outAnimatorSet != null) {
                outAnimatorSet.cancel();
            }
        }
    }


    private void resetDoodleCropLocation(RectF cropViewRect) {
        RectF bound = mDoodleView.getDoodleBound();
        float x = mDoodle.getDoodleTranslationX() , y = mDoodle.getDoodleTranslationY();

        float cropLeft = cropViewRect.left;
        float cropTop = cropViewRect.top;
        float cropRight = cropViewRect.right;
        float cropBottom = cropViewRect.bottom;

        float bitmapLeft = bound.left;
        float bitmapTop = bound.top;
        float bitmapRight = bound.right;
        float bitmapBottom = bound.bottom;

        if (bitmapLeft <= cropLeft && bitmapRight >= cropRight && bitmapTop <= cropTop && bitmapBottom >= cropBottom) {
            return;
        } else {
            if (bitmapLeft > cropLeft) {
                x = cropLeft;
            }

            if (bitmapRight < cropRight) {
                x = cropRight - bound.width();
            }


            if (bitmapTop > cropTop) {
                y = cropTop;
            }

            if (bitmapBottom < cropBottom) {
                y = cropBottom - bound.height();
            }

        }

        switch (mDoodleView.getDoodleRotation()){
            case 0:
                break;
            case 90:
                break;
            case 180:
                break;
            case 270:
                break;
        }
        mCropTransAnimY = y - mDoodleView.getCentreTranY();
        mDoodle.setDoodleTranslation(x - mDoodleView.getCentreTranX(), mCropTransAnimY);
    }
}