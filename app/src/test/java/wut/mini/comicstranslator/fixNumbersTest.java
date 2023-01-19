package wut.mini.comicstranslator;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class fixNumbersTest {

    @Test
    public void checkSimple() {
        String string = PdfViewerActivity.fixNumbers("test");

        assertEquals(string, "test");
    }

    @Test
    public void checkShort() {
        String string = PdfViewerActivity.fixNumbers("t");

        assertEquals(string, "t");
    }

    @Test
    public void checkShort2() {
        String string = PdfViewerActivity.fixNumbers("te");

        assertEquals(string, "te");
    }

    @Test
    public void checkShort3() {
        String string = PdfViewerActivity.fixNumbers("t5");

        assertEquals(string, "tS");
    }

    @Test
    public void checkShort4() {
        String string = PdfViewerActivity.fixNumbers("7a");

        assertEquals(string, "Za");
    }

    @Test
    public void checkNormal() {
        String string = PdfViewerActivity.fixNumbers("TOMA57");

        assertEquals(string, "TOMASZ");
    }

    @Test
    public void checkReal() {
        String string = PdfViewerActivity.fixNumbers("TOMA57");

        assertEquals(string, "TOMASZ");
    }

    @Test
    public void checkEmpty() {
        String string = PdfViewerActivity.fixNumbers("");

        assertEquals(string, "");
    }
}
