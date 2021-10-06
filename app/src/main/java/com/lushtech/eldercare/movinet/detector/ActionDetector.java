/*
 * Copyright 2019-2020 by Security and Safety Things GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lushtech.eldercare.movinet.detector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.RawRes;
import android.util.Log;
import android.util.Size;
import com.lushtech.eldercare.movinet.services.MainService;
import com.lushtech.eldercare.movinet.utilities.ArgMax;
import com.lushtech.eldercare.movinet.utilities.ResourceHelper;
import com.lushtech.eldercare.movinet.utilities.SoftMax;
import org.apache.commons.lang3.StringUtils;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ObjectDetector is the class responsible for preparing and accessing our model. It is based on the example provided by
 * the TensorFlow team
 */
public class ActionDetector {

    private static final String LOGTAG = MainService.class.getSimpleName();
    /**
     * Gets byte associated with red channel
     */
    @SuppressWarnings("MagicNumber")
    private static final int SHIFT_RED = 16;
    /**
     * Gets byte associated with green channel
     */
    @SuppressWarnings("MagicNumber")
    private static final int SHIFT_GREEN = 8;
    /**
     * Median value of 0 - 255 used to normalize inputs for non quantized models
     */
    @SuppressWarnings("MagicNumber")
    private static final float IMAGE_MED = 128.0f;
    /**
     * Used to select individual bytes from RGB channels of the image
     */
    @SuppressWarnings("MagicNumber")
    private static final int BYTE_MASK = 0xff;
    /**
     * nxn size of the image the model expects as input
     */
    private final int mInputSize;
    /**
     * Ordered list mapping from model output to string label
     */
    private final List<String> mLabels;
    /**
     * TensorFlow lite api
     */
    private final Interpreter mModel;
    /**
     * Whether the model is quantized or not. This affects how input images are processed
     */
    private final boolean mIsQuantized;
    /**
     * Holds the int pixel values for each image
     */
    private final int[] mPixelValues;
    /**
     * Buffer for the image data that will contain bytes in RGB ordering
     */
    //private final ByteBuffer mImgData;
    private final float[][][][][] mImgData;
    private final Map<String, Object> initInputMap = new HashMap<>();
    private final Map<String, Object> initOutputMap = new HashMap<>();
    private final List<String> keys = new ArrayList<>();
    /**
     * outputClasses: array of shape [Batchsize, mMaxDetectionsPerImage]
     * contains the classes of detected boxes
     */
    private Map<String, Object> inputMap = new HashMap<>();
    private Map<String, Object> outputMap = new HashMap<>();
    private int frameIdx = 1;
    private List<Recognition> recognitions;

    //private List<Recognition> recognitions = new ArrayList<>();

    /**
     * Constructs a new ObjectDetector from a specified model and label file
     * <p>
     * Determines buffer sizes and loads the specified model and labels
     *
     * @param c                         The application context
     * @param modelFileName             The name of the model to load from the app assets directory
     * @param labelFileResId            Int resource id of the label file stored in /res/raw/
     * @param maxDetectionsPerImage     The max number of detections per image
     * @param inputSize                 The size of the input the model expects should be inputSize x inputSize
     * @param numThreads                The number of threads TensorFlow should be instructed to use
     * @param allowFp16PrecisionForFp32 When set, optimizes memory at the cost of accuracy by using 16 bit floating
     *                                  point numbers rather than 32 bit
     * @param useNNAPI                  When set TensorFlow will be configured to use the Android neural network api
     *                                  that attempts to run the model on the best available hardware for any system.
     *                                  Has limited support on the Security and Safety Things platform
     * @param isQuantized               Defines whether the input model is quantized (lossy compressed) or not
     *                                  this is a property of the model and should be set accordingly
     */
    @SuppressWarnings("MagicNumber")
    ActionDetector(final Context c,
                   final String modelFileName,
                   final @RawRes int labelFileResId,
                   final int maxDetectionsPerImage,
                   final int inputSize,
                   final int numThreads,
                   final boolean allowFp16PrecisionForFp32,
                   final boolean useNNAPI,
                   final boolean isQuantized) {
        mInputSize = inputSize;
        mIsQuantized = isQuantized;
        final int numBytesPerChannel = mIsQuantized ? 1 : 4;
        /*
         * Allocate image buffer using height x width x 3 (from RGB channels) x <size of data>
         */
        // mImgData = ByteBuffer.allocateDirect(mInputSize * mInputSize * 3 * numBytesPerChannel);
        /*
         * Use endianness of the hardware for the buffer
         */
        //mImgData.order(ByteOrder.nativeOrder());
        mImgData = new float[1][1][172][172][3];
        /*
         * Allocate array for image pixel values
         */
        mPixelValues = new int[mInputSize * mInputSize];


        /*
         * Configure TensorFlow interpreter options from parameters
         */
        final Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatList = new CompatibilityList();

        if (compatList.isDelegateSupportedOnThisDevice()) {
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
            Log.i(LOGTAG, "-----------Running using GPU Delegate-----------");
        } else {
            // if the GPU is not supported, run on numThreads threads
            options.setNumThreads(numThreads)
                    .setAllowFp16PrecisionForFp32(allowFp16PrecisionForFp32)
                    .setUseNNAPI(useNNAPI);
            Log.i(LOGTAG, "------------Running using CPU Delegate---------");
        }

        try {
            /* Load the model */
            mModel = new Interpreter(ResourceHelper.loadModelFile(c.getAssets(), modelFileName), options);
            /* Prepare the labels */
            mLabels = ResourceHelper.loadLabels(c, labelFileResId);

            //mModel.resizeInput(0, new int[]{mInputSize * mInputSize * 3});
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Unable to create ObjectDetector");
        }
    }

    public void reset_buffer() {

        /**
         * Resets buffer to initial states

         */

        outputMap = keys.stream()
                .collect(Collectors.toMap(Function.identity(), initOutputMap::get));

        inputMap = keys.stream()
                .collect(Collectors.toMap(Function.identity(), initInputMap::get));

    }

    public void init_buffer() {


        for (int i = 0; i < mModel.getInputTensorCount(); i++) {

            Tensor x = mModel.getInputTensor(i);


            if (x.dataType().toString().equals("FLOAT32")) {

                /**
                 * Model input and output signatures can be seen using python API (tensorflow 2.5)
                 * import tensorflow as tf
                 * interpreter = tf.lite.Interpreter('model.tflite')
                 * print(interpreter.get_signature_list())
                 *
                 */

                Object input = Array.newInstance(Float.TYPE, x.shape());
                Object output = Array.newInstance(Float.TYPE, x.shape());
                String key = StringUtils.substringBetween(x.name(), "serving_default_", ":0");


                if (!key.equals("image")) {

                    keys.add(key);

                    initInputMap.put(key, input);
                    initOutputMap.put(key, output);
                }

            } else {

                Object input = Array.newInstance(Integer.TYPE, x.shape());
                Object output = Array.newInstance(Integer.TYPE, x.shape());
                String key = StringUtils.substringBetween(x.name(), "serving_default_", ":0");


                if (!key.equals("image")) {

                    initInputMap.put(key, input);
                    initOutputMap.put(key, output);

                    keys.add(key);
                }

            }


        }


        inputMap = keys.stream()
                .collect(Collectors.toMap(Function.identity(), initInputMap::get));


        outputMap = keys.stream()
                .collect(Collectors.toMap(Function.identity(), initOutputMap::get));


    }

    /**
     * Runs inference on a bitmap
     * 1. Performs some data preprocessing
     * Populates the `imgData` input array with bytes from the bitmap in RGB order
     * Normalizes data if necessary
     * <p>
     * 2. Inference
     * Sets up inputs and outputs for the TensorFlow lite api `runSignature`
     * https://www.tensorflow.org/lite/guide/inference
     * <p>
     * Inputs
     * Object[1]: our imgData array is the sole element
     * <p>
     * Outputs (in index order)
     * Object class id: mapped to human readable label using the label map
     * Confidence: float in range from 0 to 1
     * Detection Count: how many objects were detected in the frame
     * <p>
     * 3. Maps outputs to
     * {@link Recognition} objects
     * for easier use.
     *
     * @param bitmap Image bitmap to run inference on
     * @return List of recognized objects
     */
    @SuppressWarnings("MagicNumber")
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        /*
         * Preprocess the image data from 0-255 int to normalized value based on the provided parameters.
         */
        bitmap.getPixels(mPixelValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        /*mImgData.rewind();
        for (int i = 0; i < mInputSize; ++i) {
            for (int j = 0; j < mInputSize; ++j) {
                *//*
         * Get the data for the jth pixel in the ith row of the image
         *//*
                final int pixelValue = mPixelValues[i * mInputSize + j];
                if (mIsQuantized) {
                    *//* Quantized model
                    mImgData.put((byte) ((pixelValue >> SHIFT_RED) & BYTE_MASK));
                    mImgData.put((byte) ((pixelValue >> SHIFT_GREEN) & BYTE_MASK));
                    mImgData.put((byte) (pixelValue & BYTE_MASK));
                } else {
                    /* Float model *//*
                    mImgData.putFloat((((pixelValue >> SHIFT_RED) & BYTE_MASK) - IMAGE_MED) / IMAGE_MED);
                    mImgData.putFloat((((pixelValue >> SHIFT_GREEN) & BYTE_MASK) - IMAGE_MED) / IMAGE_MED);
                    mImgData.putFloat(((pixelValue & BYTE_MASK) - IMAGE_MED) / IMAGE_MED);
                }
            }
        }*/

       // long start = System.currentTimeMillis();
        for (int i = 0; i < mInputSize; ++i) {
            for (int j = 0; j < mInputSize; ++j) {
                final int pixelValue = mPixelValues[i * mInputSize + j];
                Color color = Color.valueOf(pixelValue);

                if (mIsQuantized) {
                    //Quantized model
                    //mImgData.put((byte) ((pixelValue >> SHIFT_RED) & BYTE_MASK));
                    //mImgData.put((byte) ((pixelValue >> SHIFT_GREEN) & BYTE_MASK));
                    //mImgData.put((byte) (pixelValue & BYTE_MASK));

                } else {
                    // Float model
                    mImgData[0][0][i][j][0] = color.red();
                    mImgData[0][0][i][j][1] = color.green();
                    mImgData[0][0][i][j][2] = color.blue();

                }
            }
        }
        //long finish = System.currentTimeMillis();

        //long timeElapsed = finish - start;

        //Log.d(LOGTAG, String.format("Filling image time %d", timeElapsed));

        /**
         * outputScores: array of shape [1, 600]
         * contains the scores of detected actions
         */
        float[][] mOutputScores = new float[1][600];

        ArgMax probabiliyProcessor;
        if (frameIdx < 8) {

            inputMap.put("image", mImgData);

            outputMap.put("logits", mOutputScores);

            //start = System.currentTimeMillis();

            mModel.runSignature(inputMap, outputMap);

            //finish = System.currentTimeMillis();

            //timeElapsed = finish - start;

            //Log.d(LOGTAG, String.format("Inference time %d", timeElapsed));

            inputMap = keys.stream()
                    .filter(outputMap::containsKey)
                    .collect(Collectors.toMap(Function.identity(), outputMap::get));



            float[][] predictions = (float[][]) outputMap.get("logits");

            probabiliyProcessor = new ArgMax(predictions[0]);

            float score = probabiliyProcessor.getResult().getMaxValue();

            SoftMax softmaxProcessor = new SoftMax(score,predictions[0]);


            double score_normalized = softmaxProcessor.get_softmax();



            int index = probabiliyProcessor.getResult().getIndex();


            recognitions = new ArrayList<>();

           /* recognitions.add( new Recognition(
                    String.valueOf(frameIdx),
                    mLabels.get(index),
                   score));*/
            frameIdx++;

            //Log.d(LOGTAG,recognitions.toString());
        } else {




            inputMap.put("image", mImgData);

            outputMap.put("logits", mOutputScores);

            mModel.runSignature(inputMap, outputMap);

            inputMap = keys.stream()
                    .filter(outputMap::containsKey)
                    .collect(Collectors.toMap(Function.identity(), outputMap::get));

            float[][] predictions = (float[][]) outputMap.get("logits");

            probabiliyProcessor = new ArgMax(predictions[0]);

            float score = probabiliyProcessor.getResult().getMaxValue();

            SoftMax softmaxProcessor = new SoftMax(score,predictions[0]);


            float score_normalized = (float)softmaxProcessor.get_softmax();

            int index = probabiliyProcessor.getResult().getIndex();

            recognitions = new ArrayList<>();


            recognitions.add( new Recognition(
                    String.valueOf(frameIdx),
                    mLabels.get(index),
                    score_normalized));


            Log.i(LOGTAG, String.format("Predicted Class %s, Score %f", mLabels.get(index),score_normalized));

            //start = System.currentTimeMillis();

            reset_buffer();

            //finish = System.currentTimeMillis();

            //timeElapsed = finish - start;

            //Log.d(LOGTAG, String.format("Reseting buffer time %d", timeElapsed));


            frameIdx = 1;

            //Log.d(LOGTAG,recognitions.toString());

            //recognitions = new ArrayList<>();




        }

       //long latency = mModel.getLastNativeInferenceDurationNanoseconds();

        //Log.d(LOGTAG, String.format("Network latency %d in miliseconds ", TimeUnit.NANOSECONDS.toMillis(latency)));


        return recognitions;
    }

    /**
     * Gets the size of image the detector requires
     *
     * @return Size defining the expected height and width of the input
     */
    public Size getRequiredImageSize() {
        return new Size(mInputSize, mInputSize);
    }
}
