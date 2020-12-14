package cn.hzw.doodledemo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.hzw.doodle.core.IDoodle;

public abstract class BaseEditFragment extends Fragment {

    private FrameLayout contentLayout;
    protected View rootView;
    private LinearLayout llPreOrNext;
    private ImageView ivPre;
    private ImageView ivNext;
    private TextView tvCurrentControl;
    private IDoodle doodle;
    protected IEditListener mEditListener;

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter) {
            return AnimationUtils.loadAnimation(getActivity(), R.anim.edit_fragment_in);
        } else {
            return AnimationUtils.loadAnimation(getActivity(), R.anim.edit_fragment_out);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = LayoutInflater.from(getContext()).inflate(R.layout.frag_base_edit,container,false);
        contentLayout = rootView.findViewById(R.id.content_view);
        LayoutInflater.from(getContext()).inflate(getContentLayout(), contentLayout, false);
        initBaseEditView();
        init();
        initView();
        return rootView;
    }

    protected abstract void init();

    private void initBaseEditView(){
        llPreOrNext = rootView.findViewById(R.id.ll_bottom_pre_or_next);
        ivPre = rootView.findViewById(R.id.iv_pre);
        ivNext = rootView.findViewById(R.id.iv_next);
        tvCurrentControl = rootView.findViewById(R.id.tv_current_control);
        ImageView ivClose = rootView.findViewById(R.id.iv_close);
        ImageView ivDone = rootView.findViewById(R.id.iv_done);

        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.onClose();
                }
                /*mDoodle.cleanCurrentMode();
                editViewAnimOut(scrawlEditView);
                //resetBitmap(false,scrawlEditView.getMeasuredHeight());
                llEdit.setVisibility(View.VISIBLE);*/
            }
        });

        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    abstract void initView();


    protected abstract int getContentLayout();

    public void setEditListener(IEditListener editListener){
        mEditListener = editListener;
    }


    public void refreshPreOrNextStatus(boolean isShow,boolean isHavePre,boolean isHaveNext,String control){
        if (isShow){
            llPreOrNext.setVisibility(View.VISIBLE);
            tvCurrentControl.setVisibility(View.GONE);
            if (isHaveNext){
                ivPre.setImageResource(R.drawable.icon_pre_black);
            }else {
                ivPre.setImageResource(R.drawable.icon_pre_disable_black);
            }
            if (isHaveNext){
                ivNext.setImageResource(R.drawable.icon_next_black);
            }else {
                ivNext.setImageResource(R.drawable.icon_next_disable_black);
            }
        }else {
            llPreOrNext.setVisibility(View.GONE);
            tvCurrentControl.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(control)){
                tvCurrentControl.setText(control);
            }
        }
    }

    public void editViewAnimIn(final View view) {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0, -view.getMeasuredHeight()).setDuration(1000);
        translateAnimator.start();
    }

    public void editViewAnimOut(final View view) {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", -view.getMeasuredHeight(),0).setDuration(1000);
        translateAnimator.start();
    }


}
