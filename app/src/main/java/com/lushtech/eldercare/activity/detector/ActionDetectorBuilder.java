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

package com.lushtech.eldercare.activity.detector;

import android.content.Context;
import com.lushtech.eldercare.movinet.R;

/**
 * Configures and builds an object detector
 */
public class ActionDetectorBuilder {
    private final Context mContext;
    private String mModelFileName = "model.tflite";
    private int mLabelFileResId = R.raw.labelmap;
    @SuppressWarnings("MagicNumber")
    private int mMaxDetectionsPerImage = 10;
    @SuppressWarnings("MagicNumber")
    private int mInputSize = 172;
    @SuppressWarnings("MagicNumber")
    private int mNumThreads = 2;
    private boolean mAllowFp16PrecisionForFp32 = false;
    private boolean mUseNNAPI = false;
    private boolean mIsQuantized = false;

    /**
     * Creates a new detector builder
     *
     * @param context App context
     */
    public ActionDetectorBuilder(final Context context) {
        mContext = context;
    }

    /**
     * Builds the detector with the current configuration
     *
     * @return A ready to use object detector
     * @throws ExceptionInInitializerError On failure to initialize due to error in IO
     */
    public ActionDetector build() throws ExceptionInInitializerError {
        /* Additional error and exception handling can be done here */
        return new ActionDetector(mContext,
            mModelFileName,
            mLabelFileResId,
            mMaxDetectionsPerImage,
            mInputSize,
            mNumThreads,
            mAllowFp16PrecisionForFp32,
            mUseNNAPI,
            mIsQuantized);
    }

    /**
     * Sets model name
     *
     * @param name Filename of the model
     * @return This builder
     */
    public ActionDetectorBuilder setModelFileName(final String name) {
        mModelFileName = name;
        return this;
    }

    /**
     * Sets the maximum number of detections for a single image
     * @param n The maximum
     * @return This builder
     */
    public ActionDetectorBuilder setMaxDetectionsPerImage(final int n) {
        mMaxDetectionsPerImage = n;
        return this;
    }

    /**
     * Sets input size the detector expects
     *
     * @param size nxn size value for n
     * @return This builder
     */
    public ActionDetectorBuilder setInputSize(final int size) {
        mInputSize = size;
        return this;
    }

    /**
     * Sets the number of threads to use
     *
     * @param n Number of threads
     * @return This builder
     */
    public ActionDetectorBuilder setNumThreads(final int n) {
        mNumThreads = n;
        return this;
    }

    /**
     * Configures the detector to use 16 bit precision for 32 bit values to save on space
     *
     * @return This builder
     */
    public ActionDetectorBuilder allowFp16PrecisionForFp32() {
        mAllowFp16PrecisionForFp32 = true;
        return this;
    }

    /**
     * Configures the detector to use the neural network processing API
     *
     * @return This builder
     */
    public ActionDetectorBuilder useNNAPI() {
        mUseNNAPI = true;
        return this;
    }

    /**
     * Sets whether the model in question is quantized (lossy compressed) or not
     *
     * @param isQuantized Boolean specifying whether the model is quantized
     * @return This builder
     */
    public ActionDetectorBuilder setIsQuantized(final boolean isQuantized) {
        mIsQuantized = isQuantized;
        return this;
    }
}
