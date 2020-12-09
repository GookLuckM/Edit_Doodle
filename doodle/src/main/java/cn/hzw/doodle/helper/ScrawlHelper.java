package cn.hzw.doodle.helper;

public class ScrawlHelper {

    private static  ScrawlHelper mInstance;

    public ScrawlHelper newInstance(){
        if (mInstance == null){
            mInstance = new ScrawlHelper();
        }
        return mInstance;
    }


}
