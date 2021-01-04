package cn.hzw.doodle.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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

import cn.hzw.doodle.OverlayViewChangeListener;
import cn.hzw.doodle.R;
import cn.hzw.doodle.view.OverlayView;

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
import cn.hzw.doodle.util.DimenUtils;
import cn.hzw.doodle.util.ImageUtils;
import cn.hzw.doodle.util.LogUtil;
import cn.hzw.doodle.util.StatusBarUtil;
import cn.hzw.doodle.util.Util;


public class EditPhotoActivity extends AppCompatActivity implements View.OnClickListener, IEditListener {

    private static final int EDIT_TEXT_REQUEST_CODE = 9999;

    public static final String EXTRA_FROM = "extra_from";
    public static final String EXTRA_SEND_TO = "extra_send_to";
    public static final int FROM_ALBUM = 1;
    public static final int FROM_CAMERA = 2;

    public static final String KEY_PARAMS = "key_doodle_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";
    public static final String NONE = "none";
    public static final String MODE_SCRAWL = "mode_scrawl";
    public static final String MODE_ERASER = "mode_eraser";
    public static final String MODE_TEXT = "mode_text";
    public static final String MODE_CROP = "mode_crop";
    public static final String MODE_MOSAIC = "mode_mosaic";
    public static final String MODE_MOSAIC_ERASER = "mode_mosaic_eraser";


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
    private boolean isCropReset;


    /*private float scaleAnimTransX;
    private float scaleAnimTranY;*/


    /*private float animScale;
    private int mCenterWidth;
    private int mCenterHeight;*/


    /*private float centerX;
    private float centerY;*/


    private LinearLayout llDeleteRectView;
    private ImageView ivDelete;
    private TextView tvDelete;

    private int[] deleteLocation;

    private int textAlignmentMode;

    private float mSelectedRatio;


    private int screenWidth;
    private int switchScreenHeight;
    private int screenHeight;


    private RectF mCropViewRect;
    private float cropTransX = 0f;
    private float cropTransY = 0f;
    private float cropScale = 1f;


    private float preCropScale;
    private float preCropTransX;
    private float preCropTransY;
    private RectF mPreCropRect;
    private int mPreRatio = 0;


    private boolean isFirst = true;
    private float editScale = 1f;
    private RectF editRect = new RectF();
    private float editTransX = 0f;
    private float editTransY = 0f;
    private float editWidth;
    private float editHeight;
    private float editX;
    private float editY;


    private RectF origin;
    private RelativeLayout rlContent;
    private String sendTo;
    private int from;
    private TextView tvFinish;
    private FrameLayout flFinish;
    private DoodleText editText;
    private ObjectAnimator mInTranslateAnimator;
    private ObjectAnimator mOutTranslateAnimator;


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

        mFrameLayout = (FrameLayout) findViewById(R.id.doodle_container);
        fragmentManager = getSupportFragmentManager();

        sendTo = getIntent().getStringExtra(EXTRA_SEND_TO);
        from = getIntent().getIntExtra(EXTRA_FROM, 0);

        initView();
        initParams();

        //Util.setToolbarPaddingTop(this, rlContent, 12);
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

                origin = new RectF(mDoodleView.getDoodleBound());
            }
        });


        mDoodleView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                refreshEditBackOrNextStatus();
                refreshWipeShowEnable();
                // 隐藏设置面板
                if (mDoodleParams.mChangePanelVisibilityDelay > 0) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            llEdit.setVisibility(View.GONE);
                            flFinish.setVisibility(View.GONE);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            if (editFragment == null) {
                                llEdit.setVisibility(View.VISIBLE);
                                flFinish.setVisibility(View.VISIBLE);
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
            public void onCreateSelectableItem(IDoodle doodle, DoodleText doodleText, float x, float y) {
                if (mDoodle.getPen() == DoodlePen.TEXT) {
                    if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
                        mUCropFrame.setVisibility(View.GONE);
                    }
                    CURRENT_MODE = MODE_TEXT;
                    editText = doodleText;
                    mDoodle.setPen(DoodlePen.TEXT);
                    Intent intent = new Intent(EditPhotoActivity.this, AddTextActivity.class);
                    intent.putExtra(AddTextActivity.EXTRA_TEXT, doodleText.getText());
                    intent.putExtra(AddTextActivity.EXTRA_ALLIGMENT, doodleText.getAlignmentMode());
                    intent.putExtra(AddTextActivity.EXTRA_COLOR, doodleText.getColor().getColor());
                    intent.putExtra(AddTextActivity.EXTRA_IS_SHOW_BG, doodleText.isShowTextBg());
                    intent.putExtra(AddTextActivity.EXTRA_IS_EDIT, true);

                    startActivityForResult(intent, EDIT_TEXT_REQUEST_CODE);
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
        flFinish = findViewById(R.id.fl_finish);
        tvFinish = findViewById(R.id.tv_finish);

        tvFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        rlContent = findViewById(R.id.rl_content);
        editLayout = findViewById(R.id.frag_view);
        llEdit = findViewById(R.id.ll_edit);
        llDeleteRectView = findViewById(R.id.ll_delete_rect);
        ivDelete = findViewById(R.id.iv_delete);
        tvDelete = findViewById(R.id.tv_delete);
        TextView tvSendTO = findViewById(R.id.tv_sendTo);
        TextView tvDone = findViewById(R.id.tv_done);

        if (from == FROM_CAMERA) {
            if (!TextUtils.isEmpty(sendTo)) {
                tvSendTO.setVisibility(View.VISIBLE);
                tvSendTO.setText("> " + sendTo);
            }
            tvDone.setText("发送");
        } else {
            tvDone.setText("完成");
        }


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
            if (CURRENT_MODE == MODE_ERASER) {
                CURRENT_MODE = MODE_ERASER;
            } else {
                CURRENT_MODE = MODE_SCRAWL;
            }
            mDoodleView.setEditMode(false);
            llEdit.setVisibility(View.GONE);
            showFragment(MODE_SCRAWL);
        } else if (v.getId() == R.id.iv_mosaic) {
            if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
                mUCropFrame.setVisibility(View.GONE);
            }
            if (CURRENT_MODE == MODE_MOSAIC_ERASER) {
                CURRENT_MODE = MODE_MOSAIC_ERASER;
            } else {
                CURRENT_MODE = MODE_MOSAIC;
            }
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
            //进入裁剪界面后,如果点击X退出,裁剪不生效,需要还原至进入前状态.
            mPreRatio = mDoodle.getDoodleRotation();
            if (mCropViewRect != null) {
                mPreCropRect = new RectF(mCropViewRect);
                preCropScale = cropScale;
                preCropTransX = cropTransX;
                preCropTransY = cropTransY;
            }
            mDoodleView.setEditMode(true);
            llEdit.setVisibility(View.GONE);
            mDoodle.setDoodleCropRect(null, 1);
            showFragment(MODE_CROP);
        }
    }


    @Override
    public void setColor(int color) {
        selectedColor = color;
        CURRENT_MODE = MODE_SCRAWL;
        mDoodle.setPen(DoodlePen.BRUSH);
        mDoodle.setShape(selectedShape);
        mDoodle.setColor(new DoodleColor(color));

    }

    @Override
    public void setMode(DoodlePen doodlePen) {
        if (doodlePen.equals(DoodlePen.ERASER)) {
            CURRENT_MODE = MODE_ERASER;
            mDoodle.setShape(DoodleShape.HAND_WRITE);
        } else if (doodlePen.equals(DoodlePen.BRUSH)) {
            CURRENT_MODE = MODE_SCRAWL;
            mDoodle.setShape(selectedShape);
        } else if (doodlePen.equals(DoodlePen.MOSAIC_ERASER)) {
            CURRENT_MODE = MODE_MOSAIC_ERASER;
            mDoodle.setShape(DoodleShape.HAND_WRITE);
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
        if (CURRENT_MODE != MODE_ERASER) {
            mDoodle.setPen(DoodlePen.BRUSH);
            mDoodle.setShape(doodleShape);
        }
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
            case MODE_MOSAIC_ERASER:
            case MODE_MOSAIC:
                mDoodle.cleanMosaic();
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                if (mPreCropRect != null) {
                    if (mCropViewRect == null) {
                        mCropViewRect = new RectF(mPreCropRect);
                    } else {
                        mCropViewRect.set(mPreCropRect);
                    }
                    cropScale = preCropScale;
                    cropTransX = preCropTransX;
                    cropTransY = preCropTransY;
                }
                mDoodleView.setDoodleCropRect(null, 2);
                mDoodleView.setDoodleRotation(mPreRatio);
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
            case MODE_MOSAIC_ERASER:
            case MODE_MOSAIC:
                for (IDoodleItem iDoodleItem : mDoodleView.getMosaicBeforeDrawItem()) {
                    mDoodleView.notifyItemFinishedDrawing(iDoodleItem);
                }
                hideFragment(MODE_MOSAIC);
                break;
            case MODE_CROP:
                if (mUCropFrame != null) {
                    if (mCropViewRect == null) {
                        mCropViewRect = new RectF(mUCropFrame.getCropViewRect());
                    } else {
                        mCropViewRect.set(mUCropFrame.getCropViewRect());
                    }
                    //resetDoodleCropSize(mCropViewRect);
                    mDoodleView.setDoodleCropRect(mCropViewRect, 0);
                    if (mUCropFrame.getVisibility() == View.VISIBLE) {
                        mUCropFrame.setVisibility(View.GONE);
                    }


                }
                hideFragment(MODE_CROP);
                break;
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
                case MODE_ERASER:
                case MODE_SCRAWL:
                    // 没有找到表示没有被创建过
                    editFragment = new EditScrawlFragment();
                    break;
                case MODE_MOSAIC_ERASER:
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
            refreshWipeShowEnable();
            // 找到了，表示已经被add了，所以直接show
            fragmentTransaction.show(editFragment);
        }
        fragmentTransaction.commit();

        editLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (editLayout.getMeasuredHeight() > 0) {
                    if (isFirst) {
                        getChangeEdit(editLayout.getHeight());
                    }
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
    public void onPre() {
        if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
            mDoodle.undo();
        } else if (CURRENT_MODE == MODE_MOSAIC || CURRENT_MODE == MODE_MOSAIC_ERASER) {
            mDoodle.mosaicUndo();
        }
        refreshEditBackOrNextStatus();
        refreshWipeShowEnable();
    }

    @Override
    public void onNext() {
        if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
            mDoodle.redo();
        } else if (CURRENT_MODE == MODE_MOSAIC || CURRENT_MODE == MODE_MOSAIC_ERASER) {
            mDoodle.mosaicRedo();
        }
        refreshEditBackOrNextStatus();
        refreshWipeShowEnable();
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
        //由于setDoodleRotation是将图片旋转后,按照屏幕宽高进行缩放
        //此时仍然在裁剪界面,需重新计算旋转后按照screenWidth和screenHeight - fragment.height比例缩放后的参数并将图片缩放平移到中间位置
        changeCurrentDoodleEdit(editLayout.getMeasuredHeight());
        mDoodleView.setDoodleScaleAndTrans(cropScale, cropTransX, cropTransY);
        if (mUCropFrame != null) {
            //重新计算,裁剪框的最大范围和显示宽高比
            mUCropFrame.setOriginRect(mDoodleView.getDoodleBound());
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


    @Override
    public void resetDoodle() {
        isCropReset = true;
        mDoodleView.setDoodleMinScale(editScale);
        mDoodleView.setDoodleRotation(0);
        mDoodleView.setDoodleScale(editScale, 0, 0);
        mDoodleView.setDoodleTranslation(editTransX, editTransY);
        float ratio = editRect.width() * 1f / editRect.height() * 1f;
        mUCropFrame.setOriginRect(editRect);
        mUCropFrame.setTargetAspectRatio(ratio);
        mSelectedRatio = ratio;

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
            mBtnPenIds.put(DoodlePen.BRUSH, R.id.btn_pen_hand);
            mBtnPenIds.put(DoodlePen.MOSAIC, R.id.btn_pen_mosaic);
            mBtnPenIds.put(DoodlePen.TEXT, R.id.btn_pen_text);
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
                mTouchGestureListener.getSelectedItem().setSize(getSize() / getDoodleScale());
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
                                              int from, String sendTo, int requestCode) {
        Intent intent = new Intent(activity, EditPhotoActivity.class);
        intent.putExtra(KEY_PARAMS, params);
        intent.putExtra(EXTRA_FROM, from);
        intent.putExtra(EXTRA_SEND_TO, sendTo);
        activity.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onBackPressed() {
        if (mUCropFrame != null && mUCropFrame.getVisibility() == View.VISIBLE) {
            mUCropFrame.setVisibility(View.GONE);
        }
        cleanAnim();
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
                boolean isEdit = data.getBooleanExtra(AddTextActivity.EXTRA_IS_EDIT, false);
                if (isEdit) {
                    if (editText != null) {
                        editText.setAlignmentMode(textAlignmentMode);
                        mDoodle.setColor(new DoodleColor(textSelectedColor));
                        editText.setIsShowTextBg(isShowTextBg);
                        editText.setText(text);
                    }
                } else {
                    createDoodleText(null, 0, screenHeight / 2, text, isShowTextBg, textAlignmentMode);
                }
            }
        }
    }

    public void editViewAnimIn(final View view) {
        mDoodleView.setDoodleMinScale(editScale);
        if (mInTranslateAnimator == null) {
            mInTranslateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0, -view.getMeasuredHeight() - DimenUtils.dp2px(this, 9)).setDuration(200);
            mInTranslateAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    flFinish.setVisibility(View.GONE);

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    switch (CURRENT_MODE) {
                        case MODE_ERASER:
                            mDoodle.setPen(DoodlePen.ERASER);
                            mDoodle.setSize(mScrawlSize, mScrawlIndex);
                            break;
                        case MODE_SCRAWL:
                            mDoodle.setPen(DoodlePen.BRUSH);
                            mDoodle.setColor(new DoodleColor(selectedColor));
                            mDoodle.setSize(mScrawlSize, mScrawlIndex);
                            mDoodle.setShape(selectedShape);
                            break;
                        case MODE_MOSAIC_ERASER:
                            mDoodle.setPen(DoodlePen.MOSAIC_ERASER);
                            mDoodle.setSize(mMosaicSize, -1);
                        case MODE_MOSAIC:
                            mDoodle.setPen(DoodlePen.MOSAIC);
                            mDoodle.setSize(mMosaicSize, -1);
                            mDoodle.setShape(DoodleShape.HAND_WRITE);
                            break;
                        case MODE_CROP:
                            initUCropFrame();
                            float ratio = 1f;

                            mUCropFrame.setOriginRect(editRect);
                            if (mCropViewRect == null) {
                                ratio = editRect.width() / editRect.height();
                            } else {
                                ratio = mCropViewRect.width() / mCropViewRect.height();
                            }
                            mUCropFrame.setTargetAspectRatio(ratio);
                            mSelectedRatio = ratio;
                            break;
                    }
                    if (mCropViewRect == null) {
                        mDoodleView.setDoodleScaleAndTrans(editScale, editTransX, editTransY);
                    } else {
                        mDoodleView.setDoodleScaleAndTrans(cropScale, cropTransX, cropTransY);
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
        mInTranslateAnimator.start();


    }


    private void changeCurrentDoodleEdit(int height) {
        //此时获取的是图片旋转后,按照屏幕宽高缩放后的范围
        RectF doodleBound = mDoodleView.getDoodleBound();

        float w = doodleBound.width();
        float h = doodleBound.height();

        float nw = w * 1f / screenWidth * 1f;
        float nh = h * 1f / switchScreenHeight * 1f;
        float scale = 1f;
        float centerWidth = 0f;
        float centerHeight = 0f;
        float centerX = 0f;
        float centerY = 0f;
        if (nw > nh) {
            scale = 1 / nw;
            centerWidth = screenWidth;
            centerHeight = (int) (h * scale);
        } else {
            scale = 1 / nh;
            centerWidth = (int) (w * scale);
            centerHeight = switchScreenHeight;
        }

        //由于旋转重置了图片,因此重新计算图片需要缩放的大小
        cropScale = scale;
        if (scale < 1) {
            mDoodleView.setDoodleMinScale(scale);
        }

        // 使图片居中
        centerX = (screenWidth - centerWidth) / 2f;
        centerY = (switchScreenHeight - centerHeight) / 2f;

        switch (mDoodleView.getDoodleRotation()) {
            case 0:
                cropTransX = centerX - doodleBound.left;
                cropTransY = centerY - doodleBound.top;
                break;
            case 90:
                cropTransX = centerY - doodleBound.top;
                cropTransY = centerX - doodleBound.left;
                break;
            case 180:
                cropTransX = centerX - doodleBound.left;
                cropTransY = centerY - doodleBound.top + height;
                break;
            case 270:
                cropTransX = centerY - doodleBound.top + height;
                cropTransY = centerX - doodleBound.left;
                break;
        }


    }

    private void getChangeEdit(int height) {

        float w = origin.width();
        float h = origin.height();

        switchScreenHeight = screenHeight - height - DimenUtils.dp2px(this, 9);

        float nw = w * 1f / screenWidth * 1f;
        float nh = h * 1f / switchScreenHeight * 1f;
        float scale = 1f;
        if (nw > nh) {
            scale = 1 / nw;
            editWidth = screenWidth;
            editHeight = (int) (h * scale);
        } else {
            scale = 1 / nh;
            editWidth = (int) (w * scale);
            editHeight = switchScreenHeight;
        }
        editScale = scale;

        if (editScale < 1) {
            mDoodleView.setDoodleMinScale(editScale);
        }

        // 使图片居中
        editX = (screenWidth - editWidth) / 2f;
        editY = (switchScreenHeight - editHeight) / 2f;

        switch (mDoodleView.getDoodleRotation()) {
            case 0:
                editTransX = editX - origin.left;
                editTransY = editY - origin.top;
                break;
            case 90:
                editTransX = editX - origin.top;
                editTransY = editY - origin.left;
                break;
            case 180:
                editTransX = editX - origin.left;
                editTransY = editY - origin.top + height;
                break;
            case 270:
                editTransX = editX - origin.top + height;
                editTransY = editX - origin.left;
                break;
        }

        editRect.set(editX, editY, editX + editWidth, editY + editHeight);


    }

    public void editViewAnimOut(View view, final BaseEditFragment fragment) {

        if (mOutTranslateAnimator == null) {
            mOutTranslateAnimator = ObjectAnimator.ofFloat(view, "translationY", -view.getMeasuredHeight() - DimenUtils.dp2px(this, 9), 0).setDuration(200);
            mOutTranslateAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (fragmentManager != null && fragment != null) {
                        fragmentManager.beginTransaction().hide(fragment).commit();
                        mDoodleView.setEditMode(true);
                    }
                    flFinish.setVisibility(View.VISIBLE);
                    if (mCropViewRect == null || mCropViewRect.isEmpty()) {
                        mDoodleView.setDoodleMinScale(1);
                        mDoodleView.setDoodleScaleAndTrans(1, 0, 0);
                    } else {
                        mDoodleView.setDoodleMinScale(1);
                        mDoodleView.setDoodleScaleAndTrans(cropScale, cropTransX, cropTransY);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        mOutTranslateAnimator.start();

    }


    private void refreshEditBackOrNextStatus() {
        if (mDoodle != null && editFragment != null) {
            List<IDoodleItem> allItem = null;
            List<IDoodleItem> allRedoItem = null;
            if (CURRENT_MODE == MODE_SCRAWL || CURRENT_MODE == MODE_ERASER) {
                allItem = mDoodleView.getUnDoItem();
                allRedoItem = mDoodle.getAllRedoItem();
            } else if (CURRENT_MODE == MODE_MOSAIC || CURRENT_MODE == MODE_MOSAIC_ERASER) {
                allItem = mDoodle.getMosaicItem();
                if (allItem == null) {
                    allItem = mDoodle.getMosaicBeforeDrawItem();
                } else {
                    allItem.addAll(mDoodle.getMosaicBeforeDrawItem());
                }
                allRedoItem = mDoodle.getMosaicRedoItem();
            }

            boolean isShowBack = false;
            boolean isShowNext = false;
            if (allItem != null && allItem.size() > 0) {
                isShowBack = true;
            }
            if (allRedoItem != null && allRedoItem.size() > 0) {
                isShowNext = true;
            }

            editFragment.refreshPreOrNextStatus(isShowBack || isShowNext, isShowBack, isShowNext, "");

        }
    }

    private void refreshWipeShowEnable() {
        if (editFragment != null) {
            if (editFragment instanceof EditScrawlFragment) {
                if (mDoodleView.getUnDoItem().size() > 0) {
                    ((EditScrawlFragment) editFragment).refreshWipeStatue(true);
                } else {
                    ((EditScrawlFragment) editFragment).refreshWipeStatue(false);
                }
            } else if (editFragment instanceof EditMosaicFragment) {
                if (mDoodleView.getMosaicBeforeDrawItem().size() > 0) {
                    ((EditMosaicFragment) editFragment).refreshWipeStatue(true);
                } else {
                    ((EditMosaicFragment) editFragment).refreshWipeStatue(false);
                }
            }
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
                    if (isCropReset) {
                        isCropReset = false;
                        return;
                    }
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
                    if (editFragment != null && editFragment instanceof EditCropFragment) {
                        ((EditCropFragment) editFragment).onRefreshOrigin(true);
                    }
                }

                @Override
                public void onCropRectEnd(RectF rectF) {
                    LogUtil.d("DoodleView", "onCropRectEnd");

                    //未放大前裁剪框范围(屏幕坐标)
                    RectF tempRect = new RectF(rectF);

                    //设置裁剪框比例,会重新根据OverlayView 中originRect 计算并居中显示
                    mUCropFrame.setTargetAspectRatio(rectF.width() * 1f / rectF.height() * 1f);

                    //放大居中之后,裁剪框范围(屏幕坐标)
                    RectF cropViewRect = mUCropFrame.getCropViewRect();
                    //当前图片显示范围
                    RectF doodleBound = mDoodleView.getDoodleBound();

                    //计算裁剪框从最初显示位置,到放大并居中显示后的缩放比例
                    float sw = tempRect.width() * 1f / cropViewRect.width();
                    float sh = tempRect.height() * 1f / cropViewRect.height();
                    float scale = 1f;
                    if (sw > sh) {
                        scale = 1f / sw;
                    } else {
                        scale = 1f / sh;
                    }

                    //由于进入裁剪时,图片已被缩放,所以图片的缩放比例应该乘以当前已被缩放的比例
                    cropScale = scale * mDoodleView.getDoodleScale();

                    //将图片按照裁剪框的左下角为锚点缩放  计算后为点在 原图上的坐标
                    float pivotX = 0f;
                    float pivotY = 0f;



                    /**屏幕坐标右为X正方向、下为Y轴正方向

                     0: 画布及图片坐标方向与屏幕坐标相同

                     90: 画布及图片坐标 下为X轴正方向 左为Y轴正方向

                     180: 画布及图片坐标 右为X轴正方向 上为Y轴正方向

                     270: 画布及图片坐标 上为X轴正方向 右为Y轴正方向**/
                    switch (mDoodleView.getDoodleRotation()) {
                        case 0:
                            pivotX = (tempRect.left - doodleBound.left) / mDoodleView.getAllScale();
                            pivotY = (tempRect.bottom - doodleBound.top) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, pivotX, pivotY);
                            cropTransX = (cropViewRect.left - tempRect.left) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (cropViewRect.bottom - tempRect.bottom) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 90:
                            pivotX = (tempRect.bottom - doodleBound.top) / mDoodleView.getAllScale();
                            pivotY = (doodleBound.right - tempRect.left) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, pivotX, pivotY);
                            cropTransX = (cropViewRect.bottom - tempRect.bottom) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (tempRect.left - cropViewRect.left) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 180:
                            pivotX = (doodleBound.right - tempRect.left) / mDoodleView.getAllScale();
                            pivotY = (doodleBound.bottom - tempRect.bottom) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, pivotX, pivotY);
                            cropTransX = (tempRect.left - cropViewRect.left) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (tempRect.bottom - cropViewRect.bottom) + mDoodleView.getDoodleTranslationY();
                            break;
                        case 270:
                            pivotX = (doodleBound.bottom - rectF.bottom) / mDoodleView.getAllScale();
                            pivotY = (tempRect.left - doodleBound.left) / mDoodleView.getAllScale();
                            mDoodle.setDoodleScale(cropScale, pivotX, pivotY);
                            cropTransX = (tempRect.bottom - cropViewRect.bottom) + mDoodleView.getDoodleTranslationX();
                            cropTransY = (cropViewRect.left - tempRect.left) + mDoodleView.getDoodleTranslationY();
                            break;

                    }


                    mDoodle.setDoodleTranslation(cropTransX, cropTransY);
                    if (editFragment != null && editFragment instanceof EditCropFragment) {
                        ((EditCropFragment) editFragment).onRefreshOrigin(true);
                    }
                }

            });
        } else {
            mUCropFrame.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            if (mInTranslateAnimator != null) {
                mInTranslateAnimator.cancel();
            }

            if (mOutTranslateAnimator != null) {
                mOutTranslateAnimator.cancel();
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

        mDoodle.setDoodleTranslation(x - mDoodleView.getCentreTranX(), y - mDoodleView.getCentreTranY());
    }

    private void cleanAnim() {
        if (mInTranslateAnimator != null) {
            mInTranslateAnimator.cancel();
        }

        if (mOutTranslateAnimator != null) {
            mOutTranslateAnimator.cancel();
        }

    }
}