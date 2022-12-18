package com.example.pdfreader2;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.ANN_MLP;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Page {
    private Mat orig_image;
    private List<Rectangle> speech_bubbles;
    private List<Rectangle> rejected_speech_bubbles;

    public Page(Mat orig_image) {
        this.orig_image = orig_image;
    }

    public Page(String orig_image_path) {
        this.orig_image = Imgcodecs.imread(orig_image_path, Imgcodecs.IMREAD_ANYCOLOR);
    }

    public void setSpeech_bubbles(List<Rectangle> speech_bubbles) {
        this.speech_bubbles = speech_bubbles;
    }

    public List<Rectangle> getSpeech_bubbles() {
        return speech_bubbles;
    }

    public Page(Bitmap bmp) {
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
        //convertTo changes the depth/per_pixel_type, not the channel count.

        this.orig_image = mat;
    }

    public Page() {}

    public Mat getOrig_image() {
        return orig_image;
    }

    public void setOrig_image(Mat orig_image) {
        this.orig_image = orig_image;
    }

    public void setOrig_image(Bitmap bmp) {
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
        //convertTo changes the depth/per_pixel_type, not the channel count.

        this.orig_image = mat;
    }

    public List<Rectangle> getRejected_speech_bubbles() {
        return rejected_speech_bubbles;
    }

    public void setRejected_speech_bubbles(List<Rectangle> rejected_speech_bubbles) {
        this.rejected_speech_bubbles = rejected_speech_bubbles;
    }


    public List<List<Rectangle>> generate_speech_bubbles(double min_confidence, double scale,
                                        BubblesDetector bubblesDetector) {
        return generate_speech_bubbles(min_confidence, scale, bubblesDetector, 0.2);
    }

    public List<List<Rectangle>> generate_speech_bubbles(double min_confidence, double scale,
                                        BubblesDetector bubblesDetector, double percentage_bigger_frames) {
        int W = this.orig_image.cols();
        int H = this.orig_image.rows();
        int bigger_frame_Wpx = (int)(percentage_bigger_frames / 100 * W);
        int bigger_frame_Hpx = (int)(percentage_bigger_frames / 100 * H);
        return generate_speech_bubbles(min_confidence, scale, bubblesDetector,
                bigger_frame_Wpx, bigger_frame_Hpx, false, null);
    }

    public List<List<Rectangle>> generate_speech_bubbles(double min_confidence, double scale,
                                                   BubblesDetector bubblesDetector,
                                                   double percentage_bigger_frames,
                                                   boolean use_classifier,
                                                   BubblesClassifier bubblesClassifier) {
        int W = this.orig_image.cols();
        int H = this.orig_image.rows();
        int bigger_frame_Wpx = (int)(percentage_bigger_frames / 100 * W);
        int bigger_frame_Hpx = (int)(percentage_bigger_frames / 100 * H);
        return generate_speech_bubbles(min_confidence, scale, bubblesDetector,
                bigger_frame_Wpx, bigger_frame_Hpx, use_classifier, bubblesClassifier);
    }


    public List<List<Rectangle>> generate_speech_bubbles(double min_confidence, double scale,
                                                   BubblesDetector bubblesDetector,
                                                   int bigger_frame_Wpx, int bigger_frame_Hpx,
                                                   boolean use_classifier,
                                                   BubblesClassifier bubblesClassifier) {
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
        Log.d("wymiary", "po skali:" + newW + " x " + newH);
        Imgproc.resize(image, image, sz);
        H = image.rows();
        Size siz = new Size(image.cols(), H);
        Net net = bubblesDetector.getNet();
        Mat blob = Dnn.blobFromImage(image, 1.0, siz,
                new Scalar(123.68, 116.78, 103.94), true, false);

        net.setInput(blob);
        List<Mat> outs = new ArrayList<>(2);
        List<String> layerNames = new ArrayList<>();
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
            rect.setStartX(Math.max((int)(rect.getStartX() * rW), 0));
            rect.setEndX(Math.min((int)(rect.getEndX() * rW), this.orig_image.cols()));
            rect.setStartY(Math.min((int)(rect.getStartY() * rH), this.orig_image.rows()));
            rect.setEndY(Math.max((int)(rect.getEndY() * rH), 0));
        }
        return makeBiggerRectangles(rects, bigger_frame_Wpx, bigger_frame_Hpx,
                use_classifier, bubblesClassifier);
    }

    public void save_with_speech_bubbles(String output_path) {
        save_with_speech_bubbles(output_path, 3);
    }

    public List<List<Rectangle>> remove_classified_as_not_bubbles(BubblesClassifier bubblesClassifier,
                                                            List<Rectangle> bubbles_to_classify) {
        // making copy of bubbles_to_classify
        List<Rectangle> bubbles_copy = new ArrayList<>();
        for (Rectangle rectangle: bubbles_to_classify) {
            bubbles_copy.add(new Rectangle(rectangle.getStartX(),
                    rectangle.getStartY(), rectangle.getEndX(), rectangle.getEndY()));
        }
        ANN_MLP ANN = bubblesClassifier.getANN();
        Mat image = new Mat();
        List<Rectangle> not_bubbles = new ArrayList<>();
        this.orig_image.copyTo(image);
        Log.d("wymiary", "w klasyfikatorze:" + image.cols() + " x " + image.rows());
        Mat datasetHist = new Mat();

        for (int i = 0; i < bubbles_copy.size(); i ++) {
            Rectangle speech_bubble = bubbles_copy.get(i);
            Mat croppedMat = image.submat(speech_bubble.getStartY(), speech_bubble.getEndY(),
                    speech_bubble.getStartX(), speech_bubble.getEndX());

            // place for setting background color

            Log.d("kolory_tla", "start");

            Map<String, Integer> colors;
            colors = new HashMap<>();
            int count_white = 0;
            for (int j = 0; j < croppedMat.rows(); j=j+10) { // height
                for (int k = 0; k < croppedMat.cols(); k=k+10) {
                    int count;
                    int red = (int) croppedMat.get(j, k)[0];
                    int green = (int) croppedMat.get(j, k)[1];
                    int blue = (int) croppedMat.get(j, k)[2];
                    if (red < 25 & green < 25 & blue < 25) { continue; }
                    if (red > 220 & green > 220 & blue > 220) {
                        count_white++;
                    }
                    String key = red + "," + green + "," + blue;
                    if (colors.containsKey(key)) {
                        count = colors.get(key);
                        colors.put(key, count + 1);
                    } else {
                        colors.put(key, 1);
                    }
                }
            }
            double all_not_black_px = colors.values().stream().mapToInt(x -> x).sum();
            List<Integer> color = new ArrayList<>();
            if ((double) count_white / all_not_black_px > 0.3) {
                for (int a=0; a < 3; a++) {
                    color.add(255);
                }
                speech_bubble.setBackground_color(color);
            } else {
                int avg_red = 0;
                int avg_green = 0;
                int avg_blue = 0;
                for (Map.Entry<String, Integer> entry : colors.entrySet()) {
                    String[] color_values = entry.getKey().split(",");
                    int num_of_px = entry.getValue();
                    avg_red += Integer.parseInt(color_values[0]) * num_of_px;
                    avg_green += Integer.parseInt(color_values[1]) * num_of_px;
                    avg_blue += Integer.parseInt(color_values[2]) * num_of_px;
                }
                avg_red = (int) ((double) avg_red / all_not_black_px);
                avg_green = (int) ((double) avg_green / all_not_black_px);
                avg_blue = (int) ((double) avg_blue / all_not_black_px);
                color.add(avg_red);
                color.add(avg_green);
                color.add(avg_blue);
                speech_bubble.setBackground_color(color);
            }

            Log.d("kolory_tla", "koniec");
            //

            Mat imgHSV = new Mat();
            Imgproc.cvtColor(croppedMat, imgHSV, Imgproc.COLOR_BGR2HSV);
            MatOfInt selectedChannels = new MatOfInt(0);
            Mat imgHist = new Mat();
            MatOfInt histSize = new MatOfInt(180);
            MatOfFloat ranges = new MatOfFloat(0f, 180f);
            Imgproc.calcHist(Collections.singletonList(imgHSV), selectedChannels, new Mat(),
                    imgHist, histSize, ranges);
            imgHist = imgHist.t();
            datasetHist.push_back(imgHist);
        }

        datasetHist.convertTo(datasetHist, CvType.CV_32F);
        for (int i = 0; i < datasetHist.rows(); i++) {
            Mat sample = datasetHist.row(i);
            Mat results = new Mat();
            ANN.predict(sample, results, 0);

            double response = results.get(0, 0)[0];
            int predicted_label = (int) Math.round(response);
            if (predicted_label == 1) {
                not_bubbles.add(bubbles_copy.get(i));
            }
        }
        bubbles_copy.removeAll(not_bubbles);
        List<List<Rectangle>> all_rectangles = new ArrayList<>();
        all_rectangles.add(bubbles_copy);
        all_rectangles.add(not_bubbles);
        return all_rectangles;
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

    public Bitmap image_with_speech_bubbles(int thickness) {
        Mat image = new Mat();
        this.orig_image.copyTo(image);

        for (Rectangle rect : this.speech_bubbles) {
            Point point1 = new Point(rect.getStartX(), rect.getStartY());
            Point point2 = new Point(rect.getEndX(), rect.getEndY());
            Scalar color = new Scalar(0, 255, 0);
            Imgproc.rectangle(image, point1, point2, color, thickness);
        }
        //
        final Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        //

        return bitmap;
    }

    private List<List<Rectangle>> makeBiggerRectangles(List<Rectangle> rectangles,
                                                 int Wpx_more, int Hpx_more,
                                                 boolean use_classifier,
                                                 BubblesClassifier bubblesClassifier) {

        List<List<Rectangle>> rectangles_result = new ArrayList<>();
        if (rectangles.size() == 0) {
            rectangles_result.add(rectangles);
            rectangles_result.add(new ArrayList<>());
            return rectangles_result;
        }
        int num_of_beginning_rects = rectangles.toArray().length;
        List<Integer> rectangle_cluster = IntStream.range(0, num_of_beginning_rects)
                .boxed().collect(Collectors.toList());
        for (int i = 0; i < num_of_beginning_rects; i++) {
            for (int j = i+1; j < num_of_beginning_rects; j++) {
                Rectangle p1 = rectangles.get(i);
                Rectangle p2 = rectangles.get(j);
                int w = this.orig_image.cols();
                int h = this.orig_image.rows();
                if (p1.isOverlappingOrNear(p2, w/400, h/400)) {
                    int min_cluster = Math.min(rectangle_cluster.get(i), rectangle_cluster.get(j));
                    int max_cluster = Math.max(rectangle_cluster.get(i), rectangle_cluster.get(j));
                    if (min_cluster == max_cluster) {
                        continue;
                    }
                    if (rectangle_cluster.get(i) == min_cluster) {
                        rectangle_cluster.set(j, min_cluster);
                    }
                    else {
                        rectangle_cluster.set(i, min_cluster);
                    }
                    for (int k = 1; k < num_of_beginning_rects; k++) {
                        if (rectangle_cluster.get(k) == max_cluster) {
                            rectangle_cluster.set(k, min_cluster);
                        }
                    }
                }
            }
        }

        // making copy of rectangles
        List<Rectangle> rectangles_copy = new ArrayList<>();
        for (Rectangle rectangle: rectangles) {
            rectangles_copy.add(new Rectangle(rectangle.getStartX(),
                    rectangle.getStartY(), rectangle.getEndX(), rectangle.getEndY()));
        }

        List<Rectangle> bigger_rectangles = new ArrayList<>();

        for (int i = 0; i < num_of_beginning_rects; i++) {
            List<Rectangle> rects_in_cluster_i = new ArrayList<>();
            for (int j = 0; j < num_of_beginning_rects; j++) {
                if (rectangle_cluster.get(j) == i) {
                    rects_in_cluster_i.add(rectangles_copy.get(j));
                }
            }
            if (rects_in_cluster_i.toArray().length == 0) {
                continue;
            }
            Rectangle big_rect = rects_in_cluster_i.get(0);
            rects_in_cluster_i.remove(0);
            for (Rectangle rect : rects_in_cluster_i) {
                big_rect = big_rect.makeBiggerRectangle(rect);
            }
            bigger_rectangles.add(big_rect);
        }

        //deleting rectangles if they are inside another rectangle

        List<Rectangle> rectangles_to_del = new ArrayList<>();

        for (int i = 0; i < bigger_rectangles.size(); i++) {
            for (int j = i+1; j < bigger_rectangles.size(); j++) {
                Rectangle rectangle1 = bigger_rectangles.get(i);
                Rectangle rectangle2 = bigger_rectangles.get(j);
                if (rectangle1.isInside(rectangle2)) {
                    rectangles_to_del.add(rectangle1);
                } else if (rectangle2.isInside(rectangle1)) {
                    rectangles_to_del.add(rectangle2);
                }
            }
        }

        bigger_rectangles.removeAll(rectangles_to_del);

        // making frames a little bit bigger

        for (Rectangle rect : bigger_rectangles) {
            rect.setStartX(rect.getStartX() - Wpx_more);
            rect.setStartY(rect.getStartY() - Hpx_more);
            rect.setEndX(rect.getEndX() + Wpx_more);
            rect.setEndY(rect.getEndY() + Hpx_more);
        }


        //making smaller rectangles if they are stick out of image

        for (int i = 0; i < bigger_rectangles.size(); i++) {
            if (bigger_rectangles.get(i).getStartX() < 0) {
                bigger_rectangles.get(i).setStartX(0);
            } if (bigger_rectangles.get(i).getStartY() > this.orig_image.rows()) {
                bigger_rectangles.get(i).setStartY(this.orig_image.rows());
            } if (bigger_rectangles.get(i).getEndX() > this.orig_image.cols()) {
                bigger_rectangles.get(i).setEndX(this.orig_image.cols());
            } if (bigger_rectangles.get(i).getEndY() < 0) {
                bigger_rectangles.get(i).setEndY(0);
            }
        }

        if (use_classifier) {
            List<List<Rectangle>> all_rectangles = this.remove_classified_as_not_bubbles(
                    bubblesClassifier, bigger_rectangles
            );
            rectangles_result.add(all_rectangles.get(0));
            rectangles_result.add(all_rectangles.get(1));
        } else {
            rectangles_result.add(bigger_rectangles);
            rectangles_result.add(new ArrayList<>());
        }


        return rectangles_result;
    }

}
