package com.drhowdydoo.layoutinspector.model;

public class ArrowSet {


    private Arrow leftArrow;
    private Arrow rightArrow;
    private Arrow topArrow;
    private Arrow bottomArrow;

    public ArrowSet(Arrow leftArrow, Arrow rightArrow, Arrow topArrow, Arrow bottomArrow) {
        this.leftArrow = leftArrow;
        this.rightArrow = rightArrow;
        this.topArrow = topArrow;
        this.bottomArrow = bottomArrow;
    }

    public ArrowSet() {
        leftArrow = new Arrow();
        rightArrow = new Arrow();
        topArrow = new Arrow();
        bottomArrow = new Arrow();
    }

    public Arrow getLeftArrow() {
        return leftArrow;
    }

    public void setLeftArrow(Arrow leftArrow) {
        this.leftArrow = leftArrow;
    }

    public Arrow getRightArrow() {
        return rightArrow;
    }

    public void setRightArrow(Arrow rightArrow) {
        this.rightArrow = rightArrow;
    }

    public Arrow getTopArrow() {
        return topArrow;
    }

    public void setTopArrow(Arrow topArrow) {
        this.topArrow = topArrow;
    }

    public Arrow getBottomArrow() {
        return bottomArrow;
    }

    public void setBottomArrow(Arrow bottomArrow) {
        this.bottomArrow = bottomArrow;
    }

    @Override
    public String toString() {
        return "ArrowSet{" +
                "leftArrow=" + leftArrow.toString() +
                ", rightArrow=" + rightArrow.toString() +
                ", topArrow=" + topArrow.toString() +
                ", bottomArrow=" + bottomArrow.toString() +
                '}';
    }

    public void clear(){
        leftArrow.clear();
        rightArrow.clear();
        topArrow.clear();
        bottomArrow.clear();
    }

}
