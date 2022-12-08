package com.example.pdfreader2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.Process;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.Callbacks;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.util.SizeF;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

//TODO: dodalem nowa klase, ktora jest identyczna jak PdfFile, tylko zmienilem package. Trzeba sprawdzic czy to dziala.
import com.github.barteksc.pdfviewer.PDFView;

public class PdfViewerActivity extends AppCompatActivity{

    private PDFView pdfView;
    private Button mZoomInButton;
    private Button mZoomOutButton;
    private Button mPageLeftButton;
    private Button mPageRightButton;
    private Button mFindBubblesButton;

    //TODO: do wywalenia
    private Button mLogBubblesButton;

    private Page page;
    private ArrayList<Bitmap> pdfAsListOfBitmaps;
    private Handler mainHandler = new Handler();

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
                .onPageScroll(new OnPageScrollListener())
                .fitEachPage(true)
                .pageFitPolicy(FitPolicy.HEIGHT)
                .swipeHorizontal(true)
                .pageSnap(true)
                .autoSpacing(true)
                .pageFling(true)
                .load();

        pdfView.setMidZoom(2f);


        /*pdfAsListOfBitmaps = pdfToBitmap(file);

        Bitmap bmp = pdfAsListOfBitmaps.get(1);
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
        //convertTo changes the depth/per_pixel_type, not the channel count.

        Log.d("TEST_BITMAPY", "to dobre" + String.valueOf(mat));

        Page page = new Page(mat);

        String pathEast = getPath("frozen_east_text_detection.pb", this);
        Log.d("TEST_BITMAPY", String.valueOf(pathEast));

        BubblesDetector bubblesDetector = new BubblesDetector(pathEast);
        Log.d("TEST_BITMAPY", String.valueOf(bubblesDetector));

        page.generate_speech_bubbles(0.5, 0.05, bubblesDetector);
        Log.d("TEST_BITMAPY", String.valueOf(page.getSpeech_bubbles()));**/


        //TODO: Do kazdego guzika zrobic wyszarzanie, kiedy ma nie byc mozliwa akcja
        //TODO: Naprawic zoomout by na np. 172% zooma spadlo do 100%


        //przyblizenie
        mZoomInButton = (Button) findViewById(R.id.zoomIn);
        mZoomInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int curPage = pdfView.getCurrentPage();

                pdfView.zoomTo(pdfView.getZoom() + 1f);
                pdfView.jumpTo(curPage);
                pdfView.loadPages();
            }
        });


        //oddalenie
        mZoomOutButton = (Button) findViewById(R.id.zoomOut);
        mZoomOutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int curPage = pdfView.getCurrentPage();
                float curZoom = pdfView.getZoom();

                if (curZoom >= 2f) {
                    pdfView.zoomTo(curZoom - 1f);
                    pdfView.jumpTo(curPage);
                    pdfView.loadPages();
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

        mFindBubblesButton = (Button) findViewById(R.id.findBubbles);
        mFindBubblesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //mFindBubblesButton.setEnabled(false);
                page = new Page();
                int DDpi = getResources().getDisplayMetrics().densityDpi;
                ExampleRunnable runnable = new ExampleRunnable(DDpi, file, pdfView.getCurrentPage(), page, bubblesDetector);
                new Thread(runnable).start();
            }
        });

        //log dymkow

        mLogBubblesButton = (Button) findViewById(R.id.logBubbles);
        mLogBubblesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d("TESTWYKRYCIA", String.valueOf(page.getSpeech_bubbles()));
            }
        });
    }

    //zmiana pdfa na liste bitmap
    private ArrayList<Bitmap> pdfToBitmap(File pdfFile) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();

        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

            Bitmap bitmap;
            final int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                bitmaps.add(bitmap);

                // close the page
                page.close();

            }

            // close the renderer
            renderer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return bitmaps;

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

        Bitmap pdfPageAsBitmap;

        ExampleRunnable(int DDpi, File file, int pageIdx, Page page, BubblesDetector bubblesDetector) {
            this.DDpi = DDpi;
            this.file = file;
            this.pageIdx = pageIdx;
            this.page = page;
            this.bubblesDetector = bubblesDetector;
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
                speechBubbles = page2.generate_speech_bubbles(0.5, 0.35, bubblesDetector);
                page2.setSpeech_bubbles(speechBubbles);
                Log.d("THREAD_TEST", "endGenerateSpeechBubblesThread" + speechBubbles.toArray().length + speechBubbles);

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
                        Log.d("THREAD_TEST", "endUIThread");
                    }
                });

                //Page page2 = new Page(pdfPageAsBitmap);
                //page2.setSpeech_bubbles(speechBubbles);

                Bitmap bitmap = page2.image_with_speech_bubbles(3);
                //TODO: wywalic to kiedys, teraz jest do testu tylko
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "testowyObrazekRectangle8", "lolxd");
                Log.d("THREAD_TEST", "endThread");
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

                //TODO: zakomentowane, bo testujemy
                //int width = DDpi / 72 * page.getWidth();
                //int height = DDpi / 72 * page.getHeight();

                int width = 3740;
                int height = 4895;

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

    private Rectangle tappedRectangle(List<Rectangle> rectangles, float x, float y) {
        Rectangle resultRectangle = null;

        for (Rectangle rectangle : rectangles) {
            if (rectangle.getStartX() <= x && x <= rectangle.getEndX() && rectangle.getStartY() <= y && y <= rectangle.getEndY()) {
                resultRectangle = rectangle;
                break;
            }
        }

        return resultRectangle;
    }

    private class OnPageScrollListener implements com.github.barteksc.pdfviewer.listener.OnPageScrollListener {
        @Override
        public void onPageScrolled(int page, float positionOffset) {
            Log.d("Scroll", "Lis jest mega fajny serio");
        }
    }

    private class OnDrawListener implements com.github.barteksc.pdfviewer.listener.OnDrawListener {
        @Override
        public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
            int x=400;
            int y=400;
            int radius=40;
            Paint paint=new Paint();
            // Use Color.parseColor to define HTML colors
            paint.setColor(Color.parseColor("#CD5C5C"));
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private class onTapListener implements OnTapListener {
        @Override
        public boolean onTap(MotionEvent e) {

            //x, y = piksel EKRANU TELEFONU
            //mappedX, mappedY = piksel CAŁEJ WSTĘGI PDFA, jezeli jest przyblizony, to wtedy przyblizonego
            //thisPageX, <mappedY> = piksel AKTUALNEJ STRONY, jezeli jest przyblizona, to wtedy przyblizonej
            //thisPageXRealScale, thisPageYRealScale = piksel AKTUALNEJ STRONY, jezeli jest przyblizona, to i tak zwraca normalne wymiary

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
                //TODO: teraz uznaje, ze kazda strona ma ten sam width, zrobic liste z suma kulumowana i brac z tego zamaist page * onePageWidth
                thisPageX = mappedX - pageIdx * onePageWidth * pdfView.getZoom();
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

            //Log.d("ONTAPTEST", "Page[0] = " + pdfView.getPageSize(0) + " | Page[1] = " + pdfView.getPageSize(1) + " | Page[2] = " + pdfView.getPageSize(2));
            return false;
        }
    }
}



