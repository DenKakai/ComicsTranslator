package com.example.pdfreader2;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TappedRectangleTest {

    @Test
    public void checkInside() {
        List<Rectangle> rectangleList = new ArrayList<>();
        rectangleList.add(new Rectangle(5, 5, 10, 10));
        Rectangle result = PdfViewerActivity.tappedRectangle(rectangleList, 6, 6);

        assertEquals(result, rectangleList.get(0));
    }

    @Test
    public void checkOnEdge() {
        List<Rectangle> rectangleList = new ArrayList<>();
        rectangleList.add(new Rectangle(5, 5, 10, 10));
        Rectangle result = PdfViewerActivity.tappedRectangle(rectangleList, 5, 6);

        assertEquals(result, rectangleList.get(0));
    }

    @Test
    public void checkOutside() {
        List<Rectangle> rectangleList = new ArrayList<>();
        rectangleList.add(new Rectangle(5, 5, 10, 10));
        Rectangle result = PdfViewerActivity.tappedRectangle(rectangleList, 3, 20);

        assertEquals(result, null);
    }

}