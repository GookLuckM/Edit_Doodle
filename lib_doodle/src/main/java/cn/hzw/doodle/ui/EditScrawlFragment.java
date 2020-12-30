package cn.hzw.doodle.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleShape;
import cn.hzw.doodle.R;
import cn.hzw.doodle.util.DimenUtils;

public class EditScrawlFragment extends BaseEditFragment implements ScrawlColorsAdapter.OnColorClickListener {

    private String[] colorNames;
    private String[] colorArr;
    private List<String> colorList;
    private List<String> colorNamesList;
    private View scrawlShapeView;
    private List<Integer> shapeIds;
    private ImageButton btnScrawlShape;
    private ImageButton btnScrawlWipe;
    private ImageButton btnScrawlPaintSize;
    private View scrawlPenSizeView;
    private HashMap<Integer, Integer> penSizeMap;
    private String mSelectedColor = "#FA5051";
    private ScrawlColorsAdapter scrawlColorsAdapter;

    @Override
    protected void init() {
        setTitle("涂鸦");
        colorArr = getResources().getStringArray(R.array.color_arr);
        colorNames = getResources().getStringArray(R.array.color_names);
        if (colorArr != null && colorArr.length > 1 && colorNames != null && colorNames.length > 1 && colorArr.length == colorNames.length) {
            colorList = Arrays.asList(colorArr);
            colorNamesList = Arrays.asList(colorNames);
        }
    }


    @Override
    protected void initView() {
        btnScrawlWipe = rootView.findViewById(R.id.btn_wipe);
        btnScrawlShape = rootView.findViewById(R.id.btn_shape);
        btnScrawlPaintSize = rootView.findViewById(R.id.btn_paint_size);
        RecyclerView rvColors = rootView.findViewById(R.id.rv_colors);
        scrawlColorsAdapter = new ScrawlColorsAdapter(getActivity(), colorList,colorNamesList);
        scrawlColorsAdapter.setOnColorClickListener(this);
        rvColors.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        rvColors.setAdapter(scrawlColorsAdapter);


        btnScrawlWipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnScrawlWipe.isSelected()){
                    btnScrawlWipe.setSelected(false);
                }else {
                    btnScrawlWipe.setSelected(true);
                }

                if (mEditListener != null){
                    if (btnScrawlWipe.isSelected()) {
                        mEditListener.setMode(DoodlePen.ERASER);
                        if (scrawlColorsAdapter != null){
                            scrawlColorsAdapter.setSelectedColor("");
                        }
                    }else {
                        mEditListener.setMode(DoodlePen.BRUSH);
                        if (scrawlColorsAdapter != null){
                            scrawlColorsAdapter.setSelectedColor(mSelectedColor);
                        }
                    }
                }

            }
        });

        btnScrawlWipe.setClickable(false);
        btnScrawlWipe.setImageResource(R.drawable.icon_wipe_disable);

        btnScrawlShape.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                ViewStub vsScrawlShape = rootView.findViewById(R.id.vs_scrawl_shape);
                if (scrawlShapeView == null) {
                    vsScrawlShape.inflate();
                    vsScrawlShape.setVisibility(View.GONE);
                    scrawlShapeView = rootView.findViewById(R.id.scrawl_shape);
                    initScrawlShapeView();
                }
                scrawlShapeView.post(new Runnable() {
                    @Override
                    public void run() {
                        editViewAnimIn(scrawlShapeView);
                    }
                });

            }
        });

        btnScrawlPaintSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewStub vsScrawlPenSize = rootView.findViewById(R.id.vs_scrawl_pen_size);
                if (scrawlPenSizeView == null) {
                    vsScrawlPenSize.inflate();
                    vsScrawlPenSize.setVisibility(View.GONE);
                    scrawlPenSizeView = rootView.findViewById(R.id.scrawl_pen_size);
                    initPenSizeView();
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

    private void initScrawlShapeView(){
        final ImageButton btnShapeNormal = rootView.findViewById(R.id.btn_shape_scrawl);
        final ImageButton btnShapeCircle = rootView.findViewById(R.id.btn_shape_circle);
        final ImageButton btnShapeRectangle = rootView.findViewById(R.id.btn_shape_rectangle);
        final ImageButton btnShapeArrow = rootView.findViewById(R.id.btn_shape_arrow);
        ImageView ivArrowDown = rootView.findViewById(R.id.iv_shape_arrow_down);
        shapeIds = new ArrayList<>();
        shapeIds.add(R.id.btn_shape_scrawl);
        shapeIds.add(R.id.btn_shape_circle);
        shapeIds.add(R.id.btn_shape_rectangle);
        shapeIds.add(R.id.btn_shape_arrow);
        btnShapeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setShape(DoodleShape.HAND_WRITE);
                }
                singleShapeSelected(btnShapeNormal.getId());
                btnScrawlShape.setImageResource(R.drawable.icon_shape_normal_unselect);
            }
        });

        btnShapeCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setShape(DoodleShape.HOLLOW_CIRCLE);
                }
                singleShapeSelected(btnShapeCircle.getId());
                btnScrawlShape.setImageResource(R.drawable.icon_shape_circle_unselected);
            }
        });

        btnShapeRectangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setShape(DoodleShape.HOLLOW_RECT);
                }
                singleShapeSelected(btnShapeRectangle.getId());
                btnScrawlShape.setImageResource(R.drawable.icon_shape_rectangle_unselected);
            }
        });

        btnShapeArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setShape(DoodleShape.ARROW);
                }
                singleShapeSelected(btnShapeArrow.getId());
                btnScrawlShape.setImageResource(R.drawable.icon_shape_arrow_unselected);
            }
        });

        ivArrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editViewAnimOut(scrawlShapeView);
            }
        });

    }

    private void initPenSizeView(){
        final ImageButton btnSizeSmall = rootView.findViewById(R.id.btn_small);
        final ImageButton btnSizeMid = rootView.findViewById(R.id.btn_mid);
        final ImageButton btnSizeNormal = rootView.findViewById(R.id.btn_normal);
        final ImageButton btnSizeLarge = rootView.findViewById(R.id.btn_large);
        final ImageButton btnSizeLarger = rootView.findViewById(R.id.btn_larger);
        ImageView ivArrowDown = rootView.findViewById(R.id.iv_size_arrow_down);
        penSizeMap = new HashMap<>();
        penSizeMap.put(R.id.btn_small, DimenUtils.dp2px(getContext(),2));
        penSizeMap.put(R.id.btn_mid,DimenUtils.dp2px(getContext(),3));
        penSizeMap.put(R.id.btn_normal,DimenUtils.dp2px(getContext(),7));
        penSizeMap.put(R.id.btn_large,DimenUtils.dp2px(getContext(),10));
        penSizeMap.put(R.id.btn_larger,DimenUtils.dp2px(getContext(),16));
        btnSizeSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setSize(penSizeMap.get(btnSizeSmall.getId()),0);
                }
                singleSizeSelected(btnSizeSmall.getId());
                btnScrawlPaintSize.setImageResource(R.drawable.icon_stroke_smaller_unselected);
            }
        });

        btnSizeMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setSize(penSizeMap.get(btnSizeMid.getId()),1);
                }
                singleSizeSelected(btnSizeMid.getId());
                btnScrawlPaintSize.setImageResource(R.drawable.icon_stroke_small_unselected);
            }
        });

        btnSizeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setSize(penSizeMap.get(btnSizeNormal.getId()),2);
                }
                singleSizeSelected(btnSizeNormal.getId());
                btnScrawlPaintSize.setImageResource(R.drawable.icon_stroke_mid_unselected);
            }
        });

        btnSizeLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setSize(penSizeMap.get(btnSizeLarge.getId()),3);
                }
                singleSizeSelected(btnSizeLarge.getId());
                btnScrawlPaintSize.setImageResource(R.drawable.icon_stroke_large_unselected);
            }
        });

        btnSizeLarger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.setSize(penSizeMap.get(btnSizeLarger.getId()),4);
                }
                singleSizeSelected(btnSizeLarger.getId());
                btnScrawlPaintSize.setImageResource(R.drawable.icon_stroke_big_large_unselected);
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
        return R.layout.frag_edit_scrawl;
    }

    @Override
    public void onColorClick(String color) {
        btnScrawlWipe.setSelected(false);
        mSelectedColor = color;
        if (!TextUtils.isEmpty(color) && mEditListener != null){
            mEditListener.setColor(Color.parseColor(color));
        }
    }

    private void singleShapeSelected(int selectedId) {
        if (scrawlShapeView != null && shapeIds != null) {
            for (Integer id : shapeIds) {
                if (id == selectedId) {
                    rootView.findViewById(id).setSelected(true);
                } else {
                    rootView.findViewById(id).setSelected(false);
                }
            }
        }
    }

    private void singleSizeSelected(int selectedId) {
        if (scrawlPenSizeView != null && penSizeMap != null) {
            for (Integer id : penSizeMap.keySet()) {
                if (id == selectedId) {
                    rootView.findViewById(id).setSelected(true);
                } else {
                    rootView.findViewById(id).setSelected(false);
                }
            }
        }
    }

    public void refreshWipeStatue(boolean isWipeEnable){
        if (!isWipeEnable){
            btnScrawlWipe.setImageResource(R.drawable.icon_wipe_disable);
            btnScrawlWipe.setClickable(false);
            if (mEditListener != null){
                mEditListener.setMode(DoodlePen.BRUSH);
                if (scrawlColorsAdapter != null){
                    scrawlColorsAdapter.setSelectedColor(mSelectedColor);
                }
            }
        }else {
            btnScrawlWipe.setImageResource(R.drawable.icon_wipe_selector);
            btnScrawlWipe.setClickable(true);
        }
    }
}
