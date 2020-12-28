package cn.hzw.doodle.helper;

public class DoodleHelper {

    private static DoodleHelper mInstance;

    private float currentScale;

    public DoodleHelper newInstance(){
        if (mInstance == null){
            mInstance = new DoodleHelper();
        }
        return mInstance;
    }


    public void setScale(float scale){
        this.currentScale = scale;
    }

    


}
