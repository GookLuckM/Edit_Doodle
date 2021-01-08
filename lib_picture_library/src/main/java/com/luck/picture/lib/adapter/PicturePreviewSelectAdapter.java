/*
 * Copyright (C) 2014 pengjianbo(pengjianbosoft@gmail.com), Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.luck.picture.lib.adapter;

import android.content.Context;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.ArrayList;
import java.util.List;


public class PicturePreviewSelectAdapter extends RecyclerView.Adapter<PicturePreviewSelectAdapter.ViewHolder> {

    private Context context;
    private List<LocalMedia> list = new ArrayList<>();
    private LayoutInflater mInflater;
    private LocalMedia currentLocalMedia;
    private ItemClickListener itemClickListener;

    public PicturePreviewSelectAdapter(Context context, List<LocalMedia> list) {
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.list = list;
    }

    public void bindData(List<LocalMedia> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setCurrentLocalMediaAndNotifyData(LocalMedia localMedia) {
        currentLocalMedia = localMedia;
        notifyDataSetChanged();
    }

    public void setItemClickListener(ItemClickListener listener) {
        itemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = mInflater.inflate(R.layout.picture_preview_select_item,
                parent, false);
        return new ViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String path = "";
        LocalMedia photoInfo = list.get(position);
        if (photoInfo != null) {
            if(photoInfo.isEdit() && !TextUtils.isEmpty(photoInfo.getEditPath())){
                path = photoInfo.getEditPath();
            }else {
                path = photoInfo.getPath();
            }
        }
        holder.itemView.setTag(photoInfo);

        if (photoInfo.equals(currentLocalMedia)) {
            holder.orange_border.setVisibility(View.VISIBLE);
        } else {
            holder.orange_border.setVisibility(View.GONE);
        }

        RequestOptions options = new RequestOptions()
                //.placeholder(com.yalantis.ucrop.R.color.ucrop_color_grey)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(path)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options)
                .into(holder.mIvPhoto);
    }


    @Override
    public int getItemCount() {
        return list.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvPhoto;
        View orange_border;
        View itemView;

        public ViewHolder(View view, final ItemClickListener listener) {
            super(view);
            itemView = view;
            mIvPhoto = view.findViewById(R.id.iv_picture);
            orange_border = view.findViewById(R.id.orange_border);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getTag() != null && listener != null) {
                        LocalMedia localMedia = (LocalMedia) view.getTag();
                        listener.onClick(localMedia);
                    }
                }
            });
        }
    }

    /**
     * Item点击事件
     */
    public interface ItemClickListener {
        void onClick(LocalMedia localMedia);
    }
}
