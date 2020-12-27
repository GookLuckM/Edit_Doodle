package cn.hzw.doodledemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import cn.hzw.doodle.DoodleActivity;
import cn.hzw.doodle.DoodleParams;
import cn.hzw.doodle.DoodleView;
import cn.hzw.doodle.util.LogUtil;
import cn.hzw.doodledemo.guide.DoodleGuideActivity;

import static com.luck.picture.lib.PictureSelector.create;


public class MainActivity extends Activity {

    public static final int REQ_CODE_SELECT_IMAGE = 100;
    public static final int REQ_CODE_DOODLE = 101;
    private TextView mPath;

    String[] PermissionString={Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

            //第 1 步: 检查是否有相应的权限
            boolean isAllGranted = checkPermissionAllGranted(PermissionString);
            if (isAllGranted) {
                //Log.e("err","所有权限已经授权！");
                return;
            }
            // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
            ActivityCompat.requestPermissions(this,
                    PermissionString, 1);

        findViewById(R.id.btn_select_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 进入相册 不需要的api可以不写
                create(MainActivity.this)
                        .openGallery(PictureMimeType.ofImage())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        .maxSelectNum(1)// 最大图片选择数量
                        .minSelectNum(1)// 最小选择数量
                        .imageSpanCount(4)// 每行显示个数
                        .selectionMode(PictureConfig.SINGLE)// 多选 or 单选 PictureConfig.MULTIPLE : PictureConfig.SINGLE
                        .previewImage(true)// 是否可预览图片
                        .previewVideo(true)// 是否可预览视频
                        .enablePreviewAudio(false) // 是否可播放音频
                        .isCamera(false)// 是否显示拍照按钮
                        .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                        //.imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
                        //.setOutputCameraPath("/CustomPath")// 自定义拍照保存路径
                        .enableCrop(false)// 是否裁剪
                        .compress(true)// 是否压缩. 选择logo不压缩
                        .synOrAsy(true)//同步true或异步false 压缩 默认同步
                        //.compressSavePath(getPath())//压缩图片保存地址
                        //.sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                        .glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                        .isGif(false)// 是否显示gif图片
                        .freeStyleCropEnabled(false)// 裁剪框是否可拖拽
                        .circleDimmedLayer(false)// 是否圆形裁剪
                        .showCropFrame(true)// 是否显示裁剪矩形边框 圆形裁剪时建议设为false
                        .showCropGrid(true)// 是否显示裁剪矩形网格 圆形裁剪时建议设为false
                        .openClickSound(false)// 是否开启点击声音
                        //.selectionMedia(selectList)// 是否传入已选图片
                        //.isDragFrame(false)// 是否可拖动裁剪框(固定)
                        //.previewEggs(false)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                        //.cropCompressQuality(90)// 裁剪压缩质量 默认100
                        .minimumCompressSize(100)// 小于100kb的图片不压缩
                        //.cropWH()// 裁剪宽高比，设置如果大于图片本身宽高则无效
                        .rotateEnabled(false) // 裁剪是否可旋转图片
                        .scaleEnabled(true)// 裁剪是否可放大缩小图片
                        .forResult(REQ_CODE_SELECT_IMAGE); //结果回调onActivityResult code; PictureConfig.CHOOSE_REQUEST
            }
        });

        findViewById(R.id.btn_guide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), DoodleGuideActivity.class));
            }
        });

        findViewById(R.id.btn_mosaic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MosaicDemo.class));
            }
        });
        findViewById(R.id.btn_scale_gesture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ScaleGestureItemDemo.class));
            }
        });
        mPath = (TextView) findViewById(R.id.img_path);
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                //Log.e("err","权限"+permission+"没有授权");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (data == null) {
                return;
            }
            List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);

            if (selectList != null && selectList.size() > 0) {
                LocalMedia media = selectList.get(0);
                String url = media.isCompressed() ? media.getCompressPath() : media.getPath();
                if (!TextUtils.isEmpty(url)) {
                    LogUtil.d("Doodle", url);
                    // 涂鸦参数
                    DoodleParams params = new DoodleParams();
                    params.mIsFullScreen = true;
                    // 图片路径
                    params.mImagePath = url;
                    // 初始画笔大小
                    params.mPaintUnitSize = DoodleView.DEFAULT_SIZE;
                    // 画笔颜色
                    params.mPaintColor = Color.RED;
                    // 是否支持缩放item
                    params.mSupportScaleItem = true;
                    // 启动涂鸦页面
                    //startUcrop(list.get(0));
                    cn.hzw.doodle.ui.EditPhotoActivity.startActivityForResult(MainActivity.this, params, REQ_CODE_DOODLE);
                    //DoodleActivity.startActivityForResult(MainActivity.this, params, REQ_CODE_DOODLE);
                }

            }
        } else if (requestCode == REQ_CODE_DOODLE) {
            if (data == null) {
                return;
            }
            if (resultCode == DoodleActivity.RESULT_OK) {
                String path = data.getStringExtra(DoodleActivity.KEY_IMAGE_PATH);
                if (TextUtils.isEmpty(path)) {
                    return;
                }
                //ImageLoader.getInstance(this).display(findViewById(R.id.img), path);
                mPath.setText(path);
            } else if (resultCode == DoodleActivity.RESULT_ERROR) {
                Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void startUcrop(String path) {
        UCrop.Options options = new UCrop.Options();
        options.setShowCropFrame(true);
        options.setShowCropGrid(true);
        options.setDragFrameEnabled(true);
        options.setScaleEnabled(true);
        options.setRotateEnabled(true);
        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(true);

        UCrop.of(Uri.fromFile(new File(path)), Uri.fromFile(new File(getExternalCacheDir().getPath(),
                System.currentTimeMillis() + "")))
                .withMaxResultSize(1000, 1000)
                .withOptions(options)
                .start(this);
    }
}
