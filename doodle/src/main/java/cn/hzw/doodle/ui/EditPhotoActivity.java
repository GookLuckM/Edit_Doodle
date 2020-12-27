package cn.hzw.doodle.ui;

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
import cn.hzw.doodle.R;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleItemListener;
import cn.hzw.doodle.core.IDoodlePen;
import cn.hzw.doodle.core.IDoodleSelectableItem;
import cn.hzw.doodle.core.IDoodleShape;
import cn.hzw.doodle.core.IDoodleTouchDetector;
import cn.hzw.doodle.util.DimenUtils;
import cn.hzw.doodle.util.ImageUtils;
import cn.hzw.doodle.util.LogUtil;
import cn.hzw.doodle.util.StatusBarUtil;
import cn.hzw.doodle.util.Util;
import cn.hzw.doodledemo.IEditListener;

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
    private int mScrawlIndex;
    private OverlayView mUCropFrame;
    private float scaleAnimTransX;
    private float scaleAnimTranY;
    private float animScale;
    private int mCenterWidth;
    private int mCenterHeight;


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
    private float cropScale = 1f;
    private int textAlignmentMode;
    private float mSelectedRatio;
    private float cropTransX = -1f;
    private float cropTransY = -1f;
    private int screenWidth;
    private int switchScreenHeight;
    private int screenHeight;
    private float mPivotX;
    private float mPivotY;
    private RectF mCropViewRect;
    private float preCropScale;
    private float preCropTransX;
    private float preCropTransY;


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
                // 设置初始值
                mDoodle.setSize(mScrawlSize, 0);
                // 选择画笔
                mDoodle.setPen(DoodlePen.BRUSH);
                mDoodle.setShape(DoodleShape.HAND_WRITE);
                mDoodle.setColor(new DoodleColor(selectedColor));
                mDoodleView.setEditMode(true);
                mTouchGestureListener.setSupportScaleItem(mDoodleParams.mSupportScaleItem);
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
            Integer mSizeIndex;

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

                    if (mSizeIndex == null) {
                        mSizeIndex = mDoodle.getSizeIndex();
                    }
                    mDoodle.setPen(selectableItem.getPen());
                    mDoodle.setColor(selectableItem.getColor());
                    mDoodle.setSize(selectableItem.getSize(), mScrawlSize);
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
                            mDoodle.setSize(mSize, mSizeIndex);
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
        mMosaicLevel = DimenUtils.dp2px(this, 8);
        mMosaicSize = mMosaicLevel * 3;
        mScrawlSize = DimenUtils.dp2px(this, 7);
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
        ivScrawl.setOnClickListener(this);
        ivMosaic.setOnClickListener(this);
        ivText.setOnClickListener(this);
        ivCrop.setOnClickListener(this);
        tvDone.setOnClickListener(this);
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
            preCropScale = cropScale;
            preCropTransX = cropTransX;
            preCropTransY = cropTransY;
            mDoodleView.setEditMode(true);
            llEdit.setVisibility(View.GONE);
            mDoodle.setDoodleCropRect(null, 1);
            showFragment(MODE_CROP);
        }
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
    public void setSize(int size, int index) {
        mScrawlSize = size;
        mScrawlIndex = index;
        mDoodle.setSize(size, index);
    }

    @Override
    public void setMosaicSize(int size, int index) {
        mMosaicSize = size;
        mDoodle.setSize(size, -1);
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
                mDoodle.cleanDoodle();
                hideFragment(MODE_SCRAWL);
                break;
            case MODE_MOSAIC:
                mDoodle.cleanMosaic();
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                cropScale = preCropScale;
                cropTransX = preCropTransX;
                cropTransY = preCropTransY;
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
                for (IDoodleItem iDoodleItem : mDoodleView.getDoodleBeforeDrawItem()) {
                    mDoodleView.notifyItemFinishedDrawing(iDoodleItem);
                }
                hideFragment(MODE_SCRAWL);
                break;
            case MODE_MOSAIC:
                for (IDoodleItem iDoodleItem : mDoodleView.getMosaicBeforeDrawItem()) {
                    mDoodleView.notifyItemFinishedDrawing(iDoodleItem);
                }
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                if (mUCropFrame != null) {
                    mCropViewRect = mUCropFrame.getCropViewRect();
                    resetDoodleCropSize(mCropViewRect);
                    mDoodleView.setDoodleCropRect(mCropViewRect, 0);
                    if (mUCropFrame.getVisibility() == View.VISIBLE) {
                        mUCropFrame.setVisibility(View.GONE);
                    }


                }
                hideFragment(MODE_CROP);
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
        if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
            mDoodle.undo();
        }else if (CURRENT_MODE == MODE_MOSAIC){
            mDoodle.mosaicUndo();
        }
        refreshEditBackOrNextStatus();
    }

    @Override
    public void onNext() {
        if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
            mDoodle.redo();
        }else if (CURRENT_MODE == MODE_MOSAIC){
            mDoodle.mosaicRedo();
        }
        refreshEditBackOrNextStatus();
    }

    @Override
    public void onCropRatioChange(float ratio) {
        if (ratio == -1f) {
            ratio = mSelectedRatio;
            mUCropFrame.setIsLimit(false);
        } else if (ratio == 99f) {
            Bitmap bitmap = mDoodle.getBitmap();
            float w = bitmap.getWidth() * 1f;
            float h = bitmap.getHeight() * 1f;
            mSelectedRatio = w / h;
            mUCropFrame.setIsLimit(false);
        } else {
            mSelectedRatio = ratio;
            mUCropFrame.setIsLimit(true);
        }

        if (mUCropFrame != null) {
            mUCropFrame.setTargetAspectRatio(mSelectedRatio);
        }
    }

    @Override
    public void onRotate() {
        // 旋转图片
        mDoodle.setDoodleRotation(mDoodle.getDoodleRotation() + 90);
        changeDoodleSize(editLayout.getMeasuredHeight());
        mDoodleView.setDoodleScale(animScale, 0, 0);
        mDoodle.setDoodleTranslation(scaleAnimTransX, scaleAnimTranY);
        mDoodleView.setRotateOriginBound(mDoodleView.getDoodleBound());
        if (mUCropFrame != null) {
            mUCropFrame.setOriginRect(mDoodleView.getRotateOriginDoodleBound());
            float ratio = -1f;
            switch (mDoodle.getDoodleRotation()) {
                case 0:
                case 180:
                    ratio = mDoodle.getBitmap().getWidth() * 1f / mDoodle.getBitmap().getHeight() * 1f;
                    break;
                case 90:
                case 270:
                    ratio = mDoodle.getBitmap().getHeight() * 1f / mDoodle.getBitmap().getWidth() * 1f;
                    break;
            }
            if (editFragment != null && editFragment instanceof EditCropFragment) {
                ((EditCropFragment) editFragment).setCropRatio(ratio);
            }
            mUCropFrame.setTargetAspectRatio(ratio);
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
                mDoodle.setSize(mScrawlSize, mScrawlIndex);
                mDoodle.setColor(new DoodleColor(selectedColor));
            } else if (pen == DoodlePen.MOSAIC) {
                mDoodle.setSize(mMosaicSize, -1);
                mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
            } else if (pen == DoodlePen.TEXT) {
                mDoodle.setSize(DimenUtils.dp2px(EditPhotoActivity.this, 28) / mDoodleView.getAllScale(), -1);
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
        public void setSize(float paintSize, int index) {
            super.setSize(paintSize, index);

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
        if (cropTransX != -1 && cropTransY != -1) {
            scaleAnimTransX = cropTransX;
            scaleAnimTranY = cropTransY;
        } else {
            changeDoodleSize(view.getMeasuredHeight());
        }
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

        mInScaleAnimator.setFloatValues(1, animScale * cropScale);


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
                            mDoodle.setSize(mScrawlSize, mScrawlIndex);
                            mDoodle.setShape(selectedShape);
                            break;
                        case MODE_MOSAIC:
                            mDoodle.setPen(DoodlePen.MOSAIC);
                            mDoodle.setSize(mMosaicSize, -1);
                            mDoodle.setShape(DoodleShape.HAND_WRITE);
                            break;
                        case MODE_CROP:
                            if (mCropViewRect == null) {
                                mCropViewRect = new RectF(mDoodleView.getDoodleBound());
                            }
                            float ratio = mCropViewRect.width() / mCropViewRect.height();

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

        switchScreenHeight = screenHeight - height - DimenUtils.dp2px(this, 9);

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

        if (animScale < 1) {
            mDoodleView.setDoodleMinScale(animScale);
        }

        // 使图片居中
        centerX = (screenWidth - mCenterWidth) / 2f;
        centerY = (switchScreenHeight - mCenterHeight) / 2f;

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
                scaleAnimTransX = centerX - doodleBound.left;
                scaleAnimTranY = centerY - doodleBound.top + height;
                break;
            case 270:
                scaleAnimTransX = centerY - doodleBound.top + height;
                scaleAnimTranY = centerX - doodleBound.left;

                break;
        }


    }

    public void editViewAnimOut(View view, final BaseEditFragment fragment) {
        boolean isReverse = false;
        if (cropTransX != -1 && cropTransY != -1) {
            scaleAnimTransX = cropTransX;
            scaleAnimTranY = cropTransY;
            isReverse = true;
        } else {
            changeDoodleSize(view.getMeasuredHeight());
            isReverse = false;
        }
        if (mOutScaleAnimator == null) {
            mOutScaleAnimator = new ValueAnimator();
            mOutScaleAnimator.setDuration(1000);
            final boolean finalIsReverse = isReverse;
            mOutScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = 1 - animation.getAnimatedFraction();
                    if (finalIsReverse) {
                        fraction = 1 - fraction;
                    }
                    mDoodle.setDoodleTranslation(scaleAnimTransX * fraction, scaleAnimTranY * fraction);
                }
            });

            mOutScaleAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mDoodle.setDoodleScale(cropScale, mPivotX, mPivotY);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        mOutScaleAnimator.setFloatValues(animScale * cropScale, cropScale);

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
                    mDoodleView.setDoodleMinScale(cropScale);
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
            List<IDoodleItem> allItem = null;
            List<IDoodleItem> allRedoItem = null;
            if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
                allItem = mDoodle.getAllItem();

                if (allItem == null){
                    allItem = mDoodle.getDoodleBeforeDrawItem();
                }else {
                    allItem.addAll(mDoodle.getDoodleBeforeDrawItem());
                }
                allRedoItem = mDoodle.getAllRedoItem();
            }else if(CURRENT_MODE == MODE_MOSAIC ){
                allItem = mDoodle.getMosaicItem();
                if (allItem == null ){
                    allItem = mDoodle.getMosaicBeforeDrawItem();
                }else {
                    allItem.addAll(mDoodle.getMosaicBeforeDrawItem());
                }
                allRedoItem = mDoodle.getMosaicRedoItem();
            }

            boolean isShowBack = false;
            boolean isShowNext = false;
            if (allItem != null) {
                for (IDoodleItem item : allItem) {
                        isShowBack = true;
                }
            }
            if (allRedoItem != null) {
                for (IDoodleItem item : allRedoItem) {
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
            mUCropFrame.setCropFrameStrokeWidth(DimenUtils.dp2px(this, 3));

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
                    int centerWidth = (int) (mDoodleView.getCenterWidth() * mDoodleView.getRotateScale());
                    int centerHeight = (int) (mDoodleView.getCenterHeight() * mDoodleView.getRotateScale());
                    float sw = 1f;
                    float sh = 1f;
                    switch (mDoodleView.getDoodleRotation()) {
                        case 0:
                        case 180:
                            sw = cropRect.width() / centerWidth * 1f;
                            sh = cropRect.height() / centerHeight * 1f;
                            break;
                        case 90:
                        case 270:
                            sw = cropRect.width() / centerHeight * 1f;
                            sh = cropRect.height() / centerWidth * 1f;
                            break;
                    }

                    if (sw > sh) {
                        mDoodleView.setDoodleMinScale(sw);
                    } else {
                        mDoodleView.setDoodleMinScale(sh);
                    }
                    resetDoodleCropLocation(cropRect);
                }

                @Override
                public void onCropRectEnd(RectF rectF) {
                    LogUtil.d("DoodleView", "onCropRectEnd");
                    RectF tempRect = new RectF(rectF);
                    mUCropFrame.setTargetAspectRatio(rectF.width() * 1f / rectF.height() * 1f);
                    RectF cropViewRect = mUCropFrame.getCropViewRect();
                    RectF doodleBound = mDoodleView.getDoodleBound();
                    float sw = tempRect.width() * 1f / cropViewRect.width();
                    float sh = tempRect.height() * 1f / cropViewRect.height();
                    float scale = 1f;
                    if (sw > sh) {
                        scale = 1f / sw;
                    } else {
                        scale = 1f / sh;
                    }

                    cropScale = scale * mDoodleView.getDoodleScale();

                    mPivotX = 0f;
                    mPivotY = 0f;


                    switch (mDoodleView.getDoodleRotation()) {
                        case 0:
                            mPivotX = (tempRect.left - doodleBound.left) / mDoodleView.getAllScale();
                            mPivotY = (tempRect.bottom - doodleBound.top) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, mPivotX, mPivotY);
                            cropTransX = (cropViewRect.left - tempRect.left) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (cropViewRect.bottom - tempRect.bottom) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 90:
                            mPivotX = (tempRect.bottom - doodleBound.top) / mDoodleView.getAllScale();
                            mPivotY = (doodleBound.right - tempRect.left) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, mPivotX, mPivotY);
                            cropTransX = (cropViewRect.bottom - tempRect.bottom) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (tempRect.left - cropViewRect.left) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 180:
                            mPivotX = (doodleBound.right - tempRect.left) / mDoodleView.getAllScale();
                            mPivotY = (doodleBound.bottom - tempRect.bottom) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, mPivotX, mPivotY);
                            cropTransX = (tempRect.left - cropViewRect.left) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (tempRect.bottom - cropViewRect.bottom) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 270:
                            mPivotX = (doodleBound.bottom - rectF.bottom) / mDoodleView.getAllScale();
                            mPivotY = (tempRect.left - doodleBound.left) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, mPivotX, mPivotY);
                            cropTransX = (tempRect.bottom - cropViewRect.bottom) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (cropViewRect.left - tempRect.left) + mDoodleView.getDoodleTranslationY();
                            break;

                    }


                    mDoodle.setDoodleTranslation(cropTransX, cropTransY);

                }

            });
        } else {
            mUCropFrame.setVisibility(View.VISIBLE);
        }

        mUCropFrame.setOriginRect(mCropViewRect);
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
        float x = mDoodle.getDoodleTranslationX(), y = mDoodle.getDoodleTranslationY();

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
            switch (mDoodle.getDoodleRotation()) {
                case 0:
                    if (bitmapLeft > cropLeft) {
                        x = cropLeft - mDoodleView.getCentreTranX();
                    }

                    if (bitmapRight < cropRight) {
                        x = cropRight - bound.width() - mDoodleView.getCentreTranX();
                    }


                    if (bitmapTop > cropTop) {
                        y = cropTop - mDoodleView.getCentreTranY();
                    }

                    if (bitmapBottom < cropBottom) {
                        y = cropBottom - bound.height() - mDoodleView.getCentreTranY();
                    }
                    break;
                case 90:
                    if (bitmapLeft > cropLeft) {
                        y = bitmapLeft - cropLeft + mDoodleView.getDoodleTranslationY();
                    }

                    if (bitmapRight < cropRight) {
                        y = bitmapRight - cropRight + mDoodleView.getDoodleTranslationY();
                    }


                    if (bitmapTop > cropTop) {
                        x = cropTop - bitmapTop + mDoodleView.getDoodleTranslationX();
                    }

                    if (bitmapBottom < cropBottom) {
                        x = cropBottom - bitmapBottom + mDoodleView.getDoodleTranslationX();
                    }
                    break;
                case 180:
                    if (bitmapLeft > cropLeft) {
                        x = bitmapLeft - cropLeft + mDoodleView.getDoodleTranslationX();
                    }

                    if (bitmapRight < cropRight) {
                        x = bitmapRight - cropRight + mDoodleView.getDoodleTranslationX();
                    }


                    if (bitmapTop > cropTop) {
                        y = bitmapTop - cropTop + mDoodleView.getDoodleTranslationY();
                    }

                    if (bitmapBottom < cropBottom) {
                        y = bitmapBottom - cropBottom + mDoodleView.getDoodleTranslationY();
                    }
                    break;
                case 270:
                    if (bitmapLeft > cropLeft) {
                        y = cropLeft - bitmapLeft + mDoodleView.getDoodleTranslationY();
                    }

                    if (bitmapRight < cropRight) {
                        y = cropRight - bitmapRight + mDoodleView.getDoodleTranslationY();
                    }


                    if (bitmapTop > cropTop) {
                        x = bitmapTop - cropTop + mDoodleView.getDoodleTranslationX();
                    }

                    if (bitmapBottom < cropBottom) {
                        x = bitmapBottom - cropBottom + mDoodleView.getDoodleTranslationX();
                    }
                    break;

            }

        }
        mCropTransAnimY = y - mDoodleView.getCentreTranY();
        mDoodle.setDoodleTranslation(x - mDoodleView.getCentreTranX(), mCropTransAnimY);
    }
}