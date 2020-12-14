package cn.hzw.doodledemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.List;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleShape;
import cn.hzw.doodle.core.IDoodle;

public class EditScrawlFragment extends BaseEditFragment implements ScrawlColorsAdapter.OnColorClickListener {

    private String[] colorNames;
    private String[] colorArr;
    private List<String> colorList;
    private List<String> colorNamesList;

    @Override
    protected void init() {
        colorArr = getResources().getStringArray(R.array.color_arr);
        colorNames = getResources().getStringArray(R.array.color_names);
        if (colorArr != null && colorArr.length > 1 && colorNames != null && colorNames.length > 1 && colorArr.length == colorNames.length) {
            colorList = Arrays.asList(colorArr);
            colorNamesList = Arrays.asList(colorNames);
        }
    }

    @Override
    protected void initView() {
        ImageButton btnScrawlWipe = rootView.findViewById(R.id.btn_wipe);
        ImageButton btnScrawlShape = rootView.findViewById(R.id.btn_shape);
        ImageButton btnScrawlPaintSize = rootView.findViewById(R.id.btn_paint_size);
        RecyclerView rvColors = rootView.findViewById(R.id.rv_colors);
        ScrawlColorsAdapter scrawlColorsAdapter = new ScrawlColorsAdapter(getActivity(), colorList,colorNamesList);
        scrawlColorsAdapter.setOnColorClickListener(this);
        rvColors.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        rvColors.setAdapter(scrawlColorsAdapter);



        btnScrawlWipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*CURRENT_MODE = MODE_ERASER;
                mDoodle.setPen(DoodlePen.ERASER);
                mDoodle.setShape(DoodleShape.HAND_WRITE);*/
            }
        });

        btnScrawlShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                /*shapeEditView.post(new Runnable() {
                    @Override
                    public void run() {
                        editViewAnimIn(shapeEditView);
                    }
                });*/

            }
        });

        btnScrawlPaintSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


    }

    @Override
    protected int getContentLayout() {
        return R.layout.frag_edit_scrawl;
    }

    @Override
    public void onColorClick(String color) {
        if (!TextUtils.isEmpty(color) && mEditListener != null){
            mEditListener.setColor(Color.parseColor(color));
        }
    }
}
