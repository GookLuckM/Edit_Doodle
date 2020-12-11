package cn.hzw.doodledemo;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.StatusBarUtil;
import cn.forward.androids.utils.Util;
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

public class EditPhotoActivity extends AppCompatActivity implements View.OnClickListener , ScrawlColorsAdapter.OnColorClickListener {

    private static final int EDIT_TEXT_REQUEST_CODE = 9999;

    public static final String KEY_PARAMS = "key_doodle_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";
    public static final String NONE = "none";
    public static final String MODE_SCRAWL = "mode_scrawl";
    public static final String MODE_ERASER = "mode_eraser";
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


    private int mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3;

    private DoodleOnTouchGestureListener mTouchGestureListener;
    private ScrawlEditFragment scrawlEditFragment;
    private ViewStub edit_frag;
    private FragmentManager fragmentManager;
    private LinearLayout llEdit;
    private View scrawlEditView;
    private ImageButton btnScrawlWipe;
    private ImageButton btnScrawlShape;
    private ImageButton btnScrawlPaintSize;
    private String[] colorNames;
    private List<String> colorList;
    private List<String> colorNamesList;
    private ScrawlColorsAdapter scrawlColorsAdapter;
    private ViewStub shapeStubView;
    private View shapeEditView;
    private List<Integer> shapeIds;

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
        colorNames = getResources().getStringArray(R.array.color_names);
        if (colorArr != null && colorArr.length > 1 && colorNames != null && colorNames.length > 1 && colorArr.length == colorNames.length) {
            colorList = Arrays.asList(colorArr);
            colorNamesList = Arrays.asList(colorNames);
        }


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

                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:

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

        llEdit = findViewById(R.id.ll_edit);
        ImageView ivScrawl = findViewById(R.id.iv_scrawl);
        ImageView ivText = findViewById(R.id.iv_text);
        ImageView ivMosaic = findViewById(R.id.iv_mosaic);
        ImageView ivCrop = findViewById(R.id.iv_crop);
        edit_frag = findViewById(R.id.edit_frag);
        shapeStubView = findViewById(R.id.scrawl_shape_edit);
        ivScrawl.setOnClickListener(this);



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
        if (v.getId() == R.id.tv_done) {
            mDoodle.save();
        } else if (v.getId() == R.id.iv_scrawl) {
            CURRENT_MODE = MODE_SCRAWL;
            mDoodle.setPen(DoodlePen.BRUSH);
            mDoodle.setShape(DoodleShape.HAND_WRITE);
            llEdit.setVisibility(View.GONE);
            if (scrawlEditView == null) {
                scrawlEditView = edit_frag.inflate();
                initScrawlEditView();
            }
            scrawlEditView.setVisibility(View.VISIBLE);
            scrawlEditView.post(new Runnable() {
                @Override
                public void run() {
                    resetBitmap(true,scrawlEditView.getMeasuredHeight());
                    scrawlEditView.setTranslationY(-scrawlEditView.getMeasuredHeight());
                    editViewAnimIn(scrawlEditView);
                }
            });



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

            } else if (pen == DoodlePen.MOSAIC) {

            } else if (pen == DoodlePen.BRUSH) {

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
                mDoodle.setSize( getResources().getDimensionPixelSize(R.dimen.dp_7));
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


    public void resetBitmap(boolean isScale, int height) {
        mDoodleView.setIsRest(false);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mDoodleView.getLayoutParams();
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (isScale) {
            int switchScreenHeight = screenHeight - height - getResources().getDimensionPixelOffset(R.dimen.dp_9);
            layoutParams.height = switchScreenHeight;
        } else {
            layoutParams.height = screenHeight;
        }
        mDoodleView.setLayoutParams(layoutParams);
    }


    public void editViewAnimIn(View view){
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0,-view.getMeasuredHeight()).setDuration(1000);
        translateAnimator.start();
    }

    public void editViewAnimOut(View view){
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY",-view.getMeasuredHeight(), 0).setDuration(1000);
        translateAnimator.start();
    }

    private void initScrawlEditView(){
        if (scrawlEditView != null){
            btnScrawlWipe = findViewById(R.id.btn_wipe);
            btnScrawlShape = findViewById(R.id.btn_shape);
            btnScrawlPaintSize = findViewById(R.id.btn_paint_size);
            ImageView ivScrawlClose = findViewById(R.id.iv_close);
            ImageView ivScrawlDone = findViewById(R.id.iv_done);
            RecyclerView rvColors = findViewById(R.id.rv_colors);
            ImageView ivPre = findViewById(R.id.iv_pre);
            ImageView ivNext = findViewById(R.id.iv_next);
            scrawlColorsAdapter = new ScrawlColorsAdapter(EditPhotoActivity.this, colorList,colorNamesList);
            scrawlColorsAdapter.setOnColorClickListener(this);
            rvColors.setLayoutManager(new LinearLayoutManager(EditPhotoActivity.this,LinearLayoutManager.HORIZONTAL,false));
            rvColors.setAdapter(scrawlColorsAdapter);
            ivPre.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDoodle.undo();
                }
            });

            ivNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDoodle.redo();
                }
            });
            btnScrawlWipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CURRENT_MODE = MODE_ERASER;
                    mDoodle.setPen(DoodlePen.ERASER);
                    mDoodle.setShape(DoodleShape.HAND_WRITE);
                }
            });

            btnScrawlShape.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shapeEditView == null) {
                        shapeEditView = shapeStubView.inflate();
                        initScrawlShapeView();
                    }

                    shapeEditView.post(new Runnable() {
                        @Override
                        public void run() {
                            editViewAnimIn(shapeEditView);
                        }
                    });

                }
            });

            btnScrawlPaintSize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            ivScrawlClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDoodle.cleanCurrentMode();
                    editViewAnimOut(scrawlEditView);
                    resetBitmap(false,0);
                    llEdit.setVisibility(View.VISIBLE);
                }
            });

            ivScrawlDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }


    private void initScrawlShapeView(){
        ImageButton btnShapeNormal = shapeEditView.findViewById(R.id.btn_shape_scrawl);
        ImageButton btnShapeCircle = shapeEditView.findViewById(R.id.btn_shape_circle);
        ImageButton btnShapeRectangle = shapeEditView.findViewById(R.id.btn_shape_rectangle);
        ImageButton btnShapeArrow = shapeEditView.findViewById(R.id.btn_shape_arrow);
        ImageView ivArrowDown = shapeEditView.findViewById(R.id.iv_shape_arrow_down);
        shapeIds = new ArrayList<>();
        shapeIds.add(R.id.btn_shape_scrawl);
        shapeIds.add(R.id.btn_shape_circle);
        shapeIds.add(R.id.btn_shape_rectangle);
        shapeIds.add(R.id.btn_shape_arrow);
        btnShapeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodle.setShape(DoodleShape.HAND_WRITE);
                signleShapeSelected(R.id.btn_shape_scrawl);
            }
        });

        btnShapeCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodle.setShape(DoodleShape.HOLLOW_CIRCLE);
                signleShapeSelected(R.id.btn_shape_circle);
            }
        });

        btnShapeRectangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodle.setShape(DoodleShape.HOLLOW_RECT);
                signleShapeSelected(R.id.btn_shape_rectangle);
            }
        });

        btnShapeArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodle.setShape(DoodleShape.ARROW);
                signleShapeSelected(R.id.btn_shape_arrow);
            }
        });

        ivArrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editViewAnimOut(shapeEditView);
            }
        });

    }


    private void signleShapeSelected(int selectedId){
        if (shapeEditView != null && shapeIds != null) {
            for (Integer id : shapeIds) {
                if (id == selectedId) {
                    shapeEditView.findViewById(id).setSelected(true);
                } else {
                    shapeEditView.findViewById(id).setSelected(false);
                }
            }
        }
    }
}