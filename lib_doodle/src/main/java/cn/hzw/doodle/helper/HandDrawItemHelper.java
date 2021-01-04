package cn.hzw.doodle.helper;

import java.util.ArrayList;
import java.util.List;

import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleItem;

/**
 * maoning
 * 2021-01-04 23:21
 */
public class HandDrawItemHelper  {

    private static HandDrawItemHelper mInstance;

    private List<IDoodleItem> handDrawItemStack = new ArrayList<>();


    private List<IDoodleItem> currentEditItemStack = new ArrayList<>();


    private List<IDoodleItem> undoItemStack = new ArrayList<>();


    private List<IDoodleItem> redoItemStack = new ArrayList<>();

    public static HandDrawItemHelper newInstance(){
        if (mInstance == null){
            mInstance = new HandDrawItemHelper();
        }

        return mInstance;
    }


    public void drawingAddToStack(IDoodleItem item){
        currentEditItemStack.add(item);
    }


    public void finishAddToStack(IDoodleItem item){
        if (!handDrawItemStack.contains(item)) {
            handDrawItemStack.add(item);
        }
    }


    public void undoItemStack(){
        IDoodleItem remove = undoItemStack.remove(0);
        redoItemStack.add(0,remove);


    }


    public void redoItemStatck(){
        IDoodleItem remove = redoItemStack.remove(0);
        undoItemStack.add(0,remove);
    }


    public void cleanHandDrawItem(){
        List<IDoodleItem> tempList = new ArrayList<>(currentEditItemStack);
        for (IDoodleItem item : tempList){
            undoItemStack.remove(item);
            handDrawItemStack.remove(item);
        }

        for (IDoodleItem item : undoItemStack){
            if (!handDrawItemStack.contains(item)){
                handDrawItemStack.add(item);
            }
        }

        redoItemStack.clear();
    }
}
