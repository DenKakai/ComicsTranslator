package com.example.pdfreader2;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.MatOfByte;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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