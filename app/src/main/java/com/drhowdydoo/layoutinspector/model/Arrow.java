package com.drhowdydoo.layoutinspector.model;

import androidx.annotation.NonNull;

public class Arrow {

    private float startX;
    private float startY;
    private float endX;
    private float endY;

    public Arrow(float startX, float startY, float endX, float endY) {
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

    public float getStartX() {
        return startX;
    }

    public void setStartX(float startX) {
        this.startX = startX;
    }

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public float getEndX() {
        return endX;
    }

    public void setEndX(float endX) {
        this.endX = endX;
    }

    public float getEndY() {
        return endY;
    }

    public void setEndY(float endY) {
        this.endY = endY;
    }

    public float getCenterX() {
        return ((startX + endX) / 2);
    }

    public float getCenterY(){
        return ((startY + endY) / 2);
    }


    public float length(){
        if (startX == endX) return Math.abs(endY - startY);
        else return Math.abs(endX - startX);
    }

    @NonNull
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
