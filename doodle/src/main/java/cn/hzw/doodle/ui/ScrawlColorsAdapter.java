package cn.hzw.doodle.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.hzw.doodle.R;
import cn.hzw.doodle.util.DimenUtils;

public class ScrawlColorsAdapter extends RecyclerView.Adapter<ScrawlColorsAdapter.ScrawlColorsViewHolder> {

    List<String> colorList;
    List<String> colorName;
    Context mContext;
    String selectedColor = "#FA5051";
    OnColorClickListener mListener;

    public ScrawlColorsAdapter(Context context,List<String> colorList, List<String> colorName) {
        mContext = context;
        this.colorList = colorList;
        this.colorName = colorName;
    }

    @NonNull
    @Override
    public ScrawlColorsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_scrawl_color,viewGroup,false);
        return new ScrawlColorsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScrawlColorsViewHolder holder, int i) {
        final String color = colorList.get(i);
        LayerDrawable layerDrawable = (LayerDrawable) mContext.getResources().getDrawable(R.drawable.scrawl_black_selected);
        GradientDrawable layerInnerDrawableOne = (GradientDrawable) layerDrawable.getDrawable(0);
        GradientDrawable layerInnerDrawableTwo = (GradientDrawable) layerDrawable.getDrawable(1);
        layerInnerDrawableOne.mutate();
        layerInnerDrawableTwo.mutate();
        layerInnerDrawableOne.setColor(Color.parseColor(color));
        layerInnerDrawableTwo.setColor(Color.parseColor(color));

        GradientDrawable gradientDrawable = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.scrawl_black_unselected);
        gradientDrawable.mutate();
        gradientDrawable.setColor(Color.parseColor(colorList.get(i)));
        RecyclerView.LayoutParams itemParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        if (i == 0){
            itemParams.leftMargin = 0;
        }else {
            itemParams.leftMargin = DimenUtils.dp2px(mContext,22);
        }
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.tvColorName.getLayoutParams();
        if (!TextUtils.isEmpty(selectedColor) && selectedColor.equals(color) ){
            holder.ivColor.setImageDrawable(layerDrawable);
            layoutParams.topMargin = DimenUtils.dp2px(mContext,6);
        }else {
            holder.ivColor.setImageDrawable(gradientDrawable);
            layoutParams.topMargin = DimenUtils.dp2px(mContext,13);
        }
        holder.tvColorName.setLayoutParams(layoutParams);

        holder.tvColorName.setText(colorName.get(i));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(selectedColor) && selectedColor.equals(color)){
                    return;
                }
                if (mListener != null){
                    mListener.onColorClick(color);
                }
                selectedColor = color;
                notifyDataSetChanged();

            }
        });

    }

    public interface OnColorClickListener{
        void onColorClick(String color);
    }

    public void setOnColorClickListener(OnColorClickListener listener){
        mListener = listener;
    }


    @Override
    public int getItemCount() {
        return colorList == null ? 0 : colorList.size();
    }

    class ScrawlColorsViewHolder extends RecyclerView.ViewHolder{

        int resId = R.layout.item_scrawl_color;
        private final ImageView ivColor;
        private final TextView tvColorName;

        public ScrawlColorsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivColor = itemView.findViewById(R.id.iv_color);
            tvColorName = itemView.findViewById(R.id.tv_name);
        }
    }

}
