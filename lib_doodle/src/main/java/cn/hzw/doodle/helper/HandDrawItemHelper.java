package cn.hzw.doodle.helper;

import java.util.ArrayList;
import java.util.Iterator;
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

    private List<IDoodleItem> shapeItemStack = new ArrayList<>();

    private List<IDoodleItem> currentEditShapeItemStack = new ArrayList<>();

    private List<IDoodleItem> currentEditItemStack = new ArrayList<>();

    private List<IDoodleItem> pendingEditItemStack = new ArrayList<>();

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
        undoItemStack.add(item);
    }


    public void finishAddToStack(IDoodleItem item){
        /*if (currentEditItemStack.remove(item)) {
            if (handDrawItemStack.contains(item)) {
                addFlag(FLAG_RESET_BACKGROUND);
            } else {
                addItem(item);
                return;
            }
        }*/
    }

    public void clearCurrentHandDrawStack(){
        for (IDoodleItem item : currentEditItemStack){
            handDrawItemStack.remove(item);
            undoItemStack.remove(item);
            redoItemStack.remove(item);
        }
        currentEditItemStack.clear();
    }

    public void clearCurrentShapeStack(){
        for (IDoodleItem item : currentEditShapeItemStack){
            shapeItemStack.remove(item);
            undoItemStack.remove(item);
            redoItemStack.remove(item);
        }
        currentEditShapeItemStack.clear();
    }


    public void undoItemStack(){
        List<IDoodleItem> list = new ArrayList<>(undoItemStack);
        for (int i = list.size() - 1; i >= 0; i--) {
            IDoodleItem item = list.get(i);
            removeHandDrawItem(item);
            redoItemStack.add(0, item);
            break;
        }
    }


    public void redoItemStack(){
        if (redoItemStack.isEmpty()) {
            return;
        }
        Iterator<IDoodleItem> iterator = redoItemStack.iterator();
        while (iterator.hasNext()) {
            IDoodleItem item = iterator.next();
            iterator.remove();
            redoItemInner(item);
            break;
        }
    }

    public void redoItemInner(IDoodleItem iDoodleItem){
        currentEditItemStack.add(iDoodleItem);
        undoItemStack.add(iDoodleItem);
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


    public void removeHandDrawItem(IDoodleItem doodleItem){
        currentEditItemStack.remove(doodleItem);
        pendingEditItemStack.remove(doodleItem);
        handDrawItemStack.remove(doodleItem);
        undoItemStack.remove(doodleItem);
    }


    public boolean handDrawItemIsAdd(IDoodleItem iDoodleItem){
        if (currentEditItemStack.contains(iDoodleItem)){
            return true;
        }
        return false;
    }


    public boolean isResetHandDraw(IDoodleItem iDoodleItem){
        if (handDrawItemStack.contains(iDoodleItem)){
            return true;
        }

        return false;
    }
}
