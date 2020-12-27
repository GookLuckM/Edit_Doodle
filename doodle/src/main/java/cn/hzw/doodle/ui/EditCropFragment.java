package cn.hzw.doodle.ui;

import android.view.View;
import android.widget.ImageButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hzw.doodle.R;

public class EditCropFragment extends BaseEditFragment  implements CropRatioAdapter.OnRatioClickListener{

    public static final String EXTRA_ORIGIN_RATIO = "extra_origin_ratio";
    private List<String> cropRatioList;
    private List<Float> cropRatioFloatList;
    private List<Integer> cropRatioIconList;
    private ImageButton btnRotate;
    private CropRatioAdapter mCropRatioAdapter;

    @Override
    protected void init() {
        setTitle("裁切旋转");
        float originRatio = getArguments().getFloat(EXTRA_ORIGIN_RATIO);
        String[] cropRatio = getResources().getStringArray(R.array.crop_ratio);
        cropRatioList = Arrays.asList(cropRatio);
        cropRatioFloatList = new ArrayList<>();
        cropRatioFloatList.add(originRatio);
        cropRatioFloatList.add(-1f);
        cropRatioFloatList.add(1f);
        cropRatioFloatList.add(0.75f);
        cropRatioFloatList.add(1.33f);
        cropRatioFloatList.add(0.56f);
        cropRatioFloatList.add(1.77f);
        cropRatioIconList = new ArrayList<>();
        cropRatioIconList.add(R.drawable.icon_origin_selector);
        cropRatioIconList.add(R.drawable.icon_custom_ratio_selector);
        cropRatioIconList.add(R.drawable.icon_1_1_selector);
        cropRatioIconList.add(R.drawable.icon_3_4_selector);
        cropRatioIconList.add(R.drawable.icon_4_3_selector);
        cropRatioIconList.add(R.drawable.icon_9_16_selector);
        cropRatioIconList.add(R.drawable.icon_16_9_selector);
    }

    @Override
    protected void initView() {
        btnRotate = rootView.findViewById(R.id.iv_rotate);
        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.onRotate();
                }
            }
        });
        RecyclerView rvRatioList = rootView.findViewById(R.id.rv_ratios);

        mCropRatioAdapter = new CropRatioAdapter(getContext(),cropRatioList,cropRatioFloatList,cropRatioIconList);
        rvRatioList.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        rvRatioList.setAdapter(mCropRatioAdapter);
        mCropRatioAdapter.setOnRatioClickListener(this);
    }



    @Override
    protected int getContentLayout() {
        return R.layout.frag_edit_crop;
    }


    public void setCropRatio(float ratio){
        if (!cropRatioFloatList.contains(ratio)){
            ratio = -1f;
        }
        mCropRatioAdapter.setSelectedRatio(ratio);
    }


    @Override
    public void onRatioClick(float ratio) {
        if (mEditListener != null){
            mEditListener.onCropRatioChange(ratio);
        }
    }
}
