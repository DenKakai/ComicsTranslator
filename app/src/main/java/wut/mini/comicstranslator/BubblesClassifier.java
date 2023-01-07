package wut.mini.comicstranslator;

import org.opencv.ml.ANN_MLP;

public class BubblesClassifier {
    private ANN_MLP ANN;

    public BubblesClassifier(String ann_path) {
        this.ANN = ANN_MLP.load(ann_path);
    }

    public void setANN(ANN_MLP ANN) {
        this.ANN = ANN;
    }

    public ANN_MLP getANN() {
        return ANN;
    }
}
