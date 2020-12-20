/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.adarmawan117.recognition.sibi.tflite;


import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.adarmawan117.recognition.sibi.env.Logger;
import org.adarmawan117.recognition.sibi.env.StructuredLandmarks;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TFLiteObjectDetectionAPIModel implements Classifier {
    private static float[][] anchors = new float[2944][4];
    private static final Logger LOGGER = new Logger();

    // Only return this many results.
    private static final int NUM_DETECTIONS = 2;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[][][] outputReg = new float[1][2944][18];
    private float[][][] outputClf = new float[1][2944][1];

    /*
    variabel outputJoints = kordinat tangan dari objek yang sudah terdeteksi (campuran)
    x untuk genap
    y untuk ganjil
    */
    private float[][] outputJoints = new float[1][42];

    private ByteBuffer imgData;

    private ByteBuffer imgLandmarkData;

    private Interpreter tfLite;

    private Interpreter landmarkTfLite;

    private TFLiteObjectDetectionAPIModel() {
    }

    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize     The size of image input
     * @param isQuantized   Boolean representing model is quantized or not
     */
    public static Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized)
            throws IOException {
        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();

        InputStream labelsInput = null;
        labelsInput = assetManager.open(labelFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
            d.landmarkTfLite = new Interpreter(loadModelFile(assetManager, "hand_landmark.tflite"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.imgLandmarkData = ByteBuffer.allocateDirect(d.inputSize * d.inputSize * 3 * 4);
        d.imgLandmarkData.order(ByteOrder.nativeOrder());

        d.tfLite.setNumThreads(NUM_THREADS);
        d.landmarkTfLite.setNumThreads(NUM_THREADS);

        // read anchors.csv
        try (Scanner scanner = new Scanner(assetManager.open("anchors.csv"));) {
            int x = 0;
            while (scanner.hasNextLine()) {
                // records.add(getRecordFromLine());
                String[] cols = scanner.nextLine().split(",");
                anchors[x++] = new float[]{Float.parseFloat(cols[0]), Float.parseFloat(cols[1]), Float.parseFloat(cols[2]), Float.parseFloat(cols[3])};
            }
        }
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap, Bitmap oribmp) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // put the image in imgData
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputReg);
        outputMap.put(1, outputClf);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        // Show the best detections.
        // after scaling them back to the input size.
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        ArrayList<float[]> candidate_detect_array = new ArrayList<float[]>();
        ArrayList<float[]> candidate_anchors_array = new ArrayList<float[]>();

        float[] clf = new float[outputClf[0].length];

        int max_idx = 0;
        double max_suggestion = 0;
        int count = 0;
        int clf_max_idx = 0;

        // finding the best result of detecting hand
        for (int i = 0; i < outputClf[0].length; i++) {
            clf[i] = outputClf[0][i][0];

            float x = 1 / Double.valueOf(1 + Math.exp(-outputClf[0][i][0])).floatValue();

            if (x > 0.9f) {
                count++;
                candidate_detect_array.add(outputReg[0][i]);
                candidate_anchors_array.add(anchors[i]);
                if (x > max_suggestion) {
                    max_suggestion = x;
                    max_idx = candidate_detect_array.size() - 1;
                    clf_max_idx = i;
                }
            }
        }
        if (candidate_detect_array.size() == 0)
            return recognitions;

        float[][] candidate_detect = new float[candidate_detect_array.size()][18];
        float[][] candidate_anchors = new float[candidate_anchors_array.size()][4];

        for (int i = 0; i < candidate_anchors_array.size(); i++) {
            candidate_detect[i] = candidate_detect_array.get(i);
            candidate_anchors[i] = candidate_anchors_array.get(i);
        }

        // palm detection result
        float dx, dy, w, h, side, center_wo_offst_x, center_wo_offst_y;
        dx = candidate_detect[max_idx][0];
        dy = candidate_detect[max_idx][1];
        w = candidate_detect[max_idx][2];
        h = candidate_detect[max_idx][3];
        center_wo_offst_x = candidate_anchors[max_idx][0] * inputSize;
        center_wo_offst_y = candidate_anchors[max_idx][1] * inputSize;
        side = Math.max(w, h) * 1.5f;

        // mark 3 keypoints in palm (kp0, kp1, kp2)
        Point[] kp = new Point[]{
                new Point(candidate_detect[max_idx][4] + center_wo_offst_x, candidate_detect[max_idx][5] + center_wo_offst_y),
                new Point(candidate_detect[max_idx][6] + center_wo_offst_x, candidate_detect[max_idx][7] + center_wo_offst_y),
                new Point(candidate_detect[max_idx][8] + center_wo_offst_x, candidate_detect[max_idx][9] + center_wo_offst_y),
        };

        // calculate the source and triangle for img landmark
        float[][] source_raw = getTriangle((float) kp[0].x, (float) kp[0].y, (float) kp[2].x, (float) kp[2].y, side);
        float scale = Math.max(oribmp.getHeight() / 256f, oribmp.getWidth() / 256f);
        MatOfPoint2f source = new MatOfPoint2f(new Point(source_raw[0][0] * scale, source_raw[0][1] * scale),
                new Point(source_raw[1][0] * scale, source_raw[1][1] * scale),
                new Point(source_raw[2][0] * scale, source_raw[2][1] * scale));

        MatOfPoint2f targetTriangle = new MatOfPoint2f(new Point(128f, 128f),
                new Point(128f, 0),
                new Point(0, 128f));

        int keypoint_side = 5;

        //print 7 initial keypoints for palm
        for (int k = 4; k < candidate_detect[max_idx].length; k += 2) {
            float kp_x = candidate_detect[max_idx][k] + center_wo_offst_x;
            float kp_y = candidate_detect[max_idx][k + 1] + center_wo_offst_y;

            recognitions.add(new Recognition(1, "", (float) (1 / (1 + Math.exp(-outputClf[0][clf_max_idx][0]))), new RectF(
                    kp_x - keypoint_side,
                    kp_y - keypoint_side,
                    kp_x + keypoint_side,
                    kp_y + keypoint_side)));
        }

        // calculate the mtr matrix for cropthe hand landmark image
        Mat mtr = Imgproc.getAffineTransform(source, targetTriangle);
        // cal Mtr inverse for returning the point
        Mat mtrr = new Mat();
        mtrr.create(2, 3, CvType.CV_32FC1);
        mtr.convertTo(mtrr, CvType.CV_32FC1);
        // add row {0,0,0}
        // change (2,2) --> 1 && convert to float 32
        Mat row = new Mat(1, 3, CvType.CV_32FC1);
        row.put(0, 0, 0f);
        row.put(0, 1, 0f);
        row.put(0, 2, 1f);
        mtrr.push_back(row);

        // convert the openCV mtr Mat --> Android Matrix
        Matrix mMtr = new Matrix();
        transformMatrix(mtrr, mMtr);
        float[] range = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        mMtr.mapPoints(range);
        Matrix mRot = new Matrix();
        mRot.postRotate(90);
        mRot.postTranslate(oribmp.getHeight() + ((oribmp.getWidth() - oribmp.getHeight()) / 2f), 0);

        // make bitmap for original bitmap 1280*720 to 1280*1280 pad image
        // Bitmap origBitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DCIM+"/20200203_204820.jpg");
        Bitmap origBitmap = oribmp;
        Bitmap bitmapPad = Bitmap.createBitmap(1280, 1280, Bitmap.Config.ARGB_8888);
        Bitmap affineBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);

        final Canvas rotatedCanvas = new Canvas(bitmapPad);
        rotatedCanvas.drawBitmap(origBitmap, mRot, null);

        final Canvas applyAffineCanvas = new Canvas(affineBitmap);
        applyAffineCanvas.drawBitmap(bitmapPad, mMtr, null);

        // store imglandmark as byte buffer for tensorflow
        affineBitmap.getPixels(intValues, 0, 256, 0, 0, 256, 256);
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat(((((pixelValue >> 16) & 0xFF) / 255f) - .5f) * 2);
                    imgData.putFloat(((((pixelValue >> 8) & 0xFF) / 255f) - .5f) * 2);
                    imgData.putFloat((((pixelValue & 0xFF) / 255f) - .5f) * 2);
                }
            }
        }

        Object[] inputArray2 = {imgData};
        Map<Integer, Object> outputJointMap = new HashMap<>();
        outputJointMap.put(0, outputJoints);

        // Run the inference call.
        Trace.beginSection("run");
        landmarkTfLite.runForMultipleInputsOutputs(inputArray2, outputJointMap);
        Trace.endSection();

        String gesture = "";
        StructuredLandmarks[] landmarks = getStructuredLandmarks(outputJoints[0]);

        //
        if (outputJoints[0][4] < outputJoints[0][34]) { //
            gesture = punggungTanganGesture(landmarks);
        } else {
            gesture = telapakTanganGesture(landmarks);
        }

        recognitions.add(new Recognition(2, gesture, (float) (1 / (1 + Math.exp(-outputClf[0][clf_max_idx][0]))), new RectF(
                center_wo_offst_x - side / 2,
                center_wo_offst_y - side / 2,
                center_wo_offst_x + side / 2,
                center_wo_offst_y + side / 2)));

        Trace.endSection(); // "recognizeImage"
        return recognitions;
    }

    static void transformMatrix(Mat src, Matrix dst) {

        int columns = src.cols();
        int rows = src.rows();

        float[] values = new float[columns * rows];
        int index = 0;
        for (int x = 0; x < columns; x++)
            for (int y = 0; y < rows; y++) {
                double[] value = src.get(x, y);
                values[index] = (float) value[0];

                index++;
            }

        dst.setValues(values);
    }

    public static float[][] getTriangle(float kp0_x, float kp0_y, float kp2_x, float kp2_y, float dist) {
        // cal triangle with kp x-coordinates
        float[][] triangle = new float[3][2];
        float x = kp2_x - kp0_x;
        float y = kp2_y - kp0_y;

        //    cal matrix norm
        float[][] dir_v = new float[1][2];
        dir_v[0][0] = x / (float) (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
        dir_v[0][1] = y / (float) (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));

        float[][] rotate = new float[2][2]; //R90.T
        rotate[0][0] = 0f;
        rotate[0][1] = -1f;
        rotate[1][0] = 1f;
        rotate[1][1] = 0f;

        //    cal  dir_v @ rotate
        float[][] dir_v_r = new float[dir_v.length][rotate[0].length];
        for (int i = 0; i < dir_v.length; i++) {
            for (int j = 0; j < rotate[0].length; j++) {
                for (int k = 0; k < dir_v[0].length; k++) {
                    dir_v_r[i][j] += dir_v[i][k] * rotate[k][j];
                }
            }
        }
        // b = kp2 + dir_v * dist
        float[][] b = new float[1][2];
        b[0][0] = kp2_x + dir_v[0][0] * dist - ((kp0_x - kp2_x) * .2f);
        b[0][1] = kp2_y + dir_v[0][1] * dist - ((kp0_y - kp2_y) * .2f);

        // c = kp2 + dir_v_r * dist
        float[][] c = new float[1][2];
        c[0][0] = kp2_x + dir_v_r[0][0] * dist - ((kp0_x - kp2_x) * .2f);
        c[0][1] = kp2_y + dir_v_r[0][1] * dist - ((kp0_y - kp2_y) * .2f);

        triangle[0] = new float[]{kp2_x - ((kp0_x - kp2_x) * .2f), kp2_y - ((kp0_y - kp2_y) * .2f)};
        triangle[1] = b[0];
        triangle[2] = c[0];

        return triangle;
    }

    public void setNumThreads(int num_threads) {
        if (tfLite != null) setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) setUseNNAPI(isChecked);
    }

    public static String punggungTanganGesture(StructuredLandmarks[] landmarks) {
        // finger states
        boolean jempolTerbuka = false;        // jempol
        boolean telunjukTerbuka = false;
        boolean jariTengahTerbuka = false;
        boolean jariManisTerbuka = false;
        boolean kelingkingTerbuka = false;
        LOGGER.d("Punggung tangan joints = " + Arrays.toString(landmarks));

        double pseudoFixKeyPoint = landmarks[2].getX(); //compare x
        if (landmarks[3].getX() < pseudoFixKeyPoint && landmarks[4].getX() < pseudoFixKeyPoint) {
            jempolTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[6].getY(); //compare y
        if (landmarks[7].getY() < pseudoFixKeyPoint && landmarks[8].getY() < pseudoFixKeyPoint) {
            telunjukTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[10].getY(); //compare y
        if (landmarks[11].getY() < pseudoFixKeyPoint && landmarks[12].getY() < pseudoFixKeyPoint) {
            jariTengahTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[14].getY(); //compare y
        if (landmarks[15].getY() < pseudoFixKeyPoint && landmarks[16].getY() < pseudoFixKeyPoint) {
            jariManisTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[18].getY(); //compare y
        if (landmarks[19].getY() < pseudoFixKeyPoint && landmarks[20].getY() < pseudoFixKeyPoint) {
            kelingkingTerbuka = true;
        }

        // Hand gesture recognition
        if (jempolTerbuka && !telunjukTerbuka && !jariTengahTerbuka && !jariManisTerbuka && !kelingkingTerbuka) {
            return "10";
        }

        return "Gesture tidak di kenali";
    }

    public static String telapakTanganGesture(StructuredLandmarks[] landmarks) {
        // finger states
        boolean jempolTerbuka = false;
        boolean telunjukTerbuka = false;
        boolean jariTengahTerbuka = false;
        boolean jariManisTerbuka = false;
        boolean kelingkingTerbuka = false;
        LOGGER.d("Telapak tangan joints = " + Arrays.toString(landmarks));

        // Variable Angka 6
        boolean angka6Kelingking = landmarks[4].getX() < landmarks[13].getX();
        boolean angka6Jempol = landmarks[18].getY() < landmarks[20].getY();

        // Variable Angka 7
        boolean angka7Manis = landmarks[16].getY() > landmarks[14].getY();
        boolean angka7Jempol = landmarks[4].getX() < landmarks[9].getX();

        double pseudoFixKeyPoint = landmarks[2].getX(); //compare x
        if (landmarks[3].getX() < pseudoFixKeyPoint && landmarks[4].getX() < pseudoFixKeyPoint) {
            jempolTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[6].getY(); //compare y
        if (landmarks[7].getY() < pseudoFixKeyPoint && landmarks[8].getY() < pseudoFixKeyPoint) {
            telunjukTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[10].getY(); //compare y
        if (landmarks[11].getY() < pseudoFixKeyPoint && landmarks[12].getY() < pseudoFixKeyPoint) {
            jariTengahTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[14].getY(); //compare y
        if (landmarks[15].getY() < pseudoFixKeyPoint && landmarks[16].getY() < pseudoFixKeyPoint) {
            jariManisTerbuka = true;
        }

        pseudoFixKeyPoint = landmarks[18].getY(); //compare y
        if (landmarks[19].getY() < pseudoFixKeyPoint && landmarks[20].getY() < pseudoFixKeyPoint) {
            kelingkingTerbuka = true;
        }

        // Hand gesture recognition
        if (!jempolTerbuka && telunjukTerbuka && jariTengahTerbuka && jariManisTerbuka && kelingkingTerbuka) {
            return "5";
        } else if (jempolTerbuka && telunjukTerbuka && jariTengahTerbuka && jariManisTerbuka && kelingkingTerbuka) {
            return "4";
        } else if (!jempolTerbuka && telunjukTerbuka && jariTengahTerbuka && !jariManisTerbuka && !kelingkingTerbuka) {
            return "3";
        } else if (jempolTerbuka && telunjukTerbuka && jariTengahTerbuka && !jariManisTerbuka && !kelingkingTerbuka) {
            return "2";
        } else if (jempolTerbuka && telunjukTerbuka && !jariTengahTerbuka && !jariManisTerbuka && !kelingkingTerbuka) {
            return "1";
        } else if (!jempolTerbuka && !telunjukTerbuka && !jariTengahTerbuka && !jariManisTerbuka && !kelingkingTerbuka) {
            return "0";
        } else if (angka6Jempol && angka6Kelingking && telunjukTerbuka && jariTengahTerbuka && jariManisTerbuka) {
            return "6";
        } else if (angka7Jempol && angka7Manis && kelingkingTerbuka && jariTengahTerbuka && telunjukTerbuka) {
            return "7";
        }

        return "Gesture tidak di kenali";
    }

    // fungsi untuk filter koordinat x dan y
    StructuredLandmarks[] getStructuredLandmarks(float[] joints) {
        int index = 0;
        StructuredLandmarks[] structuredLandmarks = new StructuredLandmarks[21];

        for (int i = 0; i < joints.length; i++) {
            if (i % 2 == 0) {
                // Insert X and Y
                structuredLandmarks[index] = new StructuredLandmarks(joints[i], joints[i + 1]);
                index++;
            }
        }

        return structuredLandmarks;
    }

}
