package cn.hzw.doodle.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashMap;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.R;
import cn.hzw.doodle.util.DimenUtils;

public class EditMosaicFragment extends BaseEditFragment implements ScrawlColorsAdapter.OnColorClickListener {

    private static final String MODE_THICK = "mode_thick";
    private static final String MODE_THIN = "mode_thin";

    private ImageButton btnMosaicThick;
    private ImageButton btnMosaicWipe;
    private ImageButton btnMosaicSize;
    private View scrawlPenSizeView;
    private HashMap<Integer, Integer> penThickSizeMap = new HashMap<>();
    private HashMap<Integer, Integer> penThinSizeMap = new HashMap<>();
    private View btnMosaicThin;
    private int currentLevel;
    private String MOSAIC_MODE = MODE_THICK;
    private int currentSize;
    private HashMap<Integer, Integer> penSize;

    @Override
    protected void init() {
        setTitle(getString(R.string.doodle_mosaic));
        currentSize = R.id.btn_normal;
        currentLevel = DimenUtils.dp2px(getContext(),8);

        penThickSizeMap.put(R.id.btn_small, DimenUtils.dp2px(getContext(),8));
        penThickSizeMap.put(R.id.btn_mid, DimenUtils.dp2px(getContext(),16));
        penThickSizeMap.put(R.id.btn_normal, DimenUtils.dp2px(getContext(),24));
        penThickSizeMap.put(R.id.btn_large, DimenUtils.dp2px(getContext(),40));
        penThickSizeMap.put(R.id.btn_larger, DimenUtils.dp2px(getContext(),48));

        penThinSizeMap.put(R.id.btn_small, DimenUtils.dp2px(getContext(),5));
        penThinSizeMap.put(R.id.btn_mid, DimenUtils.dp2px(getContext(),10));
        penThinSizeMap.put(R.id.btn_normal, DimenUtils.dp2px(getContext(),15));
        penThinSizeMap.put(R.id.btn_large, DimenUtils.dp2px(getContext(),25));
        penThinSizeMap.put(R.id.btn_larger, DimenUtils.dp2px(getContext(),30));
        penSize = new HashMap<>();
        penSize.putAll(penThickSizeMap);
    }

    @Override
    protected void initView() {
        btnMosaicWipe = rootView.findViewById(R.id.btn_mosaic_wipe);
        btnMosaicThick = rootView.findViewById(R.id.btn_mosaic_thick);
        btnMosaicThin = rootView.findViewById(R.id.btn_mosaic_thin);
        btnMosaicSize = rootView.findViewById(R.id.btn_mosaic_size);

        btnMosaicThick.setSelected(true);

        btnMosaicWipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnMosaicWipe.isSelected()) {
                    btnMosaicWipe.setSelected(false);
                } else {
                    btnMosaicWipe.setSelected(true);
                }

                if (mEditListener != null) {
                    if (btnMosaicWipe.isSelected()) {
                        mEditListener.setMode(DoodlePen.MOSAIC_ERASER);
                    } else {
                        mEditListener.setMode(DoodlePen.MOSAIC);
                    }
                }

            }
        });

        btnMosaicWipe.setClickable(false);
        btnMosaicWipe.setImageResource(R.drawable.icon_wipe_disable);


        btnMosaicThick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    penSize.clear();
                    penSize.putAll(penThickSizeMap);
                    btnMosaicThick.setSelected(true);
                    btnMosaicThin.setSelected(false);
                    btnMosaicWipe.setSelected(false);
                    currentLevel = DimenUtils.dp2px(getContext(),8);
                    mEditListener.setMosaicLevel(currentLevel);
                    mEditListener.setMosaicSize(penSize.get(currentSize),-1);
                }
            }
        });

        btnMosaicThin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    penSize.clear();
                    penSize.putAll(penThinSizeMap);
                    btnMosaicThin.setSelected(true);
                    btnMosaicThick.setSelected(false);
                    btnMosaicWipe.setSelected(false);
                    currentLevel = DimenUtils.dp2px(getContext(),3);
                    mEditListener.setMosaicLevel(currentLevel);
                    mEditListener.setMosaicSize(penSize.get(currentSize),-1);
                }
            }
        });


        btnMosaicSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewStub vsScrawlPenSize = rootView.findViewById(R.id.vs_scrawl_pen_size);
                if (scrawlPenSizeView == null) {
                    vsScrawlPenSize.inflate();
                    vsScrawlPenSize.setVisibility(View.GONE);
                    scrawlPenSizeView = rootView.findViewById(R.id.scrawl_pen_size);
                    initMosaicSizeView();
                }
                scrawlPenSizeView.post(new Runnable() {
                    @Override
                    public void run() {
                        editViewAnimIn(scrawlPenSizeView);
                    }
                });
            }
        });


    }


    private void initMosaicSizeView() {
        final ImageButton btnSizeSmall = rootView.findViewById(R.id.btn_small);
        final ImageButton btnSizeMid = rootView.findViewById(R.id.btn_mid);
        final ImageButton btnSizeNormal = rootView.findViewById(R.id.btn_normal);
        final ImageButton btnSizeLarge = rootView.findViewById(R.id.btn_large);
        final ImageButton btnSizeLarger = rootView.findViewById(R.id.btn_larger);
        ImageView ivArrowDown = rootView.findViewById(R.id.iv_size_arrow_down);
        btnSizeNormal.setSelected(true);

        btnSizeSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.setMosaicSize(penSize.get(btnSizeSmall.getId()),-1);
                }
                currentSize = btnSizeSmall.getId();
                singleSizeSelected(currentSize);
                btnMosaicSize.setImageResource(R.drawable.icon_stroke_small_unselected);
            }
        });

        btnSizeMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.setMosaicSize(penSize.get(btnSizeMid.getId()),-1);
                }
                currentSize = btnSizeMid.getId();
                singleSizeSelected(currentSize);
                btnMosaicSize.setImageResource(R.drawable.icon_stroke_small_unselected);
            }
        });

        btnSizeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.setMosaicSize(penSize.get(btnSizeNormal.getId()),-1);
                }
                currentSize = btnSizeNormal.getId();
                singleSizeSelected(currentSize);
                btnMosaicSize.setImageResource(R.drawable.icon_stroke_mid_unselected);
            }
        });

        btnSizeLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.setMosaicSize(penSize.get(btnSizeLarge.getId()),-1);
                }
                currentSize = btnSizeLarge.getId();
                singleSizeSelected(currentSize);
                btnMosaicSize.setImageResource(R.drawable.icon_stroke_large_unselected);
            }
        });

        btnSizeLarger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.setMosaicSize(penSize.get(btnSizeLarger.getId()),-1);
                }
                currentSize = btnSizeLarger.getId();
                singleSizeSelected(currentSize);
                btnMosaicSize.setImageResource(R.drawable.icon_stroke_big_large_unselected);
            }
        });

        ivArrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editViewAnimOut(scrawlPenSizeView);
            }
        });

    }

    @Override
    protected int getContentLayout() {
        return R.layout.frag_edit_mosaic;
    }

    @Override
    public void onColorClick(String color) {
        if (!TextUtils.isEmpty(color) && mEditListener != null) {
            mEditListener.setColor(Color.parseColor(color));
        }
    }

    private void singleSizeSelected(int selectedId) {
        if (scrawlPenSizeView != null && penSize != null) {
            for (Integer id : penSize.keySet()) {
                if (id == selectedId) {
                    rootView.findViewById(id).setSelected(true);
                } else {
                    rootView.findViewById(id).setSelected(false);
                }
            }
        }
    }

    public void refreshWipeStatue(boolean isWipeEnable){
        if (btnMosaicWipe != null) {
            if (!isWipeEnable) {
                btnMosaicWipe.setImageResource(R.drawable.icon_wipe_disable);
                btnMosaicWipe.setClickable(false);
                if (mEditListener != null) {
                    mEditListener.setMode(DoodlePen.MOSAIC);
                }
            } else {
                btnMosaicWipe.setImageResource(R.drawable.icon_wipe_selector);
                btnMosaicWipe.setClickable(true);
            }
        }
    }
}
