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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.lushtech.eldercare.activity.detector.Recognition;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Utility class to aid in rendering {@link Recognition} data
 */
@SuppressWarnings("MagicNumber")
public final class Renderer {
    private static final Paint NON_DETECTED_AREA_PAINT = getNonDetectedAreaPaint();
    private static final Paint BACKGROUND_PAINT = getBackgroundPaint();
    private static final HashMap<String, Paint> LABEL_PAINT_CACHE = new HashMap<>();
    private static final Paint TEXT_PAINT = getTextPaint();
    private static final Paint TEXT_BG_PAINT = getTextBackgroundPaint();
    private static final int TEXT_MARGIN = 1;
    private static final float TEXT_SIZE = 24.0f;
    private static final int COLOR_MASK = 0xffffff;
    private static final int ALPHA_MASK = 0xff000000;
    private static final int COLOR_BLACK = 0xff000000;
    private static final int COLOR_WHITE = 0xffffffff;
    private static final int COLOR_NON_DETECTION_AREA = 0x66000000;

    private static int sMarginX;
    private static int sMarginY;
    private static int sCropAreaW;
    private static int sCropAreaH;

    private Renderer() {
    }

    /**
     * Gets a paint that shades areas that are not actively used by a detector
     *
     * @return Paint for use in non-detection areas
     */
    private static Paint getNonDetectedAreaPaint() {
        final Paint res = new Paint(Paint.ANTI_ALIAS_FLAG);
        res.setColor(COLOR_NON_DETECTION_AREA);
        return res;
    }

    /**
     * Gets a base paint that is clear
     *
     * @return Paint for use on the background
     */
    private static Paint getBackgroundPaint() {
        final Paint res = new Paint();
        res.setStyle(Paint.Style.FILL);
        res.setColor(0);
        return res;
    }

    /**
     * Gets a base paint that is white
     *
     * @return Paint for use in drawing text
     */
    private static Paint getTextPaint() {
        final Paint res = new Paint();
        res.setStyle(Paint.Style.FILL);
        res.setTextSize(TEXT_SIZE);
        res.setColor(COLOR_BLACK);
        return res;
    }

    /**
     * Gets a base paint that is black
     *
     * @return Black paint
     */
    private static Paint getTextBackgroundPaint() {
        final Paint res = new Paint();
        res.setStyle(Paint.Style.FILL);
        res.setColor(COLOR_BLACK);
        return res;
    }

    /**
     * Renders a list of objects on a canvas
     *
     * @param canvas        The canvas to use for drawing
     * @param objects       The objects to render
     * @param minConfidence The minimum acceptable confidence for a detection to be rendered
     * @param cropW         The width of the cropped area
     * @param cropH         The height of the cropped area
     * @param inputW        The width of the image on which to draw
     * @param inputH        The height of the image on which to draw
     * @param marginX       The x dimension margin
     * @param marginY       The y dimension margin
     */
    public static void render(final Canvas canvas, final List<Recognition> objects, final float minConfidence,
        final int cropW, final int cropH, final int inputW, final int inputH, final int marginX, final int marginY) throws InterruptedException {
        /*
         * Static parameters used in many areas set at the beginning of rendering
         */
        sMarginX = marginX;
        sMarginY = marginY;
        sCropAreaW = cropW;
        sCropAreaH = cropH;
        canvas.drawPaint(BACKGROUND_PAINT);

        /*
         * Shade the areas that were not used in detection
         */
        canvas.drawRect(0, 0, inputW, sMarginY, NON_DETECTED_AREA_PAINT);
        canvas.drawRect(0, sCropAreaH + sMarginY, inputW, inputH, NON_DETECTED_AREA_PAINT);
        canvas.drawRect(0, sMarginY, sMarginX, sCropAreaH + sMarginY, NON_DETECTED_AREA_PAINT);
        canvas.drawRect(sMarginX + sCropAreaW, sMarginY, inputW, sCropAreaH + sMarginY, NON_DETECTED_AREA_PAINT);

        /*
         * Render each object on the canvas
         *
         *
         */
        if (!objects.isEmpty()) {
            for (Recognition obj : objects) {
                if (obj.getConfidence() < minConfidence) {
                    continue;
                }
                // final RectF box = translate(obj.getLocation());
                /*
                 * Draw the translated bounding box
                 */
                //canvas.drawRect(box, getPaint(obj.getLabel()));
                /*
                 * Draw the label and confidence inside a black rectangle for readability
                 */
                final String label = String.format(Locale.US, "%s: %.1f%%", obj.getLabel(), obj.getConfidence() * 100);
                final float textW = TEXT_PAINT.measureText(label);
            /*
            canvas.drawRect(box.left,
                box.top + TEXT_MARGIN,
                box.left + textW + (TEXT_MARGIN << 1),
                box.top + TEXT_SIZE + (TEXT_MARGIN << 1),
                TEXT_BG_PAINT);

             */
                canvas.drawText(label, TEXT_MARGIN + sMarginX,
                        TEXT_SIZE,
                        TEXT_PAINT);




            }
        }
    }

    /**
     * Helper function maps from relative bounding box to rendering coordinates
     *
     * @param location The relative bounding box
     * @return Scaled bounding box ready to render on the Canvas with crop area (region sent to detection) defined by
     * sMarginX, sMarginY, sCropAreaH, and sCropAreaW
     */
    private static RectF translate(final RectF location) {
        return new RectF((location.left * sCropAreaW) + sMarginX,
            (location.top * sCropAreaH) + sMarginY,
            (location.right * sCropAreaW) + sMarginX,
            (location.bottom * sCropAreaH) + sMarginY);
    }

    /**
     * Gets the paint for a specific class of object. The first time an object is encountered the paint is built and
     * configured then stored in a cache. All subsequent requests for that object are returned from the cache directly.
     * @param label The object class
     * @return A paint unique to that class
     */
    private static Paint getPaint(final String label) {
        if (!LABEL_PAINT_CACHE.containsKey(label)) {
            final int strokeWidth = 2;
            final Paint p = new Paint(Paint.LINEAR_TEXT_FLAG);
            p.setColor((label.hashCode() & COLOR_MASK) | ALPHA_MASK);
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(strokeWidth);
            LABEL_PAINT_CACHE.put(label, p);
        }
        return LABEL_PAINT_CACHE.get(label);
    }
}
