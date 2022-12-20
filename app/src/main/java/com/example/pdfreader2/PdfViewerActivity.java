package com.example.pdfreader2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import org.opencv.android.Utils;
import org.opencv.core.Mat;


public class PdfViewerActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener{

    private PDFView pdfView;
    private ImageButton mZoomInButton;
    private ImageButton mZoomOutButton;
    private ImageButton mPageLeftButton;
    private ImageButton mPageRightButton;
    private ImageButton mFindBubblesButton;
    private ImageButton mTranslateWordButton;
    private ImageButton mToggleBubbleTranslateButton;
    private ImageButton mJumpToPageButton;

    private Page page = new Page();
    private List<Integer> cumXOffset = new ArrayList<>();

    private Map<Integer, List<Rectangle>> translatedPages = new HashMap<>();
    private Map<Integer, List<Rectangle>> translatedPagesWords = new HashMap<>();
    private Map<Integer, List<Rectangle>> translatedPagesDetector = new HashMap<>();
    File file;
    Bitmap pdfPageAsBitmap;
    boolean translateBubbleFlag = false;
    boolean translateWordFlag = false;

    BubblesClassifier bubblesClassifier;
    BubblesDetector bubblesDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        String path = getIntent().getStringExtra("path");
        file = new File(path);

        pdfView = findViewById(R.id.pdfView);
        pdfView.fromFile(file)
                .onTap(new onTapListener())
                .onDraw(new OnDrawListener())
                //.onPageScroll(new OnPageScrollListener())
                .fitEachPage(true)
                .pageFitPolicy(FitPolicy.HEIGHT)
                .swipeHorizontal(true)
                .pageSnap(true)
                .autoSpacing(true)
                .pageFling(true)
                .onPageChange(new OnPageChangeListener())
                .load();

        pdfView.setMidZoom(2f);

        //TODO: zoomout ma nie przecuac ekranu do dolnego lewego rogu, a zoomin ma nie przerzucac do gornego lewego rogu
        //TODO: naprawic, ze niezawsze dobrze sie wyszarza

        //przyblizenie
        mZoomInButton = findViewById(R.id.zoomIn);
        mZoomInButton.setAlpha(0.75f);
        mZoomInButton.setOnClickListener(view -> {
            int curPage = pdfView.getCurrentPage();

            if (pdfView.getZoom() < 5f) {
                pdfView.zoomTo(pdfView.getZoom() + 1f);
                pdfView.jumpTo(curPage);
                pdfView.loadPages();
            }

            if (pdfView.getZoom() == 5f) {
                mZoomInButton.setEnabled(false);
                mZoomInButton.setAlpha(.35f);
                mZoomInButton.setClickable(false);

                mZoomOutButton.setEnabled(true);
                mZoomOutButton.setAlpha(1f);
                mZoomOutButton.setClickable(true);
            } else
            {
                mZoomInButton.setEnabled(true);
                mZoomInButton.setAlpha(1f);
                mZoomInButton.setClickable(true);

                mZoomOutButton.setEnabled(true);
                mZoomOutButton.setAlpha(1f);
                mZoomOutButton.setClickable(true);
            }
        });


        //oddalenie
        mZoomOutButton = findViewById(R.id.zoomOut);

        mZoomOutButton.setEnabled(false);
        mZoomOutButton.setAlpha(.35f);
        mZoomOutButton.setClickable(false);

        mZoomOutButton.setOnClickListener(view -> {
            int curPage = pdfView.getCurrentPage();
            float curZoom = pdfView.getZoom();

            if (curZoom >= 2f) {
                pdfView.zoomTo(curZoom - 1f);
                pdfView.jumpTo(curPage);
                pdfView.loadPages();
            } else if (curZoom > 1f){
                pdfView.zoomTo(1f);
                pdfView.jumpTo(curPage);
                pdfView.loadPages();
            }

            if (pdfView.getZoom() == 1f) {
                mZoomOutButton.setEnabled(false);
                mZoomOutButton.setAlpha(.35f);
                mZoomOutButton.setClickable(false);

            } else
            {
                mZoomOutButton.setEnabled(true);
                mZoomOutButton.setAlpha(0.75f);
                mZoomOutButton.setClickable(true);

            }
            mZoomInButton.setEnabled(true);
            mZoomInButton.setAlpha(0.75f);
            mZoomInButton.setClickable(true);
        });

        //zmiana strony w lewo
        mPageLeftButton = findViewById(R.id.pageLeft);
        mPageLeftButton.setAlpha(0.75f);
        mPageLeftButton.setOnClickListener(view -> {
            int curPage = pdfView.getCurrentPage();
            if (curPage >= 1) {
                pdfView.jumpTo(curPage - 1);
            }
        });


        //zmiana strony w prawo
        mPageRightButton = findViewById(R.id.pageRight);
        mPageRightButton.setAlpha(0.75f);
        mPageRightButton.setOnClickListener(view -> {
            int curPage = pdfView.getCurrentPage();
            if (curPage < pdfView.getPageCount() - 1) {
                pdfView.jumpTo(curPage + 1);
            }
        });

        //szukanie dymkow

        Log.d("THREAD_TEST", "startOfEast");
        String pathEast = getPath("frozen_east_text_detection.pb", this);
        bubblesDetector = new BubblesDetector(pathEast);
        Log.d("THREAD_TEST", "endOfEast");

        Log.d("THREAD_TEST", "startOfANN");
        String pathAnn = getPath("ANN_BubbleClassifier.yml", this);
        bubblesClassifier = new BubblesClassifier(pathAnn);
        Log.d("THREAD_TEST", "endOfANN");



        mFindBubblesButton = findViewById(R.id.findBubbles);
        mFindBubblesButton.setAlpha(0.75f);
        mFindBubblesButton.setOnClickListener(view -> {
            //mFindBubblesButton.setEnabled(false);
            page = new Page();
            int DDpi = getResources().getDisplayMetrics().densityDpi;
            ExampleRunnable runnable = new ExampleRunnable(DDpi, file, pdfView.getCurrentPage(),
                    page, bubblesDetector, bubblesClassifier);
            new Thread(runnable).start();
        });

        //tlumaczenie chmurki

        mToggleBubbleTranslateButton = findViewById(R.id.toggleBubbleTranslate);
        mToggleBubbleTranslateButton.setAlpha(0.75f);
        mToggleBubbleTranslateButton.setOnClickListener(view -> {
            if (translateBubbleFlag) {
                mToggleBubbleTranslateButton.setAlpha(0.75f);
                translateBubbleFlag = false;
            }
            else {
                mToggleBubbleTranslateButton.setAlpha(1f);
                translateBubbleFlag = true;
                mTranslateWordButton.setAlpha(0.75f);
                translateWordFlag = false;
            }
        });

        //tlumaczenie slowa

        mTranslateWordButton = findViewById(R.id.translateWordButton);
        mTranslateWordButton.setAlpha(0.75f);
        mTranslateWordButton.setOnClickListener(view -> {
            //translatedPages.remove(pdfView.getCurrentPage());
            //pdfView.invalidate();
            if (translateWordFlag) {
                mTranslateWordButton.setAlpha(0.75f);
                translateWordFlag = false;
            }
            else {
                mTranslateWordButton.setAlpha(1f);
                translateWordFlag = true;
                translateBubbleFlag = false;
                mToggleBubbleTranslateButton.setAlpha(0.75f);
            }
        });

        //skok strony

        mJumpToPageButton = findViewById(R.id.jumpToPage);
        mJumpToPageButton.setAlpha(0.75f);
        mJumpToPageButton.setOnClickListener(view -> openDialog());
    }

    public void openDialog() {
        ExampleDialog exampleDialog = new ExampleDialog(1, pdfView.getPageCount(), pdfView.getCurrentPage() + 1);
        exampleDialog.show(getSupportFragmentManager(), "example dialog");
    }

    public void openDialogWord(String word, String translation) {
        DialogWord exampleDialog = new DialogWord(word, translation);
        exampleDialog.show(getSupportFragmentManager(), "example dialog");
    }

    @Override
    public void applyTexts(int pageNumber) {
        pdfView.jumpTo(pageNumber - 1);
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        //okazalo sie, ze to jest sciezka do assetow: context.getFilesDir().getAbsolutePath()
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i("TAG", "Failed to upload a file");
        }
        return context.getFilesDir().getAbsolutePath() + "/" + file;
    }

    class BubbleRunnable implements Runnable {
        int DDpi;
        File file;
        int pageIdx;
        Page page;
        BubblesDetector bubblesDetector;
        BubblesClassifier bubblesClassifier;
        float thisPageXRealScale;
        float thisPageYRealScale;

        BubbleRunnable(int DDpi, File file, int pageIdx, Page page,
                       BubblesDetector bubblesDetector, BubblesClassifier bubblesClassifier,
                       float thisPageXRealScale, float thisPageYRealScale) {
            this.DDpi = DDpi;
            this.file = file;
            this.pageIdx = pageIdx;
            this.page = page;
            this.bubblesDetector = bubblesDetector;
            this.bubblesClassifier = bubblesClassifier;
            this.thisPageXRealScale = thisPageXRealScale;
            this.thisPageYRealScale = thisPageYRealScale;
        }

        @Override
        public void run() {

            Log.d("THREAD_TEST", "startThread");
            try {
                //to jest to co skanuje strone cala
                Page page2 = new Page(pdfPageToBitmap(file, pageIdx));
                Rectangle foundBubble;

                float onePageWidth = pdfView.getPageSize(pageIdx).getWidth();
                float pageDetectorWidth = page2.getOrig_image().cols();
                float proportionMapping = pageDetectorWidth / onePageWidth;

                List<Rectangle> speechBubblesRealXY = new ArrayList<>();

                Handler threadHandler = new Handler(Looper.getMainLooper());
                if (!translatedPagesDetector.containsKey(pageIdx)) {
                    page2 = scanPage(file, pageIdx, bubblesDetector, bubblesClassifier);
                    List<Rectangle> tmp = new ArrayList<>();
                    tmp.addAll(page2.getSpeech_bubbles());
                    tmp.addAll(page2.getRejected_speech_bubbles());

                    Log.d("THREAD_TEST", "startChangingXYToReal");
                    speechBubblesRealXY = mapToRealXY(tmp, proportionMapping);
                    //to jest tak, bo musi byc w threadzie
                    List<Rectangle> finalSpeechBubblesRealXY = speechBubblesRealXY;
                    threadHandler.post(() -> {
                        translatedPagesDetector.put(pageIdx, tmp);
                        translatedPages.put(pageIdx, finalSpeechBubblesRealXY);
                    });
                    foundBubble = tappedRectangle(speechBubblesRealXY, thisPageXRealScale, thisPageYRealScale);
                    //^
                }
                else {
                    foundBubble = tappedRectangle(translatedPages.get(pageIdx), thisPageXRealScale, thisPageYRealScale);
                }
                //^

                //tutaj pisac
                Log.d("TESTWYKRYCIA", String.valueOf(foundBubble));

                //jezeli nie ma tekstu, to trzeba przetlumaczyc i ustawic
                String translatedText = "";
                List<Rectangle> newBubbleWords = new ArrayList<>();

                if (Objects.isNull(foundBubble.getText())) {
                    String pathTesseract = getPathTess("eng.traineddata", getContext());
                    TessBaseAPI tess = new TessBaseAPI();

                    if (!tess.init(pathTesseract, "eng")) {
                        Log.d("TESTTESSERACT", "nie dziala");
                        // Error initializing Tesseract (wrong data path or language)
                        tess.recycle();
                    }

                    Rectangle tappedRect = new Rectangle(
                            (int) (foundBubble.getStartX() * proportionMapping),
                            (int) (foundBubble.getStartY() * proportionMapping),
                            (int) (foundBubble.getEndX() * proportionMapping),
                            (int) (foundBubble.getEndY() * proportionMapping));
                    translatedText = translateBubble(tappedRect, page2, tess);
                    Log.d("translatedText", translatedText);

                    // i dorobic slowa
                    //TODO: nie ma tutaj uzytych dwoch modeli wiec robie tylko na jednym, nie wiem jak powinno byc???

                        /*String box_2 = tess.getBoxText(0);
                        int c2 = tess.meanConfidence();
                        Log.d("confidence", c1 + " " + c2);*/

                    String text = "";
                    String box = "";
                        /*if (c2 > c1) {
                            text = text_2;
                            box = box_2;
                        } else {
                            text = text_1;
                            box = box_1;
                        }*/
                    text = tess.getUTF8Text();
                    box = tess.getBoxText(0);


                    text = WordCheck.removeSingleChars(text);

                    text = text.trim().replaceAll(" +", " ");

                    if (!text.isEmpty() & !text.equals(" ")) {
                        text.toUpperCase();
                        Log.d("ocr", text + " " + text.length());

                        List<Rectangle> bubbleWords = WordCheck.words_position(text, box);


                        for (Rectangle bubbleWord : bubbleWords) {
                            Rectangle rectangle_new = new Rectangle(
                                    (int) ((tappedRect.getStartX() + bubbleWord.getStartX()) / proportionMapping),
                                    (int) ((tappedRect.getEndY() - bubbleWord.getEndY()) / proportionMapping),
                                    (int) ((tappedRect.getStartX() + bubbleWord.getEndX()) / proportionMapping),
                                    (int) ((tappedRect.getEndY() - bubbleWord.getStartY()) / proportionMapping));
                            rectangle_new.setText(bubbleWord.getText());
                            newBubbleWords.add(rectangle_new);
                        }
                    }
                    //^
                }
                else {
                    translatedText = foundBubble.getText();
                }

                //jezeli visible to invis, i na odwrot
                String finalTranslatedText = translatedText;
                threadHandler.post(() -> {
                    foundBubble.setVisible(!foundBubble.isVisible());
                    foundBubble.setText(finalTranslatedText);
                    if (!newBubbleWords.isEmpty()) {
                        if (translatedPagesWords.containsKey(pageIdx)) {
                            translatedPagesWords.get(pageIdx).addAll(newBubbleWords);
                        }
                        else {
                            translatedPagesWords.put(pageIdx, newBubbleWords);
                        }
                    }
                    pdfView.invalidate();
                });
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class WordRunnable implements Runnable {
        int DDpi;
        File file;
        int pageIdx;
        Page page;
        BubblesDetector bubblesDetector;
        BubblesClassifier bubblesClassifier;
        float thisPageXRealScale;
        float thisPageYRealScale;

        WordRunnable(int DDpi, File file, int pageIdx, Page page,
                       BubblesDetector bubblesDetector, BubblesClassifier bubblesClassifier,
                       float thisPageXRealScale, float thisPageYRealScale) {
            this.DDpi = DDpi;
            this.file = file;
            this.pageIdx = pageIdx;
            this.page = page;
            this.bubblesDetector = bubblesDetector;
            this.bubblesClassifier = bubblesClassifier;
            this.thisPageXRealScale = thisPageXRealScale;
            this.thisPageYRealScale = thisPageYRealScale;
        }

        @Override
        public void run() {

            Log.d("THREAD_TEST", "startThread");
            try {
                //to jest to co skanuje strone cala
                Page page2 = new Page(pdfPageToBitmap(file, pageIdx));
                Rectangle foundBubble;

                float onePageWidth = pdfView.getPageSize(pageIdx).getWidth();
                float pageDetectorWidth = page2.getOrig_image().cols();
                float proportionMapping = pageDetectorWidth / onePageWidth;

                List<Rectangle> speechBubblesRealXY = new ArrayList<>();

                Handler threadHandler = new Handler(Looper.getMainLooper());
                if (!translatedPagesDetector.containsKey(pageIdx)) {
                    page2 = scanPage(file, pageIdx, bubblesDetector, bubblesClassifier);
                    List<Rectangle> tmp = new ArrayList<>();
                    tmp.addAll(page2.getSpeech_bubbles());
                    tmp.addAll(page2.getRejected_speech_bubbles());

                    Log.d("THREAD_TEST", "startChangingXYToReal");
                    speechBubblesRealXY = mapToRealXY(tmp, proportionMapping);
                    //to jest tak, bo musi byc w threadzie
                    List<Rectangle> finalSpeechBubblesRealXY = speechBubblesRealXY;
                    threadHandler.post(() -> {
                        translatedPagesDetector.put(pageIdx, tmp);
                        translatedPages.put(pageIdx, finalSpeechBubblesRealXY);
                    });
                    foundBubble = tappedRectangle(speechBubblesRealXY, thisPageXRealScale, thisPageYRealScale);
                    //^
                }
                else {
                    foundBubble = tappedRectangle(translatedPages.get(pageIdx), thisPageXRealScale, thisPageYRealScale);
                }
                //^

                //tutaj pisac
                Log.d("TESTWYKRYCIA", String.valueOf(foundBubble));

                //jezeli nie ma tekstu, to trzeba przetlumaczyc i ustawic
                String translatedText = "";
                List<Rectangle> newBubbleWords = new ArrayList<>();

                if (Objects.isNull(foundBubble.getText())) {
                    String pathTesseract = getPathTess("eng.traineddata", getContext());
                    TessBaseAPI tess = new TessBaseAPI();

                    if (!tess.init(pathTesseract, "eng")) {
                        Log.d("TESTTESSERACT", "nie dziala");
                        // Error initializing Tesseract (wrong data path or language)
                        tess.recycle();
                    }

                    Rectangle tappedRect = new Rectangle(
                            (int) (foundBubble.getStartX() * proportionMapping),
                            (int) (foundBubble.getStartY() * proportionMapping),
                            (int) (foundBubble.getEndX() * proportionMapping),
                            (int) (foundBubble.getEndY() * proportionMapping));
                    translatedText = translateBubble(tappedRect, page2, tess);
                    Log.d("translatedText", translatedText);

                    // i dorobic slowa
                    //TODO: nie ma tutaj uzytych dwoch modeli wiec robie tylko na jednym, nie wiem jak powinno byc???

                        /*String box_2 = tess.getBoxText(0);
                        int c2 = tess.meanConfidence();
                        Log.d("confidence", c1 + " " + c2);*/

                    String text = "";
                    String box = "";
                        /*if (c2 > c1) {
                            text = text_2;
                            box = box_2;
                        } else {
                            text = text_1;
                            box = box_1;
                        }*/
                    text = tess.getUTF8Text();
                    box = tess.getBoxText(0);


                    text = WordCheck.removeSingleChars(text);

                    text = text.trim().replaceAll(" +", " ");

                    if (!text.isEmpty() & !text.equals(" ")) {
                        text.toUpperCase();
                        Log.d("ocr", text + " " + text.length());

                        List<Rectangle> bubbleWords = WordCheck.words_position(text, box);


                        for (Rectangle bubbleWord : bubbleWords) {
                            Rectangle rectangle_new = new Rectangle(
                                    (int) ((tappedRect.getStartX() + bubbleWord.getStartX()) / proportionMapping),
                                    (int) ((tappedRect.getEndY() - bubbleWord.getEndY()) / proportionMapping),
                                    (int) ((tappedRect.getStartX() + bubbleWord.getEndX()) / proportionMapping),
                                    (int) ((tappedRect.getEndY() - bubbleWord.getStartY()) / proportionMapping));
                            rectangle_new.setText(bubbleWord.getText());
                            newBubbleWords.add(rectangle_new);
                        }
                    }
                    //^
                }
                else {
                    translatedText = foundBubble.getText();
                }

                //jezeli visible to invis, i na odwrot
                String finalTranslatedText = translatedText;
                //merge lista do szukanai tapnietego slowa
                List<Rectangle> possibleWords = new ArrayList<>();
                if (!newBubbleWords.isEmpty()) {
                    if (translatedPagesWords.containsKey(pageIdx)) {
                        for (Rectangle word : translatedPagesWords.get(pageIdx)) {
                            Rectangle rectangle_new = new Rectangle(
                                    word.getStartX(),
                                    word.getStartY(),
                                    word.getEndX(),
                                    word.getEndY());
                            rectangle_new.setText(word.getText());
                            possibleWords.add(rectangle_new);
                        }
                    }
                    for (Rectangle word : newBubbleWords) {
                        Rectangle rectangle_new = new Rectangle(
                                word.getStartX(),
                                word.getStartY(),
                                word.getEndX(),
                                word.getEndY());
                        rectangle_new.setText(word.getText());
                        possibleWords.add(rectangle_new);
                    }
                }
                else {
                    if (translatedPagesWords.containsKey(pageIdx)) {
                        for (Rectangle word : translatedPagesWords.get(pageIdx)) {
                            Rectangle rectangle_new = new Rectangle(
                                    word.getStartX(),
                                    word.getStartY(),
                                    word.getEndX(),
                                    word.getEndY());
                            rectangle_new.setText(word.getText());
                            possibleWords.add(rectangle_new);
                        }
                    }
                }
                //^
                threadHandler.post(() -> {
                    foundBubble.setText(finalTranslatedText);
                    if (!newBubbleWords.isEmpty()) {
                        if (translatedPagesWords.containsKey(pageIdx)) {
                            translatedPagesWords.get(pageIdx).addAll(newBubbleWords);
                        }
                        else {
                            translatedPagesWords.put(pageIdx, newBubbleWords);
                        }
                    }
                });

                //sprawdzenie czy sie tapnelo slowo:
                Rectangle foundBubbleWord = null;
                if (!possibleWords.isEmpty()) {
                    foundBubbleWord = tappedRectangle(possibleWords, thisPageXRealScale, thisPageYRealScale);
                }
                if (!Objects.isNull(foundBubbleWord)) {
                    //robi tlumaczenie tekstu


                    String text = foundBubbleWord.getText();
                    text = WordCheck.removeSingleChars(text);
                    String tlum = "";
                    try {
                        String authKey = "06bc20c9-0730-62bc-55c6-7d94d7c98be9:fx";
                        Translator translator = new Translator(authKey);
                        TextResult result =
                                translator.translateText(text, null, "pl");
                        tlum = result.getText().replace("Š", "Ą");
                        Log.d("tlum", text + " | " + tlum);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    //otworzyc dialog
                    openDialogWord(foundBubbleWord.getText(), tlum);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ExampleRunnable implements Runnable {
        int DDpi;
        File file;
        int pageIdx;
        Page page;
        BubblesDetector bubblesDetector;
        BubblesClassifier bubblesClassifier;


        ExampleRunnable(int DDpi, File file, int pageIdx, Page page,
                        BubblesDetector bubblesDetector, BubblesClassifier bubblesClassifier) {
            this.DDpi = DDpi;
            this.file = file;
            this.pageIdx = pageIdx;
            this.page = page;
            this.bubblesDetector = bubblesDetector;
            this.bubblesClassifier = bubblesClassifier;
        }

        @Override
        public void run() {

            //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
            Log.d("THREAD_TEST", "startThread");
            try {
                Handler threadHandler = new Handler(Looper.getMainLooper());
                threadHandler.post(() -> {
                    //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                    mFindBubblesButton.setEnabled(false);
                    mFindBubblesButton.setAlpha(.35f);
                    mFindBubblesButton.setClickable(false);
                });

                //to jest to co skanuje strone cala
                Page page2 = new Page(pdfPageToBitmap(file, pageIdx));

                float onePageWidth = pdfView.getPageSize(pageIdx).getWidth();
                float pageDetectorWidth = page2.getOrig_image().cols();
                float proportionMapping = pageDetectorWidth / onePageWidth;

                List<Rectangle> speechBubblesRealXY = new ArrayList<>();
                List<Rectangle> speechBubblesWordsRealXY = new ArrayList<>();

                if (!translatedPagesDetector.containsKey(pageIdx)) {
                    page2 = scanPage(file, pageIdx, bubblesDetector, bubblesClassifier);
                    List<Rectangle> tmp = new ArrayList<>();
                    tmp.addAll(page2.getSpeech_bubbles());
                    tmp.addAll(page2.getRejected_speech_bubbles());

                    Log.d("THREAD_TEST", "startChangingXYToReal");
                    speechBubblesRealXY = mapToRealXY(tmp, proportionMapping);
                    //to jest tak, bo musi byc w threadzie
                    List<Rectangle> finalSpeechBubblesRealXY = speechBubblesRealXY;
                    threadHandler.post(() -> {
                        translatedPagesDetector.put(pageIdx, tmp);
                        translatedPages.put(pageIdx, finalSpeechBubblesRealXY);
                    });
                    //^


                }
                //^

                String pathTesseract = getPathTess("eng.traineddata", getContext());
                TessBaseAPI tess = new TessBaseAPI();

                if (!tess.init(pathTesseract, "eng")) {
                    Log.d("TESTTESSERACT", "nie dziala");
                    // Error initializing Tesseract (wrong data path or language)
                    tess.recycle();
                }


                List<Rectangle> translatedPagesCopy = new ArrayList<>();
                for (Rectangle bubble : translatedPages.get(pageIdx)) {
                    Rectangle rectangle_new = new Rectangle(
                            (bubble.getStartX()),
                            (bubble.getStartY()),
                            (bubble.getEndX()),
                            (bubble.getEndY()));
                    rectangle_new.setAccepted(bubble.isAccepted());
                    rectangle_new.setVisible(bubble.isVisible());
                    rectangle_new.setBackground_color(bubble.getBackground_color());

                    translatedPagesCopy.add(rectangle_new);
                }

                for (int i = 0; i < translatedPagesDetector.get(pageIdx).size(); i++) {
                    Rectangle bubble = translatedPagesDetector.get(pageIdx).get(i);
                    if (bubble.isAccepted()) {
                        //translate accepted bubbles on page and add to speechBubblesRealXY
                        //String tlum = translateBubble(bubble, page2, tess);
                        String tlum = "";

                        Bitmap bitmap = getTappedRectangleAsBitmap(bubble, page2);
                        Paint paint = new Paint();
                        Canvas canvas = new Canvas(bitmap);
                        ColorMatrix cm = new ColorMatrix();
                        float a = 77f;
                        float b = 151f;
                        float c = 28f;
                        float t = 120 * -256f;
                        cm.set(new float[]{a, b, c, 0, t, a, b, c, 0, t, a, b, c, 0, t, 0, 0, 0, 1, 0});
                        paint.setColorFilter(new ColorMatrixColorFilter(cm));
                        canvas.drawBitmap(bitmap, 0, 0, paint);

                        Bitmap bitmap2 = getTappedRectangleAsBitmap(bubble, page2);


                        tess.setImage(bitmap);
                        String text_1 = tess.getUTF8Text();
                        String box_1 = tess.getBoxText(0);
                        int c1 = tess.meanConfidence();

                        tess.setImage(bitmap2);
                        String text_2 = tess.getUTF8Text();
                        String box_2 = tess.getBoxText(0);
                        int c2 = tess.meanConfidence();

                        //TODO: sprawdzic ktory lepszy i wybrac jeden jezeli jest przewaga znaczna
                        Log.d("confidence", c1 + " " + c2);

                        String text = "";
                        String box = "";
                        if (c2 > c1) {
                            text = text_2;
                            box = box_2;
                        } else {
                            text = text_1;
                            box = box_1;
                        }

                        text = WordCheck.removeSingleChars(text);

                        text = text.trim().replaceAll(" +", " ");
                        if (text.isEmpty() | text.equals(" ")) {
                            continue;
                        }
                        text.toUpperCase();
                        Log.d("ocr", text + " " + text.length());
                        List<Rectangle> bubbleWords = WordCheck.words_position(text, box);

                        for (Rectangle bubbleWord : bubbleWords) {
                            Rectangle rectangle_new = new Rectangle(
                                    (int) ((bubble.getStartX() + bubbleWord.getStartX()) / proportionMapping),
                                    (int) ((bubble.getEndY() - bubbleWord.getEndY()) / proportionMapping),
                                    (int) ((bubble.getStartX() + bubbleWord.getEndX()) / proportionMapping),
                                    (int) ((bubble.getEndY() - bubbleWord.getStartY()) / proportionMapping));
                            rectangle_new.setText(bubbleWord.getText());
                            speechBubblesWordsRealXY.add(rectangle_new);
                        }


                        try {
                            String authKey = "06bc20c9-0730-62bc-55c6-7d94d7c98be9:fx";
                            Translator translator = new Translator(authKey);
                            TextResult result =
                                    translator.translateText(text, null, "pl");
                            tlum = result.getText().replace("Š", "Ą");
                            Log.d("tlum", text + " | " + tlum);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //TODO: nie wiem czy ta prawa strona ora jest git
                        if (Objects.equals(tlum, "") | tlum.equals(text)) {
                            continue;
                        }
                        translatedPagesCopy.get(i).setText(tlum);
                        translatedPagesCopy.get(i).setVisible(true);
                    }
                    else {
                        translatedPagesCopy.get(i).setVisible(false);
                    }
                }

                Page finalPage = page2;
                threadHandler.post(() -> {
                    Log.d("THREAD_TEST", "startUIThread");
                    //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                    mFindBubblesButton.setEnabled(true);
                    mFindBubblesButton.setAlpha(.75f);
                    mFindBubblesButton.setClickable(true);
                    page.setOrig_image(finalPage.getOrig_image());
                    page.setSpeech_bubbles(finalPage.getSpeech_bubbles());
                    page.setRejected_speech_bubbles(finalPage.getRejected_speech_bubbles());
                    translatedPages.put(pageIdx, translatedPagesCopy);
                    translatedPagesWords.put(pageIdx, speechBubblesWordsRealXY);
                    pdfView.invalidate();
                    Log.d("THREAD_TEST", "endUIThread");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //zmiana jednej strony pdfa na bitmape
    private Bitmap pdfPageToBitmap(File pdfFile, int pageIdx) {
        Bitmap bitmap = null;
        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));
            PdfRenderer.Page page = renderer.openPage(pageIdx);

            float onePageWidth = pdfView.getPageSize(pageIdx).getWidth();
            float onePageHeight = pdfView.getPageSize(pageIdx).getHeight();
            float wxhProportion = onePageWidth / onePageHeight;
            int max_px = 13500000;
            //int max_px = 27000000;
            int width = (int) Math.floor(Math.sqrt(max_px * wxhProportion));
            int height = (int) (width / wxhProportion);
            Log.d("height and width", width + " x " + height);
            Log.d("wymiary", "w PdfRenderer:" + width + " x " + height);
            Log.d("height * width", String.valueOf(width * height));

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // close the page
            page.close();

            // close the renderer
            renderer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitmap;
    }


    public static Rectangle tappedRectangle(List<Rectangle> rectangles, float x, float y) {
        Rectangle resultRectangle = null;

        for (Rectangle rectangle : rectangles) {
            if (rectangle.getStartX() <= x && x <= rectangle.getEndX() && rectangle.getStartY() <= y && y <= rectangle.getEndY()) {
                resultRectangle = rectangle;
                break;
            }
        }
        return resultRectangle;
    }

    private static String getPathTess(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            Log.d("testPlikuTesss", context.getFilesDir() + "/tessdata");
            new File(context.getFilesDir() + "/tessdata").mkdirs();
            File outFile = new File(context.getFilesDir() + "/tessdata", file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return context.getFilesDir().getAbsolutePath();
        } catch (IOException ex) {
            Log.i("TAG", "Failed to upload a file");
        }
        return context.getFilesDir().getAbsolutePath();
    }

    private Context getContext() {
        return this;
    }


    private class OnDrawListener implements com.github.barteksc.pdfviewer.listener.OnDrawListener {
        @Override
        public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
            Paint paintBG = new Paint();
            paintBG.setStrokeWidth(3);

            Log.d("Rysowanie", String.valueOf(translatedPages.keySet()));
            if(translatedPages.containsKey(pdfView.getCurrentPage())) {
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.BLACK);
                Log.d("page", String.valueOf(page.getOrig_image()));
                for (Rectangle bubble : translatedPages.get(pdfView.getCurrentPage())) {
                    if (bubble.isVisible()) {
                        Log.d("Bubble", String.valueOf(bubble));
                        int startX = (int) (bubble.getStartX() * pdfView.getZoom());
                        int startY = (int) (bubble.getStartY() * pdfView.getZoom());
                        int endX = (int) (bubble.getEndX() * pdfView.getZoom());
                        int endY = (int) (bubble.getEndY() * pdfView.getZoom());
                        Log.d("background_color", String.valueOf(bubble.getBackground_color()));
                        paintBG.setColor(Color.rgb(bubble.getBackground_color().get(0),
                                bubble.getBackground_color().get(1),
                                bubble.getBackground_color().get(2)));
                        canvas.drawRect(startX, startY, endX, endY, paintBG);
                        RectF rect = new RectF(startX, startY, endX, endY);
                        Rectangle rectangle = new Rectangle(startX, startY, endX, endY);
                        rectangle.setText(bubble.getText());
                        float fontSize = rectangle.getOptTextSize();
                        textPaint.setTextSize(fontSize);
                        StaticLayout sl = new StaticLayout(rectangle.getText(), textPaint,
                                rectangle.getWidth(), Layout.Alignment.ALIGN_CENTER,
                                1, 1, false);
                        canvas.save();
                        canvas.translate(rect.left, rect.top);
                        sl.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }
    }

    class GetPageAsBitmapRunnable implements Runnable {
        File file;
        int pageIdx;
        Page page;

        Bitmap pdfPageAsBitmap2;

        GetPageAsBitmapRunnable(File file, int pageIdx, Page page) {
            this.file = file;
            this.pageIdx = pageIdx;
            this.page = page;
        }

        @Override
        public void run() {
            //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
            Log.d("THREAD_TEST", "startThread");
            try {
                pdfPageAsBitmap2 = pdfPageToBitmap(file, pageIdx);
                Page page2 = new Page(pdfPageAsBitmap2);

                Handler threadHandler = new Handler(Looper.getMainLooper());

                threadHandler.post(() -> {
                    Log.d("THREAD_TEST", "startUIThread");
                    //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                    page.setOrig_image(page2.getOrig_image());
                    pdfPageAsBitmap = pdfPageAsBitmap2;
                    Log.d("THREAD_TEST", "endUIThread");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("THREAD_TEST", "endThread");
        }
    }

    public class OnPageChangeListener implements com.github.barteksc.pdfviewer.listener.OnPageChangeListener {
        @Override
        public void onPageChanged(int pageIdx, int pageCount) {


            //TODO: te dwie do threada. pamietac, ze to musi byc thread w threadzie xd
            if (pageIdx != 0) {
                GetPageAsBitmapRunnable runnable = new GetPageAsBitmapRunnable(file, pageIdx, page);
                new Thread(runnable).start();
            }
            if (pageIdx == 0) {
                mPageLeftButton.setEnabled(false);
                mPageLeftButton.setAlpha(.35f);
                mPageLeftButton.setClickable(false);
            } else {
                mPageLeftButton.setEnabled(true);
                mPageLeftButton.setAlpha(.75f);
                mPageLeftButton.setClickable(true);
            }
            if (pageIdx == (pageCount - 1)) {
                mPageRightButton.setEnabled(false);
                mPageRightButton.setAlpha(.35f);
                mPageRightButton.setClickable(false);
            } else {
                mPageRightButton.setEnabled(true);
                mPageRightButton.setAlpha(.75f);
                mPageRightButton.setClickable(true);
            }
        }
    }

    private Bitmap getTappedRectangleAsBitmap(Rectangle rectangle, Page page) {
        int colStart = rectangle.getStartX();
        int colEnd = rectangle.getEndX();
        int rowStart = rectangle.getStartY();
        int rowEnd = rectangle.getEndY();

        Mat image = new Mat();
        page.getOrig_image().copyTo(image);
        Mat croppedMat = image.submat(rowStart, rowEnd, colStart, colEnd);
        Bitmap bitmap = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croppedMat, bitmap);

        /*Log.d("zapis", "proba zapisu");
        try (FileOutputStream out = new FileOutputStream("a")) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.d("zapis", "udany");
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        Log.d("zapis", "bitmapa textu sczytana");
        return bitmap;
    }


    private class onTapListener implements OnTapListener {
        @Override
        public boolean onTap(MotionEvent e) {

            //x, y = piksel EKRANU TELEFONU
            //mappedX, mappedY = piksel CAŁEJ WSTĘGI PDFA, jezeli jest przyblizony, to wtedy przyblizonego
            //thisPageX, <mappedY> = piksel AKTUALNEJ STRONY, jezeli jest przyblizona, to wtedy przyblizonej
            //thisPageXRealScale, thisPageYRealScale = piksel AKTUALNEJ STRONY, jezeli jest przyblizona, to i tak zwraca normalne wymiary

            if (cumXOffset.isEmpty()) {
                this.updateCumXOffset();
            }

            float x = e.getX();
            float y = e.getY();

            float mappedX = -pdfView.getCurrentXOffset() + x;
            float mappedY = -pdfView.getCurrentYOffset() + y;

            float test = pdfView.getPositionOffset();
            Log.d("ONTAPTEST", "x = " + e.getX() + " | y = " + e.getY());
            Log.d("ONTAPTEST", "mappedX = " + mappedX + " | mappedY = " + mappedY);
            Log.d("ONTAPTEST", "test = " + test);

            int pageIdx = pdfView.getCurrentPage();

            Log.d("ONTAPTEST", "PageIdx = " + pageIdx);

            //W     xH
            //1241.0x1674.0
            /*float onePageWidth = pdfView.getPageSize(0).getWidth();
            //mozliwe ze height jest niepotrzebne bo i tak mappedY nie jest zalezne od strony
            float onePageHeight = pdfView.getPageSize(0).getHeight();
            Log.d("ONTAPTEST", "getPageSize = " + pdfView.getPageSize(0));
            Log.d("ONTAPTEST", "onePageWidth = " + onePageWidth + " | onePageHeight = " + onePageHeight);*/


            float thisPageX;
            float thisPageXRealScale;
            if (pageIdx != 0) {
                thisPageX = mappedX - cumXOffset.get(pageIdx-1) * pdfView.getZoom();
            } else {
                thisPageX = mappedX;
            }
            thisPageXRealScale = thisPageX / pdfView.getZoom();
            float thisPageYRealScale = mappedY / pdfView.getZoom();

            Log.d("ONTAPTEST", "thisPageX = " + thisPageX + " | mappedY = " + mappedY);
            Log.d("ONTAPTEST", "thisPageXRealScale = " + thisPageXRealScale + " | thisPageYRealScale = " + thisPageYRealScale);

            if (translateBubbleFlag) {
                try {
                    int DDpi = getResources().getDisplayMetrics().densityDpi;
                    BubbleRunnable runnable = new BubbleRunnable(DDpi, file, pdfView.getCurrentPage(),
                            page, bubblesDetector, bubblesClassifier, thisPageXRealScale, thisPageYRealScale);
                    new Thread(runnable).start();


//                    Log.d("TESTWYKRYCIA", String.valueOf(foundBubble));
//
//
//                    //jezeli visible to invis, i na odwrot
//                    foundBubble.setVisible(!foundBubble.isVisible());
//
//                    //jezeli nie ma tekstu, to trzeba przetlumaczyc i ustawic
//                    if (Objects.isNull(foundBubble.getText())) {
//                        String pathTesseract = getPathTess("eng.traineddata", getContext());
//                        TessBaseAPI tess = new TessBaseAPI();
//
//                        if (!tess.init(pathTesseract, "eng")) {
//                            Log.d("TESTTESSERACT", "nie dziala");
//                            // Error initializing Tesseract (wrong data path or language)
//                            tess.recycle();
//                        }
//
//                        float pageDetectorHeight = page.getOrig_image().rows();
//                        float onePageHeight2 = pdfView.getPageSize(pageIdx).getHeight();
//                        float proportion = pageDetectorHeight / onePageHeight2;
//                        Rectangle tappedRect = new Rectangle((int) (foundBubble.getStartX() * proportion), (int) (foundBubble.getStartY() * proportion), (int) (foundBubble.getEndX() * proportion), (int) (foundBubble.getEndY() * proportion));
//                        String translatedText = translateBubble(tappedRect, page, tess);
//                        Log.d("translatedText", translatedText);
//                        foundBubble.setText(translatedText);
//                        Log.d("setText", foundBubble.getText());
//
//                        // i dorobic slowa
//                        //TODO: nie ma tutaj uzytych dwoch modeli wiec robie tylko na jednym, nie wiem jak powinno byc???
//
//                        /*String box_2 = tess.getBoxText(0);
//                        int c2 = tess.meanConfidence();
//                        Log.d("confidence", c1 + " " + c2);*/
//
//                        String text = "";
//                        String box = "";
//                        /*if (c2 > c1) {
//                            text = text_2;
//                            box = box_2;
//                        } else {
//                            text = text_1;
//                            box = box_1;
//                        }*/
//                        text = tess.getUTF8Text();
//                        box = tess.getBoxText(0);
//
//
//                        text = WordCheck.removeSingleChars(text);
//
//                        text = text.trim().replaceAll(" +", " ");
//                        if (!text.isEmpty() & !text.equals(" ")) {
//                            List<Rectangle> speechBubbleWords = new ArrayList<>();
//                            text.toUpperCase();
//                            Log.d("ocr", text + " " + text.length());
//
//                            List<Rectangle> bubbleWords = WordCheck.words_position(text, box);
//
//                            for (Rectangle bubbleWord : bubbleWords) {
//                                Rectangle rectangle_new = new Rectangle(
//                                        (int) ((tappedRect.getStartX() + bubbleWord.getStartX()) / proportion),
//                                        (int) ((tappedRect.getEndY() - bubbleWord.getEndY()) / proportion),
//                                        (int) ((tappedRect.getStartX() + bubbleWord.getEndX()) / proportion),
//                                        (int) ((tappedRect.getEndY() - bubbleWord.getStartY()) / proportion));
//                                rectangle_new.setText(bubbleWord.getText());
//                                translatedPagesWords.get(pageIdx).add(rectangle_new);
//                            }
//                        }
//                        //^
//                    }
                } catch (Exception exception) {
                    Log.d("TESTWYKRYCIA", String.valueOf(exception));
                }
            }

            //jednego slowa
            if (translateWordFlag) {
                try {
                    int DDpi = getResources().getDisplayMetrics().densityDpi;
                    WordRunnable runnable = new WordRunnable(DDpi, file, pdfView.getCurrentPage(),
                            page, bubblesDetector, bubblesClassifier, thisPageXRealScale, thisPageYRealScale);
                    new Thread(runnable).start();

//                    Rectangle foundBubble = tappedRectangle(translatedPages.get(pageIdx), thisPageXRealScale, thisPageYRealScale);
//                    Log.d("TESTWYKRYCIA", String.valueOf(foundBubble));
//
//
//                    //jezeli visible to invis, i na odwrot
//                    //foundBubble.setVisible(!foundBubble.isVisible());
//
//                    //jezeli nie ma tekstu, to trzeba przetlumaczyc i ustawic
//                    if (Objects.isNull(foundBubble.getText())) {
//                        String pathTesseract = getPathTess("eng.traineddata", getContext());
//                        TessBaseAPI tess = new TessBaseAPI();
//
//                        if (!tess.init(pathTesseract, "eng")) {
//                            Log.d("TESTTESSERACT", "nie dziala");
//                            // Error initializing Tesseract (wrong data path or language)
//                            tess.recycle();
//                        }
//
//                        float pageDetectorHeight = page.getOrig_image().rows();
//                        float onePageHeight2 = pdfView.getPageSize(pageIdx).getHeight();
//                        float proportion = pageDetectorHeight / onePageHeight2;
//                        Rectangle tappedRect = new Rectangle((int) (foundBubble.getStartX() * proportion), (int) (foundBubble.getStartY() * proportion), (int) (foundBubble.getEndX() * proportion), (int) (foundBubble.getEndY() * proportion));
//                        String translatedText = translateBubble(tappedRect, page, tess);
//                        Log.d("translatedText", translatedText);
//                        foundBubble.setText(translatedText);
//                        Log.d("setText", foundBubble.getText());
//
//                        // i dorobic slowa
//                        //TODO: nie ma tutaj uzytych dwoch modeli wiec robie tylko na jednym, nie wiem jak powinno byc???
//
//                        /*String box_2 = tess.getBoxText(0);
//                        int c2 = tess.meanConfidence();
//                        Log.d("confidence", c1 + " " + c2);*/
//
//                        String text = "";
//                        String box = "";
//                        /*if (c2 > c1) {
//                            text = text_2;
//                            box = box_2;
//                        } else {
//                            text = text_1;
//                            box = box_1;
//                        }*/
//                        text = tess.getUTF8Text();
//                        box = tess.getBoxText(0);
//
//
//                        text = WordCheck.removeSingleChars(text);
//
//                        text = text.trim().replaceAll(" +", " ");
//                        if (!text.isEmpty() & !text.equals(" ")) {
//                            List<Rectangle> speechBubbleWords = new ArrayList<>();
//                            text.toUpperCase();
//                            Log.d("ocr", text + " " + text.length());
//
//                            List<Rectangle> bubbleWords = WordCheck.words_position(text, box);
//
//                            for (Rectangle bubbleWord : bubbleWords) {
//                                Rectangle rectangle_new = new Rectangle(
//                                        (int) ((tappedRect.getStartX() + bubbleWord.getStartX()) / proportion),
//                                        (int) ((tappedRect.getEndY() - bubbleWord.getEndY()) / proportion),
//                                        (int) ((tappedRect.getStartX() + bubbleWord.getEndX()) / proportion),
//                                        (int) ((tappedRect.getEndY() - bubbleWord.getStartY()) / proportion));
//                                rectangle_new.setText(bubbleWord.getText());
//                                translatedPagesWords.get(pageIdx).add(rectangle_new);
//                            }
//                        }
//                        //^
//                    }
//
//                    //sprawdzenie czy sie tapnelo slowo:
//                    Rectangle foundBubbleWord = tappedRectangle(translatedPagesWords.get(pageIdx), thisPageXRealScale, thisPageYRealScale);
//                    if (!Objects.isNull(foundBubbleWord)) {
//                        //robi tlumaczenie tekstu
//                        Translator translator = new Translator();
//                        WordCheck w2 = new WordCheck();
//                        String text = foundBubbleWord.getText();
//                        String tlum = "";
//                        try {
//                            CountDownLatch countDownLatch = new CountDownLatch(1);
//                            translator.run(text, w2, countDownLatch);
//                            countDownLatch.await();
//                            tlum = w2.getTest();
//                            Log.d("tlum", tlum);
//                        } catch (Exception e2) {
//                            e2.printStackTrace();
//                        }
//                        //otworzyc dialog
//                        openDialogWord(foundBubbleWord.getText(), tlum);
//                    }

                } catch (Exception exception) {
                    Log.d("TESTWYKRYCIA", String.valueOf(exception));
                }
            }

            pdfView.invalidate();

            return false;
        }

        public void updateCumXOffset() {

            if (pdfView.getPageCount() == 0) {
                return;
            }

            cumXOffset.add((int) pdfView.getPageSize(0).getWidth());

            for (int i = 1; i < pdfView.getPageCount(); i++) {
                cumXOffset.add(cumXOffset.get(i-1) + (int) pdfView.getPageSize(i).getWidth());
            }
            Log.d("TEST_CUMX", String.valueOf(cumXOffset));
        }
    }

    private String translateBubble(Rectangle bubble, Page page, TessBaseAPI tess) {
        Bitmap bitmap = getTappedRectangleAsBitmap(bubble, page);

        tess.setImage(bitmap);
        String text = tess.getUTF8Text();
        text = WordCheck.removeSingleChars(text);


        String tlum = "";
        try {
            String authKey = "06bc20c9-0730-62bc-55c6-7d94d7c98be9:fx";
            Translator translator = new Translator(authKey);
            TextResult result =
                    translator.translateText(text, null, "pl");
            tlum = result.getText().replace("Š", "Ą");
            Log.d("tlum", text + " | " + tlum);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tlum;
    }

    private Page scanPage(File file, int pageIdx, BubblesDetector bubblesDetector, BubblesClassifier bubblesClassifier) {
        //translatedPagesDetector

        pdfPageAsBitmap = pdfPageToBitmap(file, pageIdx);
        Log.d("THREAD_TEST", "endpdfPageAsBitmapThread" + pdfPageAsBitmap);

        Log.d("THREAD_TEST", "startGenerateSpeechBubblesThread");
        Page page2 = new Page(pdfPageAsBitmap);
        List<Rectangle> speechBubbles;
        List<Rectangle> rejectedBubbles;
        List<List<Rectangle>> allBubbles;
        allBubbles = page2.generate_speech_bubbles(0.5, 0.5 /*0.36*/,
                bubblesDetector, 0.2,
                true, bubblesClassifier);
        speechBubbles = allBubbles.get(0);
        rejectedBubbles = allBubbles.get(1);
        for (Rectangle bubble : speechBubbles) {
            bubble.setAccepted(true);
        }
        for (Rectangle bubble : rejectedBubbles) {
            bubble.setAccepted(false);
        }
        page2.setSpeech_bubbles(speechBubbles);
        page2.setRejected_speech_bubbles(rejectedBubbles);
        Log.d("THREAD_TEST", "endGenerateSpeechBubblesThread" + speechBubbles.toArray().length + speechBubbles);

        return page2;
    }

    private List<Rectangle> mapToRealXY(List<Rectangle> input, float proportionMapping) {
        List<Rectangle> result = new ArrayList<>();

        for (Rectangle bubble : input) {
            Rectangle rectangle_new = new Rectangle(
                    (int) (bubble.getStartX() / proportionMapping),
                    (int) (bubble.getStartY() / proportionMapping),
                    (int) (bubble.getEndX() / proportionMapping),
                    (int) (bubble.getEndY() / proportionMapping));
            rectangle_new.setAccepted(bubble.isAccepted());
            rectangle_new.setVisible(bubble.isVisible());
            rectangle_new.setBackground_color(bubble.getBackground_color());

            result.add(rectangle_new);
        }

        return result;
    }
}