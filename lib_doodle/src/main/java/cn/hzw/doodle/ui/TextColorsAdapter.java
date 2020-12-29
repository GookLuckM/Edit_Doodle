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

public class TextColorsAdapter extends RecyclerView.Adapter<TextColorsAdapter.ScrawlColorsViewHolder> {

    List<String> colorList;
    Context mContext;
    String selectedColor = "#FA5051";
    OnColorClickListener mListener;

    public TextColorsAdapter(Context context, List<String> colorList) {
        mContext = context;
        this.colorList = colorList;
    }

    @NonNull
    @Override
    public ScrawlColorsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_text_color,viewGroup,false);
        return new ScrawlColorsViewHolder(itemView);
    }


    public void setSelectedColor(String color){
        this.selectedColor = color;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ScrawlColorsViewHolder holder, int i) {
        final String color = colorList.get(i);
        LayerDrawable layerDrawable = (LayerDrawable) mContext.getResources().getDrawable(R.drawable.text_black_selected);
        GradientDrawable layerInnerDrawableOne = (GradientDrawable) layerDrawable.getDrawable(0);
        GradientDrawable layerInnerDrawableTwo = (GradientDrawable) layerDrawable.getDrawable(1);
        layerInnerDrawableOne.mutate();
        layerInnerDrawableTwo.mutate();
        layerInnerDrawableOne.setColor(Color.parseColor(color));
        layerInnerDrawableTwo.setColor(Color.parseColor(color));

        LayerDrawable layerDrawableUnselected = (LayerDrawable) mContext.getResources().getDrawable(R.drawable.text_black_unselected);
        GradientDrawable layerInnerDrawableUnselectedOne = (GradientDrawable) layerDrawableUnselected.getDrawable(0);
        GradientDrawable layerInnerDrawableUnselectedTwo = (GradientDrawable) layerDrawableUnselected.getDrawable(1);
        layerInnerDrawableUnselectedOne.mutate();
        layerInnerDrawableUnselectedTwo.mutate();
        layerInnerDrawableUnselectedOne.setColor(Color.parseColor(color));
        layerInnerDrawableUnselectedTwo.setColor(Color.parseColor(color));

        if (!TextUtils.isEmpty(selectedColor) && selectedColor.equals(color) ){
            holder.ivColor.setImageDrawable(layerDrawable);
        }else {
            holder.ivColor.setImageDrawable(layerDrawableUnselected);
        }

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

        private final ImageView ivColor;

        public ScrawlColorsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivColor = itemView.findViewById(R.id.iv_color);

        }
    }

}
