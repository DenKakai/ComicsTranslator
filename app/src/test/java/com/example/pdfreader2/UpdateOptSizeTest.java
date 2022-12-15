package com.example.pdfreader2;

import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.text.TextPaint;

import org.junit.Before;
import org.junit.Test;

public class UpdateOptSizeTest extends Instrumentation {

    @Test
    public void longerText() {
        Rectangle rectangle1 = new Rectangle(100, 100, 400, 200);
        Rectangle rectangle2 = new Rectangle(100, 100, 400, 200);
        Rectangle rectangle3 = new Rectangle(100, 100, 400, 200);

        rectangle1.setText("This a short text.");
        rectangle2.setText("This one is a little bit longer so should have no bigger text size.");
        rectangle3.setText("This text is the longest so should have no bigger text size than" +
                " rectangle1 and rectangle2.");

        assertTrue(rectangle1.getOptTextSize() >= rectangle2.getOptTextSize());
        assertTrue(rectangle2.getOptTextSize() >= rectangle3.getOptTextSize());
    }

    @Test
    public void biggerRectangles() {
        Rectangle rectangle1 = new Rectangle(100, 100, 200, 200);
        Rectangle rectangle2 = new Rectangle(100, 100, 400, 300);
        Rectangle rectangle3 = new Rectangle(100, 100, 600, 400);

        rectangle1.setText("This a text that has the same length in every rectangle.");
        rectangle2.setText("This a text that has the same length in every rectangle.");
        rectangle3.setText("This a text that has the same length in every rectangle.");

        // bigger rectangle with the same text should has bigger font size

        assertTrue(rectangle1.getOptTextSize() <= rectangle2.getOptTextSize());
        assertTrue(rectangle2.getOptTextSize() <= rectangle3.getOptTextSize());
    }

}