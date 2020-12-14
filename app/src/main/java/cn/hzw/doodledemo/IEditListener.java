package cn.hzw.doodledemo;

import cn.hzw.doodle.DoodleShape;

public interface IEditListener {

    public void setColor(int color);

    public void setSize(int size);

    public void setShape(DoodleShape doodleShape);

    public void onClose();

    public void onDown();

}
