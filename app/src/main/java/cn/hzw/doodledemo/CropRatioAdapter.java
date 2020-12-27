package cn.hzw.doodledemo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CropRatioAdapter extends RecyclerView.Adapter<CropRatioAdapter.ScrawlColorsViewHolder> {

    List<String> ratioNameList;
    List<Float> ratioList;
    List<Integer> radioDrawableList;
    Context mContext;
    float selectedRatio = -1f;
    OnRatioClickListener mListener;

    public CropRatioAdapter(Context context, List<String> ratioNameList, List<Float> ratioList,List<Integer> radioDrawableList) {
        mContext = context;
        this.ratioNameList = ratioNameList;
        this.ratioList = ratioList;
        this.radioDrawableList = radioDrawableList;
    }

    @NonNull
    @Override
    public ScrawlColorsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_crop_ratio,viewGroup,false);
        return new ScrawlColorsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScrawlColorsViewHolder holder, int i) {
        final float ratio = ratioList.get(i);
        RecyclerView.LayoutParams itemParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        if (i == 0){
            itemParams.leftMargin = 0;
        }else {
            itemParams.leftMargin = mContext.getResources().getDimensionPixelOffset(R.dimen.dp_22);
        }

        holder.ivRatio.setImageResource(radioDrawableList.get(i));
        holder.tvRatioName.setText(ratioNameList.get(i));

        if (selectedRatio == ratio){
            holder.ivRatio.setSelected(true);
        }else {
            holder.ivRatio.setSelected(false);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedRatio == ratio){
                    return;
                }
                if (mListener != null){
                    mListener.onRatioClick(ratio);
                }
                selectedRatio = ratio;
                notifyDataSetChanged();

            }
        });

    }

    public void  setSelectedRatio(float ratio){
        selectedRatio = ratio;
        notifyDataSetChanged();
    }

    public interface OnRatioClickListener{
        void onRatioClick(float ratio);
    }

    public void setOnRatioClickListener(OnRatioClickListener listener){
        mListener = listener;
    }


    @Override
    public int getItemCount() {
        return ratioNameList == null ? 0 : ratioNameList.size();
    }

    class ScrawlColorsViewHolder extends RecyclerView.ViewHolder{

        int resId = R.layout.item_crop_ratio;
        private final ImageButton ivRatio;
        private final TextView tvRatioName;

        public ScrawlColorsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRatio = itemView.findViewById(R.id.iv_ratio);
            tvRatioName = itemView.findViewById(R.id.tv_name);
        }
    }

}
