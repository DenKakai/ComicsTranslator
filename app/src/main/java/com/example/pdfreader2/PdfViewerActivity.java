package com.example.pdfreader2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.Process;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.Callbacks;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.shockwave.pdfium.util.SizeF;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;


public class PdfViewerActivity extends AppCompatActivity{

    private PDFView pdfView;
    private Button mZoomInButton;
    private Button mZoomOutButton;
    private Button mPageLeftButton;
    private Button mPageRightButton;
    private Button mFindBubblesButton;
    private Button mClearBubblesButton;

    private Page page;
    private ArrayList<Bitmap> pdfAsListOfBitmaps;
    private Handler mainHandler = new Handler();
    private List<Integer> cumXOffset = new ArrayList<>();

    private Map<Integer, List<Rectangle>> translatedPages = new HashMap<Integer, List<Rectangle>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        String path = getIntent().getStringExtra("path");
        File file = new File(path);

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
        //TODO: po tym jak sie zrobi clearbubbles/findbubbles i sie dopisza do slownika, to nie wyswietlaja sie dopooki nie wykona sie jeszcze raz ondraw. trzeba to jakos wymusic

        //przyblizenie
        mZoomInButton = (Button) findViewById(R.id.zoomIn);
        mZoomInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int curPage = pdfView.getCurrentPage();

                if (pdfView.getZoom() < 5f) {
                    pdfView.zoomTo(pdfView.getZoom() + 1f);
                    pdfView.jumpTo(curPage);
                    pdfView.loadPages();
                }

                if (pdfView.getZoom() == 5f) {
                    mZoomInButton.setEnabled(false);
                    mZoomInButton.setAlpha(.5f);
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
            }
        });


        //oddalenie
        mZoomOutButton = (Button) findViewById(R.id.zoomOut);

        mZoomOutButton.setEnabled(false);
        mZoomOutButton.setAlpha(.5f);
        mZoomOutButton.setClickable(false);

        mZoomOutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
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

                if (curZoom == 1f) {
                    mZoomOutButton.setEnabled(false);
                    mZoomOutButton.setAlpha(.5f);
                    mZoomOutButton.setClickable(false);

                    mZoomInButton.setEnabled(true);
                    mZoomInButton.setAlpha(1f);
                    mZoomInButton.setClickable(true);
                } else
                {
                    mZoomOutButton.setEnabled(true);
                    mZoomOutButton.setAlpha(1f);
                    mZoomOutButton.setClickable(true);

                    mZoomInButton.setEnabled(true);
                    mZoomInButton.setAlpha(1f);
                    mZoomInButton.setClickable(true);
                }
            }
        });

        //zmiana strony w lewo
        mPageLeftButton = (Button) findViewById(R.id.pageLeft);
        mPageLeftButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int curPage = pdfView.getCurrentPage();
                if (curPage >= 1) {
                    pdfView.jumpTo(curPage - 1);
                }
            }
        });


        //zmiana strony w prawo
        mPageRightButton = (Button) findViewById(R.id.pageRight);
        mPageRightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int curPage = pdfView.getCurrentPage();
                if (curPage < pdfView.getPageCount() - 1) {
                    pdfView.jumpTo(curPage + 1);
                }
            }
        });

        //szukanie dymkow

        Log.d("THREAD_TEST", "startOfEast");
        String pathEast = getPath("frozen_east_text_detection.pb", this);
        BubblesDetector bubblesDetector = new BubblesDetector(pathEast);
        Log.d("THREAD_TEST", "endOfEast");

        Log.d("THREAD_TEST", "startOfANN");
        String pathAnn = getPath("ANN_BubbleClassifier.yml", this);
        BubblesClassifier bubblesClassifier = new BubblesClassifier(pathAnn);
        Log.d("THREAD_TEST", "endOfANN");



        mFindBubblesButton = (Button) findViewById(R.id.findBubbles);
        mFindBubblesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //mFindBubblesButton.setEnabled(false);
                page = new Page();
                int DDpi = getResources().getDisplayMetrics().densityDpi;
                ExampleRunnable runnable = new ExampleRunnable(DDpi, file, pdfView.getCurrentPage(),
                        page, bubblesDetector, bubblesClassifier);
                new Thread(runnable).start();
            }
        });

        //czyszczenie chmurek

        mClearBubblesButton = (Button) findViewById(R.id.clearBubbles);
        mClearBubblesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                translatedPages.remove(pdfView.getCurrentPage());
            }
        });
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
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
        return "";
    }

    class ExampleRunnable implements Runnable {
        int DDpi;
        File file;
        int pageIdx;
        Page page;
        BubblesDetector bubblesDetector;
        BubblesClassifier bubblesClassifier;

        Bitmap pdfPageAsBitmap;

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
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                        mFindBubblesButton.setEnabled(false);
                        mFindBubblesButton.setAlpha(.5f);
                        mFindBubblesButton.setClickable(false);
                    }
                });

                pdfPageAsBitmap = pdfPageToBitmap(file, pageIdx);
                Log.d("THREAD_TEST", "endpdfPageAsBitmapThread" + pdfPageAsBitmap);

                Log.d("THREAD_TEST", "startGenerateSpeechBubblesThread");
                Page page2 = new Page(pdfPageAsBitmap);
                List<Rectangle> speechBubbles;
                List<Rectangle> rejectedBubbles;
                List<List<Rectangle>> allBubbles;
                allBubbles = page2.generate_speech_bubbles(0.5, 0.5,
                        bubblesDetector, 0.2,
                        true, bubblesClassifier);
                speechBubbles = allBubbles.get(0);
                rejectedBubbles = allBubbles.get(1);
                page2.setSpeech_bubbles(speechBubbles);
                page2.setRejected_speech_bubbles(rejectedBubbles);
                Log.d("THREAD_TEST", "endGenerateSpeechBubblesThread" + speechBubbles.toArray().length + speechBubbles);


                Log.d("THREAD_TEST", "startChangingXYToReal");
                float onePageWidth = pdfView.getPageSize(pageIdx).getWidth();
                float pageDetectorWidth = page2.getOrig_image().cols();
                float proportionMapping = pageDetectorWidth / onePageWidth;

                List<Rectangle> speechBubblesRealXY = new ArrayList<>();
                String pathTesseract = getPathTess("eng.traineddata", getContext());
                TessBaseAPI tess = new TessBaseAPI();

                if (!tess.init(pathTesseract, "eng")) {
                    Log.d("TESTTESSERACT", "nie dziala");
                    // Error initializing Tesseract (wrong data path or language)
                    tess.recycle();
                }



                for (Rectangle bubble : speechBubbles) {
                    Bitmap bitmap = getTappedRectangleAsBitmap(bubble, page2);


                    tess.setImage(bitmap);
                    String text = tess.getUTF8Text();
                    String text2 = WordCheck.removeSingleChars(text);

                    Translator translator = new Translator();
                    WordCheck w2 = new WordCheck();
                    String tlum = "";
                    try {
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        translator.run(text2, w2, countDownLatch);
                        countDownLatch.await();
                        tlum = w2.getTest();
                        Log.d("tlum", tlum);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (Objects.equals(tlum, "")) {continue;}
                    Rectangle rectangle_new = new Rectangle(
                            (int) (bubble.getStartX() / proportionMapping),
                            (int) (bubble.getStartY() / proportionMapping),
                            (int) (bubble.getEndX() / proportionMapping),
                            (int) (bubble.getEndY() / proportionMapping));
                    rectangle_new.setText(tlum);

                    speechBubblesRealXY.add(rectangle_new);
                }

                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("THREAD_TEST", "startUIThread");
                        //Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                        mFindBubblesButton.setEnabled(true);
                        mFindBubblesButton.setAlpha(1f);
                        mFindBubblesButton.setClickable(true);
                        page.setOrig_image(page2.getOrig_image());
                        page.setSpeech_bubbles(speechBubbles);
                        page.setRejected_speech_bubbles(rejectedBubbles);
                        translatedPages.put(pageIdx, speechBubblesRealXY);
                        Log.d("THREAD_TEST", "endUIThread");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
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
                int width = (int) Math.floor(Math.sqrt(max_px * wxhProportion));
                int height = (int) (width / wxhProportion);
                Log.d("height and width", width + " x " + height);
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
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            Log.d("testPlikuTesss", context.getFilesDir() + "/tessdata");
            //TODO: sprawdzi czy mkdir jest konieczny, moze samo outFile wystarczy a jak nie, to dodac ifa na sprawdzanie czy ten folder istnieje
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
        return "";
    }

    private Context getContext() {
        return this;
    }


    private class OnDrawListener implements com.github.barteksc.pdfviewer.listener.OnDrawListener {
        @Override
        public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
            Paint paintBG = new Paint();
            paintBG.setColor(Color.WHITE);
            paintBG.setStrokeWidth(3);

            Log.d("Rysowanie", String.valueOf(translatedPages.keySet()));
            if(translatedPages.containsKey(pdfView.getCurrentPage())) {
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.BLACK);
                for (Rectangle bubble : translatedPages.get(pdfView.getCurrentPage())) {
                    Log.d("Bubble", String.valueOf(bubble));
                    int startX = (int) (bubble.getStartX() * pdfView.getZoom());
                    int startY = (int) (bubble.getStartY() * pdfView.getZoom());
                    int endX = (int) (bubble.getEndX() * pdfView.getZoom());
                    int endY = (int) (bubble.getEndY() * pdfView.getZoom());
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

//            int startX = (int) (236 * pdfView.getZoom());
//            int startY = (int) (282 * pdfView.getZoom());
//            int endX = (int) (546 * pdfView.getZoom());
//            int endY = (int) (487 * pdfView.getZoom());
//            Rectangle example_rectangle = new Rectangle(startX, startY, endX, endY);
//            example_rectangle.setText("OH, HEY... HOW'S YOUR MOM? XDDD XD XDDD XDDDDD XD");
//            //rectangle.updateOptSize()
//            canvas.drawRect(startX, startY, endX, endY, paintBG);
//
//            RectF rect = new RectF(startX, startY, endX, endY);
//            TextPaint textPaint = new TextPaint();
//            float fontSize = example_rectangle.getOptTextSize();
//            textPaint.setColor(Color.BLACK);
//            textPaint.setTextSize(fontSize);
//            StaticLayout sl = new StaticLayout(example_rectangle.getText(), textPaint,
//                    example_rectangle.getWidth(), Layout.Alignment.ALIGN_CENTER,
//                    1, 1, false);
//            Log.d("TextHeight", String.valueOf(sl.getHeight()));
//            //canvas.save();
//            canvas.translate(rect.left, rect.top);
//            sl.draw(canvas);
//            //canvas.restore();

            // Use Color.parseColor to define HTML colors
        }
    }

    public class OnPageChangeListener implements com.github.barteksc.pdfviewer.listener.OnPageChangeListener {
        @Override
        public void onPageChanged(int page, int pageCount) {
            if (page == 0) {
                mPageLeftButton.setEnabled(false);
                mPageLeftButton.setAlpha(.5f);
                mPageLeftButton.setClickable(false);
            } else {
                mPageLeftButton.setEnabled(true);
                mPageLeftButton.setAlpha(1f);
                mPageLeftButton.setClickable(true);
            }
            if (page == (pageCount - 1)) {
                mPageRightButton.setEnabled(false);
                mPageRightButton.setAlpha(.5f);
                mPageRightButton.setClickable(false);
            } else {
                mPageRightButton.setEnabled(true);
                mPageRightButton.setAlpha(1f);
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

        Log.d("zapis", "proba zapisu");
        try (FileOutputStream out = new FileOutputStream("a")) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("zapis", "udany");

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
            float onePageWidth = pdfView.getPageSize(0).getWidth();
            //mozliwe ze height jest niepotrzebne bo i tak mappedY nie jest zalezne od strony
            float onePageHeight = pdfView.getPageSize(0).getHeight();
            Log.d("ONTAPTEST", "getPageSize = " + pdfView.getPageSize(0));
            Log.d("ONTAPTEST", "onePageWidth = " + onePageWidth + " | onePageHeight = " + onePageHeight);


            float thisPageX = 0f;
            float thisPageXRealScale = 0f;
            if (pageIdx != 0) {
                thisPageX = mappedX - cumXOffset.get(pageIdx-1) * pdfView.getZoom();
            } else {
                thisPageX = mappedX;
            }
            thisPageXRealScale = thisPageX / pdfView.getZoom();
            float thisPageYRealScale = mappedY / pdfView.getZoom();

            Log.d("ONTAPTEST", "thisPageX = " + thisPageX + " | mappedY = " + mappedY);
            Log.d("ONTAPTEST", "thisPageXRealScale = " + thisPageXRealScale + " | thisPageYRealScale = " + thisPageYRealScale);

            try {
                float pageDetectorHeight = page.getOrig_image().rows();
                float pageDetectorWidth = page.getOrig_image().cols();
                Log.d("TESTWYKRYCIA", "pageDetectorHeight = " + pageDetectorHeight + " | pageDetectorWidth = " + pageDetectorWidth);

                float proportionDetector = pageDetectorWidth / pageDetectorHeight;
                float proportion = onePageWidth / onePageHeight;
                Log.d("TESTWYKRYCIA", "proportionDetector = " + proportionDetector + " | proportion = " + proportion);

                float proportionMapping = pageDetectorHeight / onePageHeight;
                Log.d("TESTWYKRYCIA", "proportionMapping = " + proportionMapping);

                Rectangle foundBubble = tappedRectangle(page.getSpeech_bubbles(), thisPageXRealScale * proportionMapping, thisPageYRealScale * proportionMapping);
                /*foundBubble.setStartX((int) (foundBubble.getStartX() / proportionMapping));
                foundBubble.setStartY((int) (foundBubble.getStartY() / proportionMapping));
                foundBubble.setEndX((int) (foundBubble.getEndX() / proportionMapping));
                foundBubble.setEndY((int) (foundBubble.getEndY() / proportionMapping));*/
                Log.d("TESTWYKRYCIA", String.valueOf(foundBubble));
            } catch (Exception exception) {
                Log.d("TESTWYKRYCIA", String.valueOf(exception));
            }

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
}



