package com.example.pdfreader2;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class Rectangle {
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private String text;
    private int width;
    private int optTextSize;
    private boolean visible;
    private boolean accepted;


    public Rectangle(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.updateWidth();
    }

    private void updateWidth() {
        this.width = this.endX - this.startX;
    }

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
        this.updateWidth();
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
        this.updateWidth();
    }

    public int getEndY() {
        return endY;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }

    public String getText() {return text;}

    public void setText(String text) {
        this.text = text;
        this.updateOptSize();
    }

    public int getWidth() {return width;}

    public int getOptTextSize() {return optTextSize;}

    public void setOptTextSize(int optTextSize) {this.optTextSize = optTextSize;}


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

    public boolean isOverlappingOrNear(Rectangle other, int neighbourPx_w, int neighbourPx_h) {
        if ((this.endY + neighbourPx_h) < other.startY
                || (this.startY - neighbourPx_h) > other.endY) {
            return false;
        }
        return (this.endX + neighbourPx_w) >= other.startX && (this.startX - neighbourPx_w) <= other.endX;
    }

    public boolean isInside(Rectangle other) {
        return (this.startX >= other.startX) & (this.endX <= other.endX) &
                (this.startY >= other.startY) & (this.endY <= other.endY);
    }

    public Rectangle makeBiggerRectangle(Rectangle other) {
        return new Rectangle(Math.min(this.startX, other.startX),
                Math.min(this.startY, other.startY),
                Math.max(this.endX, other.endX),
                Math.max(this.endY, other.endY));
    }

    public void updateOptSize() {
        TextPaint textPaint = new TextPaint();
        for (int size = 10; size < 201; size+=2) {
            textPaint.setTextSize(size);
            StaticLayout sl = new StaticLayout(this.getText(), textPaint,
                    this.getWidth(), Layout.Alignment.ALIGN_CENTER,
                    1, 1, false);
            int sl_height = sl.getHeight();
            if (sl_height > this.getEndY() - this.getStartY()) {
                this.optTextSize = size - 2;
                break;
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}