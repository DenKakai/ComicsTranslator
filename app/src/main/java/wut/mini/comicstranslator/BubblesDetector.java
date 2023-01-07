package wut.mini.comicstranslator;

import org.opencv.core.MatOfByte;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

public class BubblesDetector {
    private Net net;

    public BubblesDetector(String east_path) {
        this.net = Dnn.readNetFromTensorflow(east_path);
    }

    public BubblesDetector(MatOfByte east) {
        this.net = Dnn.readNetFromTensorflow(east);
    }

    public Net getNet() {
        return net;
    }
}