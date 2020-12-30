package cn.hzw.doodle.ui;

import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleShape;

public interface IEditListener {

    public void setColor(int color);

    public void setMode(DoodlePen doodlePen);

    public void setSize(int size,int index);

    public void setMosaicSize(int size,int index);

    public void setMosaicLevel(int mosaicLevel);

    public void setShape(DoodleShape doodleShape);

    public void onClose();

    public void onDone();

    public void onDown();

    public void onPre();

    public void onNext();

    public void onCropRatioChange(float ratio);

    public void onRotate();

    public void resetDoodle();



}
