package cn.hzw.doodle.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.hzw.doodle.R;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodledemo.IEditListener;

public abstract class BaseEditFragment extends Fragment {

    private FrameLayout contentLayout;
    protected View rootView;
    private LinearLayout llPreOrNext;
    private ImageView ivPre;
    private ImageView ivNext;
    private TextView tvCurrentControl;
    private IDoodle doodle;
    protected IEditListener mEditListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frag_base_edit,container,false);
        contentLayout = rootView.findViewById(R.id.content_view);
        View inflate = LayoutInflater.from(getContext()).inflate(getContentLayout(), null, false);
        contentLayout.addView(inflate);
        initBaseEditView();
        init();
        initView();
        return rootView;
    }

    protected abstract void init();

    protected  void setTitle(String title){
        if (!TextUtils.isEmpty(title)){
            tvCurrentControl.setText(title);
        }
    };

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
            }
        });

        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null) {
                    mEditListener.onDone();
                }
            }
        });

        ivPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.onPre();
                }
            }
        });

        ivNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditListener != null){
                    mEditListener.onNext();
                }
            }
        });
    }

    abstract void initView();


    protected abstract int getContentLayout();

    public void setEditListener(IEditListener editListener){
        mEditListener = editListener;
    }


    public void refreshPreOrNextStatus(boolean isShow,boolean isHavePre,boolean isHaveNext,String control){
        if (llPreOrNext != null && tvCurrentControl != null ) {
            if (isShow) {
                llPreOrNext.setVisibility(View.VISIBLE);
                tvCurrentControl.setVisibility(View.GONE);
                if (isHavePre) {
                    ivPre.setImageResource(R.drawable.icon_pre_black);
                } else {
                    ivPre.setImageResource(R.drawable.icon_pre_disable_black);
                }
                if (isHaveNext) {
                    ivNext.setImageResource(R.drawable.icon_next_black);
                } else {
                    ivNext.setImageResource(R.drawable.icon_next_disable_black);
                }
            } else {
                llPreOrNext.setVisibility(View.GONE);
                tvCurrentControl.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(control)) {
                    tvCurrentControl.setText(control);
                }
            }
        }
    }

    public void editViewAnimIn(final View view) {
        view.setVisibility(View.VISIBLE);
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", view.getMeasuredHeight(),0).setDuration(500);
        translateAnimator.start();
    }

    public void editViewAnimOut(final View view) {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0,view.getMeasuredHeight()).setDuration(500);
        translateAnimator.start();
    }


}
