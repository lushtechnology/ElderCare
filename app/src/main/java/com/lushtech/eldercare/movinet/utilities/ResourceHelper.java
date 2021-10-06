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

package com.lushtech.eldercare.movinet.utilities;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Helper class to load and map models and their labels
 */
public final class ResourceHelper {
    private ResourceHelper() {
        throw new UnsupportedOperationException("Helper class cannot be instantiated");
    }

    /**
     * Memory-map the model file in Assets.
     *
     * @param assets        Asset manager instance
     * @param modelFilename Name of the model
     * @return Mapped byte buffer containing the model
     * @throws IOException On failure to map resource
     */
    public static MappedByteBuffer loadModelFile(final AssetManager assets, final String modelFilename)
        throws IOException {
        final AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        final FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        final FileChannel fileChannel = inputStream.getChannel();
        final long startOffset = fileDescriptor.getStartOffset();
        final long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Loads label file
     *
     * The label file is assumed to contain the labels that the model was trained with in order.
     *
     * If a classifier mapped
     *
     * 0 - apple
     * 1 - banana
     * 2 - pear
     *
     * Then the label file should look like
     *
     * apple
     * banana
     * pear
     *
     * @param context         App context
     * @param labelResourceId Resource id
     * @return List of labels in order
     * @throws IOException On failure to read specified resource
     */
    public static List<String> loadLabels(final Context context, final @RawRes int labelResourceId) throws IOException {
        try (InputStream labelsInput = context.getResources().openRawResource(labelResourceId);
            BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput, UTF_8))) {
            return br.lines().collect(Collectors.toList());
        }
    }
}
