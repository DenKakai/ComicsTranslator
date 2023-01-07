package wut.mini.comicstranslator;

import static org.junit.Assert.*;

import org.junit.Test;

public class IsInsideTest {

    @Test
    public void isInside() {
        Rectangle rectangle1 = new Rectangle(150, 150, 200, 200);
        Rectangle rectangle2 = new Rectangle(100, 100, 300, 300);

        // rectangle 1 is inside rectangle2
        assertTrue(rectangle1.isInside(rectangle2));
        assertFalse(rectangle2.isInside(rectangle1));
    }

    @Test
    public void outside() {
        Rectangle rectangle1 = new Rectangle(150, 150, 200, 200);
        Rectangle rectangle2 = new Rectangle(201, 201, 300, 300);
        Rectangle rectangle3 = new Rectangle(175, 175, 225, 225);

        // is outside
        assertFalse(rectangle1.isInside(rectangle2));
        assertFalse(rectangle2.isInside(rectangle1));

        // overlapping rectangles
        assertFalse(rectangle1.isInside(rectangle3));
        assertFalse(rectangle3.isInside(rectangle1));
    }
}