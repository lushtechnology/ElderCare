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

package com.lushtech.eldercare.activity.utilities;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.support.annotation.NonNull;
import android.util.Size;

import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Class to work with {@link Bitmap} conversions.
 */
public final class BitmapUtilities {
    @SuppressWarnings("MagicNumber")
    private static final int COMPRESSION_QUALITY = 80;
    private static final int RGBA_8888_IMAGE_PLANE_LENGTH = 1;

    private BitmapUtilities() {
    }

    /**
     * Properly converts an {@link Image} to a {@link Bitmap}.
     *
     * @param image The {@link Image} to convert
     * @return The converted {@link Bitmap}
     */
    @NonNull
    public static Bitmap imageToBitmap(@NonNull final Image image) {
        return byteBufferToBitmap(getCleanBufferForImage(image), new Size(image.getWidth(), image.getHeight()));
    }

    /**
     * Gets a proper buffer to access the pixels of an {@link Image}, removing padding if necessary.
     *
     * @param image The {@link Image} to extract the buffer from
     * @return The {@link ByteBuffer}
     * @throws IllegalArgumentException: When {@link Image} object has an unsupported pixel format or is null
     */
    @NonNull
    public static ByteBuffer getCleanBufferForImage(@NonNull final Image image) {
        if (image.getFormat() != PixelFormat.RGBA_8888) {
            throw new IllegalArgumentException("Only Image with pixel format RGBA_8888 supported!");
        }

        final Image.Plane[] planes = image.getPlanes();
        if (planes.length != RGBA_8888_IMAGE_PLANE_LENGTH) {
            throw new RuntimeException("only a single plane is allowed in RGBA_8888");
        }

        if (planes[0].getRowStride() != (image.getWidth() * planes[0].getPixelStride())) {
            return getImageBufferWithoutPadding(image);
        } else {
            return planes[0].getBuffer();
        }
    }

    /**
     * Converts a {@link ByteBuffer} to a {@link Bitmap}, interpreting the buffer content as RGBA pixels.
     *
     * @param buffer    The buffer to convert
     * @param imageSize The dimensions of the {@link Bitmap}. The buffer must contains at least width * height * 4 bytes
     * @return The {@link Bitmap}
     */
    @NonNull
    public static Bitmap byteBufferToBitmap(@NonNull final ByteBuffer buffer, @NonNull final Size imageSize) {
        final Bitmap bitmap = Bitmap.createBitmap(imageSize.getWidth(), imageSize.getHeight(), Bitmap.Config.ARGB_8888);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    /**
     * Utility function to compress a {@link Bitmap} as a JPEG image.
     *
     * @param bitmap the {@link Bitmap} to compress
     * @return byte array of compressed {@link Bitmap} on success
     */
    @Nullable
    public static byte[] compressBitmap(@Nullable final Bitmap bitmap) {
        if (bitmap != null) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } else {
            return null;
        }
    }

    /**
     * Slower implementation of extracting the Image's buffer when the internal planes contains
     * row padding bytes. Happens for small resolutions.
     */
    @NonNull
    private static ByteBuffer getImageBufferWithoutPadding(@NonNull final Image image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final Image.Plane plane = image.getPlanes()[0];
        final int bytesPerPixel = plane.getPixelStride();

        final ByteBuffer planeBuffer = plane.getBuffer();

        final byte[] bitmapBytes = new byte[width * height * bytesPerPixel];

        for (int row = 0; row < height; row++) {
            /*
             * Position the planeBuffer offset to the start of a row/stride.
             * The code uses getRowStride() because there are maybe padding bytes at the end of
             * a row.
             */
            planeBuffer.position(row * plane.getRowStride());

            /*
             * Copy 'width * bytesPerPixel' from the current position in the plane to the bitmap buffer.
             * NOTE: There are no padding bytes in the target bitmap buffer. So the equation
             * width * bytesPerPixel == RowStride() is true for the bitmap buffer.
             */
            planeBuffer.get(bitmapBytes, row * width * bytesPerPixel, width * bytesPerPixel);
        }

        return ByteBuffer.wrap(bitmapBytes);
    }
}
