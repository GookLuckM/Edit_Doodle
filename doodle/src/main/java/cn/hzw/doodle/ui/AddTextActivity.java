package cn.hzw.doodle.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import cn.hzw.doodle.R;
import cn.hzw.doodle.util.DrawUtil;

public class AddTextActivity extends Activity {

    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_MID = 1;
    public static final int ALIGNMENT_RIGHT = 2;
    public static final String RESULT_TEXT = "result_text";
    public static final String RESULT_COLOR = "result_color";
    public static final String RESULT_ALIGNMENT = "result_alignment";
    public static final String RESULT_IS_DRAW_TEXT_BG = "result_is_draw_text_bg";
    public static final String RESULT_RECT = "result_rect";
    private String[] colorArr;
    private int selectedColor;
    private boolean isShowEditBg;
    private boolean isChangedColor;
    private EditText etInput;
    private int current_alignment_mode = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_text);
        colorArr = getResources().getStringArray(R.array.color_arr);
        selectedColor = Color.parseColor(colorArr[0]);
        TextView tvCancel = findViewById(R.id.tv_cancel);
        TextView tvDone = findViewById(R.id.tv_done);
        RadioGroup rgColor = findViewById(R.id.rg_color);

        final ImageView ivAlignment = findViewById(R.id.iv_alignment);



        etInput = findViewById(R.id.et_input);

        etInput.requestFocus();

        ivAlignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(current_alignment_mode){
                    case ALIGNMENT_LEFT:
                        current_alignment_mode = ALIGNMENT_MID;
                        etInput.setGravity(Gravity.CENTER);
                        ivAlignment.setImageResource(R.drawable.icon_alignment_mid);
                        break;
                    case ALIGNMENT_MID:
                        current_alignment_mode = ALIGNMENT_RIGHT;
                        etInput.setGravity(Gravity.RIGHT);
                        ivAlignment.setImageResource(R.drawable.icon_alignment_right);
                        break;
                    case ALIGNMENT_RIGHT:
                        current_alignment_mode = ALIGNMENT_LEFT;
                        etInput.setGravity(Gravity.LEFT);
                        ivAlignment.setImageResource(R.drawable.icon_alignment_left);
                        break;
                }


            }
        });

        final ImageButton cbBackGround = findViewById(R.id.cb_background);

        /*cbBackGround.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    isShowEditBg = true;
                }else {
                    isShowEditBg = false;
                }
                isChangedColor = false;
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
                    changeColor();
                }
            }
        });*/

        cbBackGround.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbBackGround.isSelected()){
                    isShowEditBg = false;
                    cbBackGround.setSelected(false);
                }else {
                    isShowEditBg = true;
                    cbBackGround.setSelected(true);
                }
                isChangedColor = false;
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
                    changeColor();
                }
            }
        });

        rgColor.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                /*switch (checkedId) {
                    case R.id.rb_scrawl_grey:
                        selectedColor = Color.parseColor(colorArr[0]);
                        break;
                    case R.id.rb_scrawl_black:
                        selectedColor = Color.parseColor(colorArr[1]);
                        break;
                    case R.id.rb_scrawl_red:
                        selectedColor = Color.parseColor(colorArr[2]);
                        break;
                    case R.id.rb_scrawl_yellow:
                        selectedColor = Color.parseColor(colorArr[3]);
                        break;
                    case R.id.rb_scrawl_green:
                        selectedColor = Color.parseColor(colorArr[4]);
                        break;
                    case R.id.rb_scrawl_blue:
                        selectedColor = Color.parseColor(colorArr[5]);
                        break;
                    case R.id.rb_scrawl_purple:
                        selectedColor = Color.parseColor(colorArr[6]);
                        break;

                }*/
                isChangedColor = false;
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())){
                    changeColor();
                }
            }
        });


        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())){
                    changeColor();
                }else {
                    etInput.setBackground(new ColorDrawable(Color.TRANSPARENT));
                    isChangedColor = false;
                }

                if (TextUtils.isEmpty(etInput.getText().toString())){
                    etInput.setText(" ");
                }else {
                    String content = etInput.getText().toString();
                    if (content.length() > 1 && content.substring(0,1).equals(" ")){
                            etInput.setText(content.trim());
                    }
                }
                Selection.setSelection(etInput.getText(), etInput.getText().toString().length());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("height" + etInput.getMeasuredHeight());
                System.out.println("width" + etInput.getMeasuredWidth());
                String text = etInput.getText().toString().trim();
                Intent intent = new Intent();
                intent.putExtra(RESULT_TEXT,text);
                intent.putExtra(RESULT_COLOR,selectedColor);
                intent.putExtra(RESULT_ALIGNMENT,current_alignment_mode);
                intent.putExtra(RESULT_IS_DRAW_TEXT_BG,isShowEditBg);
                Rect rect = new Rect();
                etInput.getDrawingRect(rect);
                intent.putExtra(RESULT_RECT,rect);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }

    private void changeColor(){
        if (!isChangedColor) {
            if (isShowEditBg) {
                Drawable drawable = etInput.getBackground();
                if (drawable instanceof GradientDrawable) {
                    drawable.mutate();
                    ((GradientDrawable) drawable).setColor(selectedColor);
                    etInput.setBackground(drawable);
                } else if (drawable instanceof ColorDrawable) {
                    GradientDrawable drawable1 = (GradientDrawable) getResources().getDrawable(R.drawable.bg_edit);
                    drawable1.mutate();
                    drawable1.setColor(selectedColor);
                    etInput.setBackground(drawable1);
                }
                if (DrawUtil.isLightColor(selectedColor)){
                    etInput.setTextColor(Color.BLACK);
                }else {
                    etInput.setTextColor(Color.WHITE);
                }
            } else {

                etInput.setTextColor(selectedColor);
                etInput.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        isChangedColor = true;
    }
}