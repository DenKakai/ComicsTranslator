package com.example.pdfreader2;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

public class IsOverlappingOrNearTest {

    @Test
    public void Overlapping() {
        Rectangle rectangle1 = new Rectangle(50, 50, 100, 200);
        Rectangle rectangle2 = new Rectangle(30, 40, 80, 80);
        Rectangle rectangle3 = new Rectangle(30, 240, 70, 280);
        Rectangle rectangle4 = new Rectangle(70, 100, 90, 130);
        Rectangle rectangle5 = new Rectangle(100, 70, 120, 90);

        // overlapping
        assertTrue(rectangle1.isOverlappingOrNear(rectangle2, 0, 0));
        assertTrue(rectangle2.isOverlappingOrNear(rectangle1, 0, 0));

        // these two overlaps on x axis, but don't on y axis
        assertFalse(rectangle1.isOverlappingOrNear(rectangle3, 0, 0));
        assertFalse(rectangle3.isOverlappingOrNear(rectangle1, 0, 0));

        // one rectangle is inside the other
        assertTrue(rectangle1.isOverlappingOrNear(rectangle4, 0, 0));
        assertTrue(rectangle4.isOverlappingOrNear(rectangle1, 0, 0));

        // touching edges
        assertTrue(rectangle1.isOverlappingOrNear(rectangle5, 0, 0));
        assertTrue(rectangle5.isOverlappingOrNear(rectangle1, 0, 0));
    }

    @Test
    public void Near() {
        Rectangle rectangle1 = new Rectangle(50, 50, 100, 200);
        Rectangle rectangle2 = new Rectangle(105, 50, 140, 200);
        Rectangle rectangle3 = new Rectangle(50, 205, 80, 300);
        Rectangle rectangle4 = new Rectangle(105, 205, 140, 300);
        Rectangle rectangle5 = new Rectangle(110, 50, 140, 200);

        // Near with 5px difference on x axis
        assertTrue(rectangle1.isOverlappingOrNear(rectangle2, 5, 0));
        assertTrue(rectangle2.isOverlappingOrNear(rectangle1, 5, 0));

        // Near with 5px difference on y axis
        assertTrue(rectangle1.isOverlappingOrNear(rectangle3, 0, 5));
        assertTrue(rectangle3.isOverlappingOrNear(rectangle1, 0, 5));

        // Near with 5px difference on both axes
        assertTrue(rectangle1.isOverlappingOrNear(rectangle4, 5, 5));
        assertTrue(rectangle4.isOverlappingOrNear(rectangle1, 5, 5));

        // Near with 10px difference on x axis, but they are treated as neighbours with max 5px diff
        assertFalse(rectangle1.isOverlappingOrNear(rectangle5, 5, 0));
        assertFalse(rectangle5.isOverlappingOrNear(rectangle1, 5, 0));
    }
}