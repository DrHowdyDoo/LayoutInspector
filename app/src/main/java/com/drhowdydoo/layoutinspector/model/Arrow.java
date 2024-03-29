package com.drhowdydoo.layoutinspector.model;

public class Arrow {

    private int startX;
    private int startY;
    private int endX;
    private int endY;

    public Arrow(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public Arrow() {
        startX = 0;
        startY = 0;
        endX = 0;
        endY = 0;
    }

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }

    public int getEndX() {
        return endX;
    }

    public void setEndX(int endX) {
        this.endX = endX;
    }

    public int getEndY() {
        return endY;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }

    public int getCenterX() {
        return ((startX + endX) / 2);
    }

    public int getCenterY(){
        return ((startY + endY) / 2);
    }


    public int length(){
        if (startX == endX) return Math.abs(endY - startY);
        else return Math.abs(endX - startX);
    }

    @Override
    public String toString() {
        return "Arrow{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", endX=" + endX +
                ", endY=" + endY +
                '}';
    }

    public void clear(){
        startX = 0;
        startY = 0;
        endX = 0;
        endY = 0;
    }
}
