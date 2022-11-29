package com.example.pdfreader2;

public class Rectangle {
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    public Rectangle(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
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

    @Override
    public String toString() {
        return "Rectangle{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", endX=" + endX +
                ", endY=" + endY +
                '}';
    }

    public boolean isOverlapping(Rectangle other) {
        if (this.endY < other.startY || this.startY > other.endY) {
            return false;
        }
        return this.endX >= other.startX && this.startX <= other.endX;
    }

    public boolean isOverlappingOrNear(Rectangle other, int neighbourPx) {
        if ((this.endY + neighbourPx) < other.startY
                || (this.startY - neighbourPx) > other.endY) {
            return false;
        }
        return (this.endX + neighbourPx) >= other.startX && (this.startX - neighbourPx) <= other.endX;
    }

    public Rectangle makeBiggerRectangle(Rectangle other) {
        return new Rectangle(Math.min(this.startX, other.startX),
                Math.min(this.startY, other.startY),
                Math.max(this.endX, other.endX),
                Math.max(this.endY, other.endY));
    }

}