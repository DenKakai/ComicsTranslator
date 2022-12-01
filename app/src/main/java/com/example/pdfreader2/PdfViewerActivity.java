package com.example.pdfreader2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private Button mZoomInButton;
    private Button mZoomOutButton;
    private Button mPageLeftButton;
    private Button mPageRightButton;
    private Button mFindBubblesButton;
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
                .fitEachPage(true)
                .pageFitPolicy(FitPolicy.HEIGHT)
                .swipeHorizontal(true)
                .pageSnap(true)
                .autoSpacing(true)
                .pageFling(true)
                .load();

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


        //przyblizenie
        //TODO: sprawdzic, czy mozna po prostu zmienic w obiekcie PDFView DEFAULT_MID_SCALE na 2f
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
            Log.d("THREAD_TEST", "startThread");
            try {
                Handler threadHandler = new Handler(Looper.getMainLooper());
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFindBubblesButton.setEnabled(false);
                        mFindBubblesButton.setAlpha(.5f);
                        mFindBubblesButton.setClickable(false);
                    }
                });

                pdfPageAsBitmap = pdfPageToBitmap(file, pageIdx);
                Log.d("THREAD_TEST", "endpdfPageAsBitmapThread" + pdfPageAsBitmap);

                Log.d("THREAD_TEST", "startGenerateSpeechBubblesThread");
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        page = new Page(pdfPageAsBitmap);
                        page.generate_speech_bubbles(0.5, 0.5, bubblesDetector);
                        Log.d("THREAD_TEST", "endGenerateSpeechBubblesThread" + page.getSpeech_bubbles().toArray().length + page.getSpeech_bubbles());
                        mFindBubblesButton.setEnabled(true);
                        mFindBubblesButton.setAlpha(1f);
                        mFindBubblesButton.setClickable(true);
                    }
                });
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

                int width = DDpi / 72 * page.getWidth();
                int height = DDpi / 72 * page.getHeight();
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

}



