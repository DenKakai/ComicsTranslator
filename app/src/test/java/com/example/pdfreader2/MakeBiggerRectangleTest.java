package com.example.pdfreader2;

import static org.junit.Assert.*;

import org.junit.Test;

public class MakeBiggerRectangleTest {

    @Test
    public void biggerRectFromOverlapping() {
        Rectangle rectangle1 = new Rectangle(100, 100, 200, 200);
        Rectangle rectangle2 = new Rectangle(150, 150, 300, 220);
        Rectangle expected_result = new Rectangle(100, 100, 300, 220);

        Rectangle result = rectangle1.makeBiggerRectangle(rectangle2);

        // rectangle 1 and 2 overlap
        assertEquals(expected_result.getStartX(), result.getStartX());
        assertEquals(expected_result.getStartY(), result.getStartY());
        assertEquals(expected_result.getEndX(), result.getEndX());
        assertEquals(expected_result.getEndY(), result.getEndY());
    }

    @Test
    public void biggerRectFromNotOverlapping() {
        Rectangle rectangle1 = new Rectangle(100, 100, 200, 200);
        Rectangle rectangle2 = new Rectangle(300, 300, 400, 400);
        Rectangle expected_result = new Rectangle(100, 100, 400, 400);

        Rectangle result = rectangle1.makeBiggerRectangle(rectangle2);

        // rectangle 1 and 2 don't overlap
        assertEquals(expected_result.getStartX(), result.getStartX());
        assertEquals(expected_result.getStartY(), result.getStartY());
        assertEquals(expected_result.getEndX(), result.getEndX());
        assertEquals(expected_result.getEndY(), result.getEndY());
    }

    @Test
    public void biggerRectFromOneInside() {
        Rectangle rectangle1 = new Rectangle(100, 100, 200, 200);
        Rectangle rectangle2 = new Rectangle(120, 120, 180, 180);
        Rectangle expected_result = new Rectangle(100, 100, 200, 200);

        Rectangle result = rectangle1.makeBiggerRectangle(rectangle2);

        // rectangle 2 in inside rectangle 1,
        // the result should be copy of rectangle 1
        assertEquals(expected_result.getStartX(), result.getStartX());
        assertEquals(expected_result.getStartY(), result.getStartY());
        assertEquals(expected_result.getEndX(), result.getEndX());
        assertEquals(expected_result.getEndY(), result.getEndY());
    }

}