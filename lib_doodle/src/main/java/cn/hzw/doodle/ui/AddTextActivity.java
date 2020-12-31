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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hzw.doodle.R;
import cn.hzw.doodle.util.DrawUtil;

public class AddTextActivity extends Activity implements TextColorsAdapter.OnColorClickListener {

    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_MID = 1;
    public static final int ALIGNMENT_RIGHT = 2;
    public static final String RESULT_TEXT = "result_text";
    public static final String RESULT_COLOR = "result_color";
    public static final String RESULT_ALIGNMENT = "result_alignment";
    public static final String RESULT_IS_DRAW_TEXT_BG = "result_is_draw_text_bg";
    public static final String RESULT_RECT = "result_rect";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_COLOR = "extra_color";
    public static final String EXTRA_ALLIGMENT = "extra_allignment";
    public static final String EXTRA_IS_SHOW_BG = "extra_is_show_bg";
    public static final String EXTRA_IS_EDIT = "extra_is_edit";
    private String[] colorNames;
    private String[] colorArr;
    private List<String> colorList;
    private List<String> colorNamesList;
    private int selectedColor;
    private String selectedColorStr;
    private boolean isShowEditBg;
    private boolean isChangedColor;
    private EditText etInput;
    private int current_alignment_mode = 0;
    private int color;
    private boolean isEdit;
    private String text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_text);
        colorArr = getResources().getStringArray(R.array.color_arr);
        selectedColor = Color.parseColor(colorArr[0]);
        TextView tvCancel = findViewById(R.id.tv_cancel);
        TextView tvDone = findViewById(R.id.tv_done);


        final ImageView ivAlignment = findViewById(R.id.iv_alignment);

        current_alignment_mode = getIntent().getIntExtra(EXTRA_ALLIGMENT, 0);
        text = getIntent().getStringExtra(EXTRA_TEXT);
        isEdit = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);
        color = getIntent().getIntExtra(EXTRA_COLOR, Color.parseColor("#FA5051"));
        isShowEditBg = getIntent().getBooleanExtra(EXTRA_IS_SHOW_BG, false);

        etInput = findViewById(R.id.et_input);

        etInput.requestFocus();

        if (!TextUtils.isEmpty(text)) {
            etInput.setText(text);
        }


        RecyclerView rvColors = findViewById(R.id.rv_colors);

        colorArr = getResources().getStringArray(R.array.color_arr);
        colorNames = getResources().getStringArray(R.array.color_names);
        if (colorArr != null && colorArr.length > 1 && colorNames != null && colorNames.length > 1 && colorArr.length == colorNames.length) {
            colorList = Arrays.asList(colorArr);
            colorNamesList = Arrays.asList(colorNames);
        }

        TextColorsAdapter textColorsAdapter = new TextColorsAdapter(this, colorList);
        textColorsAdapter.setOnColorClickListener(this);
        rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvColors.setAdapter(textColorsAdapter);

        switch (current_alignment_mode) {
            case ALIGNMENT_LEFT:
                current_alignment_mode = ALIGNMENT_LEFT;
                etInput.setGravity(Gravity.LEFT);
                ivAlignment.setImageResource(R.drawable.icon_alignment_left);
                break;
            case ALIGNMENT_MID:
                current_alignment_mode = ALIGNMENT_MID;
                etInput.setGravity(Gravity.CENTER);
                ivAlignment.setImageResource(R.drawable.icon_alignment_mid);

                break;
            case ALIGNMENT_RIGHT:
                current_alignment_mode = ALIGNMENT_RIGHT;
                etInput.setGravity(Gravity.RIGHT);
                ivAlignment.setImageResource(R.drawable.icon_alignment_right);
                break;
        }

        for (String colorStr : colorList) {
            if (color == Color.parseColor(colorStr)) {
                selectedColorStr = colorStr;
                selectedColor = color;
            }
        }

        if (!TextUtils.isEmpty(selectedColorStr)){
            textColorsAdapter.setSelectedColor(selectedColorStr);
        }


        ivAlignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (current_alignment_mode) {
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
                if (cbBackGround.isSelected()) {
                    isShowEditBg = false;
                    cbBackGround.setSelected(false);
                } else {
                    isShowEditBg = true;
                    cbBackGround.setSelected(true);
                }
                isChangedColor = false;
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
                    changeColor();
                }
            }
        });


        if (isShowEditBg) {
            cbBackGround.setSelected(true);
        } else {
            cbBackGround.setSelected(false);
        }

        isChangedColor = false;
        if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
            changeColor();
        }

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
                if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
                    changeColor();
                } else {
                    etInput.setBackground(new ColorDrawable(Color.TRANSPARENT));
                    isChangedColor = false;
                }

                if (TextUtils.isEmpty(etInput.getText().toString())) {
                    etInput.setText(" ");
                } else {
                    String content = etInput.getText().toString();
                    if (content.length() > 1 && content.substring(0, 1).equals(" ")) {
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
                String text = etInput.getText().toString().trim();
                Intent intent = new Intent();
                intent.putExtra(RESULT_TEXT, text);
                intent.putExtra(RESULT_COLOR, selectedColor);
                intent.putExtra(RESULT_ALIGNMENT, current_alignment_mode);
                intent.putExtra(RESULT_IS_DRAW_TEXT_BG, isShowEditBg);
                intent.putExtra(EXTRA_IS_EDIT, isEdit);
                Rect rect = new Rect();
                etInput.getDrawingRect(rect);
                intent.putExtra(RESULT_RECT, rect);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void changeColor() {
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
                if (DrawUtil.isLightColor(selectedColor)) {
                    etInput.setTextColor(Color.BLACK);
                } else {
                    etInput.setTextColor(Color.WHITE);
                }
            } else {

                etInput.setTextColor(selectedColor);
                etInput.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        isChangedColor = true;
    }

    @Override
    public void onColorClick(String color) {
        selectedColor = Color.parseColor(color);
        isChangedColor = false;
        if (!TextUtils.isEmpty(etInput.getText().toString().trim())) {
            changeColor();
        }
    }
}