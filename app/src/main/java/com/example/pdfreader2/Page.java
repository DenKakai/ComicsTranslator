package com.example.pdfreader2;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Page {
    private Mat orig_image;
    private List<Rectangle> speech_bubbles;

    public Page(Mat orig_image) {
        this.orig_image = orig_image;
    }

    public Page(String orig_image_path) {
        this.orig_image = Imgcodecs.imread(orig_image_path, Imgcodecs.IMREAD_ANYCOLOR);
    }

    public Page(Bitmap bmp) {
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
        //convertTo changes the depth/per_pixel_type, not the channel count.

        this.orig_image = mat;
    }

    public Page() {

    }

    public Mat getOrig_image() {
        return orig_image;
    }

    public List<Rectangle> getSpeech_bubbles() {
        return speech_bubbles;
    }

    public void generate_speech_bubbles(double min_confidence, double scale,
                                        BubblesDetector bubblesDetector) {
        generate_speech_bubbles(min_confidence, scale, bubblesDetector, 0.2);
    }

    public void generate_speech_bubbles(double min_confidence, double scale,
                                        BubblesDetector bubblesDetector, double percentage_bigger_frames) {
        int W = this.orig_image.cols();
        int H = this.orig_image.rows();
        int bigger_frame_Wpx = (int)(percentage_bigger_frames / 100 * W);
        int bigger_frame_Hpx = (int)(percentage_bigger_frames / 100 * H);
        generate_speech_bubbles(min_confidence, scale, bubblesDetector,
                bigger_frame_Wpx, bigger_frame_Hpx);
    }


    //TODO: Jakas inna do tego robienia
    /**public BufferedImage Mat2BufferedImage(int bubble_idx)throws IOException {
        Mat image = new Mat();
        this.orig_image.copyTo(image);
        Rectangle speech_bubble = this.getSpeech_bubbles().get(bubble_idx);
        Mat croppedMat = image.submat(speech_bubble.getStartY(), speech_bubble.getEndY(),
                speech_bubble.getStartX(), speech_bubble.getEndX());
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", croppedMat, mob);
        return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
    }*/

    public void generate_speech_bubbles(double min_confidence, double scale,
                                        BubblesDetector bubblesDetector, int bigger_frame_Wpx,
                                        int bigger_frame_Hpx) {
        Mat image = new Mat();
        this.orig_image.copyTo(image);
        int W = image.cols();
        int H = image.rows();
        int new_scale = (int)(32 / scale);
        int newW = W / new_scale * 32;
        int newH = H / new_scale * 32;
        double rW = W / (double) newW;
        double rH = H / (double) newH;
        Size sz = new Size(newW, newH);
        Imgproc.resize(image, image, sz);
        H = image.rows();
        Size siz = new Size(image.cols(), H);
        Net net = bubblesDetector.getNet();
        Mat blob = Dnn.blobFromImage(image, 1.0, siz,
                new Scalar(123.68, 116.78, 103.94), true, false);

        net.setInput(blob);
        List<Mat> outs = new ArrayList<>(2);
        List<String> layerNames = new ArrayList<String>();
        layerNames.add("feature_fusion/Conv_7/Sigmoid");
        layerNames.add("feature_fusion/concat_3");
        net.forward(outs, layerNames);
        Mat scores = outs.get(0);
        Mat geometry = outs.get(1);
        int numRows = scores.size(2);
        int numCols = scores.size(3);
        scores = scores.reshape(1, H / 4);
        geometry = geometry.reshape(1, 5 * H / 4);
        int g_width = geometry.cols();
        int g_height = geometry.rows() / 5;
        List<Rectangle> rects = new ArrayList<>();

        for(int y = 0; y < numRows; ++y) {
            Mat scoresData = scores.row(y);
            Mat x0Data = geometry.submat(0, g_height, 0, g_width).row(y);
            Mat x1Data = geometry.submat(g_height, 2 * g_height, 0, g_width).row(y);
            Mat x2Data = geometry.submat(2 * g_height, 3 * g_height, 0, g_width).row(y);
            Mat x3Data = geometry.submat(3 * g_height, 4 * g_height, 0, g_width).row(y);
            Mat anglesData = geometry.submat(4 * g_height, 5 * g_height, 0, g_width).row(y);

            for (int x = 0; x < numCols; ++x) {
                double score = scoresData.get(0, x)[0];
                if (score < min_confidence) {
                    continue;
                }
                double offsetX = x * 4.0;
                double offsetY = y * 4.0;
                double angle = anglesData.get(0, x)[0];
                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);
                double x0 = x0Data.get(0, x)[0];
                double x1 = x1Data.get(0, x)[0];
                double x2 = x2Data.get(0, x)[0];
                double x3 = x3Data.get(0, x)[0];
                double h = x0 + x2;
                double w = x1 + x3;
                int endX = (int)(offsetX + cosA * x1 + sinA * x2);
                int endY = (int)(offsetY - sinA * x1 + cosA * x2);
                int startX = (int)(endX - w);
                int startY = (int)(endY - h);
                Rectangle rect = new Rectangle(startX, startY, endX, endY);
                rects.add(rect);
            }
        }
        for (Rectangle rect : rects) {
            rect.setStartX((int)(rect.getStartX() * rW));
            rect.setEndX((int)(rect.getEndX() * rW));
            rect.setStartY((int)(rect.getStartY() * rH));
            rect.setEndY((int)(rect.getEndY() * rH));
        }
        this.speech_bubbles = makeBiggerRectangles(rects, bigger_frame_Wpx, bigger_frame_Hpx);
    }

    public void reduce_speech_bubbles(double min_confidence, double scale,
                                      BubblesDetector bubblesDetector, double percentage_bigger_frames) {
        List<Rectangle> speech_bubbles_del = new ArrayList<>();
        for (Rectangle speech_bubble : this.speech_bubbles) {
            if (!check_if_text_in_bubble(speech_bubble, 0.6,
                    1, bubblesDetector)) {
                speech_bubbles_del.add(speech_bubble);
            };
        }
        this.speech_bubbles.removeAll(speech_bubbles_del);
    }
    private boolean check_if_text_in_bubble(Rectangle speech_bubble, double min_confidence,
                                            double scale, BubblesDetector bubblesDetector) {
        Mat image = new Mat();
        this.orig_image.copyTo(image);
        Mat croppedMat = image.submat(speech_bubble.getStartY(), speech_bubble.getEndY(),
                speech_bubble.getStartX(), speech_bubble.getEndX());
        int W = croppedMat.cols();
        int H = croppedMat.rows();
        int new_scale = (int)(32 / scale);
        int newW = Math.max(W / new_scale * 32, 32);
        int newH = Math.max(H / new_scale * 32, 32);
        Size sz = new Size(newW, newH);
        Imgproc.resize(croppedMat, croppedMat, sz);
        Net net = bubblesDetector.getNet();
        Mat blob = Dnn.blobFromImage(image, 1.0, new Size(croppedMat.cols(), croppedMat.rows()),
                new Scalar(123.68, 116.78, 103.94), true, false);

        net.setInput(blob);
        List<Mat> outs = new ArrayList<>(2);
        List<String> layerNames = new ArrayList<String>();
        layerNames.add("feature_fusion/Conv_7/Sigmoid");
        layerNames.add("feature_fusion/concat_3");
        net.forward(outs, layerNames);
        Mat scores = outs.get(0);
        int numRows = scores.size(2);
        int numCols = scores.size(3);
        scores = scores.reshape(1, croppedMat.rows() / 4);
        int j = 0;
        for(int y = 0; y < numRows; ++y) {
            Mat scoresData = scores.row(y);
            for (int x = 0; x < numCols; ++x) {
                double score = scoresData.get(0, x)[0];
                if (score >= min_confidence) {
                    j++;
                }
            }
        }
        return j > 0;
    }

    public void save_with_speech_bubbles(String output_path) {
        save_with_speech_bubbles(output_path, 3);
    }

    public void save_with_speech_bubbles(String output_path, int thickness) {
        Mat image = new Mat();
        this.orig_image.copyTo(image);

        for (Rectangle rect : this.speech_bubbles) {
            Point point1 = new Point(rect.getStartX(), rect.getStartY());
            Point point2 = new Point(rect.getEndX(), rect.getEndY());
            Scalar color = new Scalar(0, 255, 0);
            Imgproc.rectangle(image, point1, point2, color, thickness);
        }
        Imgcodecs.imwrite(output_path, image);
    }

    private static List<Rectangle> makeBiggerRectangles(List<Rectangle> rectangles,
                                                        int Wpx_more, int Hpx_more) {
        List<Rectangle> big_rects = new ArrayList<>();
        while (rectangles.size() >= 2) {
            Rectangle p1 = rectangles.get(0);
            boolean was_overlapped = false;
            for (Rectangle p2 : rectangles.subList(1, rectangles.size())) {
                if (
                        p1.isOverlappingOrNear(p2, 10)
                ) {
                    was_overlapped = true;
                    Rectangle p3 = p1.makeBiggerRectangle(p2);
                    rectangles.remove(0);
                    rectangles.remove(p2);
                    rectangles.add(p3);
                    break;
                }
            }
            if (!was_overlapped) {
                big_rects.add(p1);
                rectangles.remove(p1);
            }
        }
        if (rectangles.size() > 0) {
            big_rects.add(rectangles.get(0));
        }
        for (Rectangle rect : big_rects) {
            rect.setStartX(rect.getStartX() - Wpx_more);
            rect.setStartY(rect.getStartY() - Hpx_more);
            rect.setEndX(rect.getEndX() + Wpx_more);
            rect.setEndY(rect.getEndY() + Hpx_more);
        }
        return big_rects;
    }

}
