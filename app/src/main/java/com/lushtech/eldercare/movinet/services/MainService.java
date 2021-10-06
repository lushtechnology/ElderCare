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

package com.lushtech.eldercare.movinet.services;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import com.google.protobuf.ByteString;
import com.lushtech.eldercare.movinet.detector.ActionDetector;
import com.lushtech.eldercare.movinet.detector.ActionDetectorBuilder;
import com.lushtech.eldercare.movinet.detector.Recognition;
import com.lushtech.eldercare.movinet.rest.RestEndPoint;
import com.lushtech.eldercare.movinet.utilities.BitmapUtilities;
import com.lushtech.eldercare.movinet.utilities.EasySharedPreference;
import com.lushtech.eldercare.movinet.utilities.Renderer;
import com.securityandsafetythings.Build;
import com.securityandsafetythings.app.VideoService;
import com.securityandsafetythings.jumpsuite.datatrolley.channels.Channel;
import com.securityandsafetythings.jumpsuite.datatrolley.channels.MetadataChannel;
import com.securityandsafetythings.jumpsuite.datatrolley.exceptions.InvalidChannelException;
import com.securityandsafetythings.jumpsuite.datatrolley.exceptions.InvalidFramePayloadException;
import com.securityandsafetythings.jumpsuite.datatrolley.interfaces.MetadataTrolley;
import com.securityandsafetythings.jumpsuite.datatrolley.interfaces.events.OnTrolleyConnectedEvent;
import com.securityandsafetythings.jumpsuite.datatrolley.metadata.FramePayload;
import com.securityandsafetythings.jumpsuite.datatrolley.metadata.ObjectPayload;
import com.securityandsafetythings.jumpsuite.datatrolley.trolleys.DataTrolleyProvider;
import com.securityandsafetythings.video.RefreshRate;
import com.securityandsafetythings.video.VideoCapture;
import com.securityandsafetythings.video.VideoManager;
import com.securityandsafetythings.video.VideoSession;
import com.securityandsafetythings.web_components.webserver.RestHandler;
import com.securityandsafetythings.web_components.webserver.WebServerConnector;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * This class responds to {@link #onCreate()} and {@link #onDestroy()} methods of the application
 * lifecycle. In order to receive images from the Video Pipeline, {@link MainService} extends
 * {@link VideoService} and implements its callbacks.
 *
 * The three callbacks are:
 *
 * 1. {@link #onVideoAvailable(VideoManager)} - Triggered when the Video Pipeline is ready to begin transferring {@link Image} objects.
 * 2. {@link #onImageAvailable(ImageReader)} - Triggered when the {@link ImageReader} acquires a new {@link Image}.
 * 3. {@link #onVideoClosed(VideoSession.CloseReason)} - Triggered for one of the four close reasons mentioned in method's JavaDoc.
 */
public class MainService extends VideoService {

    private static final String LOGTAG = MainService.class.getSimpleName();
    /*
     * When the video session is restarted due to base camera configuration changed,
     * this Handler is used to post messages to the UI thread/main thread
     * for proper rendering
     */
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());
    private WebServerConnector mWebServerConnector;
    private RestEndPoint mRestEndPoint;
    private ActionDetector mDetector;
    private Size mDetectorInputSize;
    private VideoManager mVideoManager;
    private MetadataTrolley mDataTrolley;
    private Channel mObjectsChannel;
    private long mFrameId;


    /**
     * {@link #onCreate()} initializes our {@link WebServerConnector}, {@link RestEndPoint}, and
     * {@link RestHandler}.
     *
     * The {@link WebServerConnector} acts as the bridge between our application and the webserver
     * which is contained in the Security and Safety Things SDK.
     *
     * The {@link RestEndPoint} is a class annotated with JaxRs endpoint annotations. This is the class
     * that we interact with via HTTP on the front end.
     *
     * The {@link RestHandler} acts as a wrapper class for our {@link RestEndPoint}. The Handler registers our
     * {@link RestEndPoint}, and connects it to the WebServer.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        /*
         * Creates a RestHandler with a base path of 'app/getPackageName()'.
         */
        final String websiteAssetPath = "website";
        final RestHandler restHandler = new RestHandler(this, websiteAssetPath);
        /*
         * Registers the RestEndPoint with the server via the RestHandler class. The RestHandler
         * is just a wrapper for the RestEndPoint's JaxRs annotated functions.
         */
        mRestEndPoint = new RestEndPoint();
        restHandler.register(mRestEndPoint);
        /*
         * Connects the RestHandler with the WebServerConnector.
         */
        mWebServerConnector = new WebServerConnector(this);
        mWebServerConnector.connect(restHandler);
        /*
         * Get a detector builder and use it to configure the detector
         */
        mDetector = new ActionDetectorBuilder(this)
            .setIsQuantized(false)
            .build();

        // Register with EventBus to receive events from DataTrolley.
        EventBus.getDefault().register(this);
        // Creates a MetadataTrolley to publish metadata to other apps and to external systems.
        mDataTrolley = DataTrolleyProvider.createMetadataTrolley(this);
        mDataTrolley.connect();
        /**
         *  Initialize mDetector Buffer
         */
        mDetector.init_buffer();



        mDetectorInputSize = mDetector.getRequiredImageSize();
    }


    /**
     * Wait for {@link MetadataTrolley} to connect before building the channel.
     *
     * @param onTrolleyConnectedEvent An event signaling that the {@link MetadataTrolley} has been connected.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final OnTrolleyConnectedEvent onTrolleyConnectedEvent) {
        try {
            /*
             * Creates a Channel that can be used to send metadata of the objects detected in the frame.
             * This channel is configured such that the metadata can be consumed by
             *    1. other apps that listen to this Channel - mObjectsChannel.getChannelPath()
             *    2. video management systems - "onvif/MetadataStream/VideoAnalytics"
             *    3. analytics dashboards (via IoT Gateway) - mObjectsChannel.getChannelPathForJSONPayload()
             */
            mObjectsChannel = new MetadataChannel.Builder(this, true, true)
                    .setName("person").setMetadataType(MetadataChannel.MetadataType.OTHER).build();
        } catch (InvalidChannelException e) {
            Log.e(LOGTAG, "Invalid channel specified.", e);
        }
    }

    /**
     * This callback is triggered when the Video pipeline is available to begin capturing images.
     *
     * @param manager is the {@link VideoManager} object we use to obtain access to the Video Pipeline
     */
    @SuppressWarnings("MagicNumber")
    @Override
    protected void onVideoAvailable(final VideoManager manager) {
        /**
         * Stores {@link VideoManager} for subscribing to video streams from the video pipeline
         */
        mVideoManager = manager;
        /*
         * Gets the default video capture and starts video session
         */
        startVideoSession();
    }

    /**
     * Callback is triggered when an image is obtained, here images can be stored for retrieval from the exposed
     * methods of the {@link RestEndPoint}.
     *
     * The example model operates on 300x300 pixel images. You may or may not recall that the image preview used in
     * Helloworld is full HD 1920x1080. We would not want to use just a small 300x300 crop of our preview image. Instead what
     * we can do is crop the full resolution image to the same aspect ratio as our model (1:1) and then rescale the cropped
     * image to the size our detector accepts. This way the resize operation does not warp our input, though this warping is
     * valid for some models, we won't use it here.
     *
     * @param reader Is the {@link ImageReader} object we use to obtain still frames from the Video Pipeline
     */
    @Override
    protected void onImageAvailable(final ImageReader reader) {
        /*
         * Gets the latest image from the Video Pipeline and stores it in the RestEndPoint class so the
         * frontend can retrieve it via a GET call to rest/example/live.
         */
        try (Image image = reader.acquireLatestImage()) {
            // ImageReader may sometimes return a null image.
            if (image == null) {
                Log.e("onImageAvailable()", "ImageReader returned null image.");
                return;
            }
            final Bitmap imageBmp = BitmapUtilities.imageToBitmap(image);
            /*
             * Crop to center region
             */
            final float targetAspectRatio = mDetectorInputSize.getWidth() / (float)mDetectorInputSize.getHeight();
            final Size cropSize = getCropArea(imageBmp.getWidth(), imageBmp.getHeight(), targetAspectRatio);
            /*
             * Calculate image margins
             * ">> 1" performs a bitshift division by 2 which computes the offset to the middle of the image
             */
            final int marginLeft = (imageBmp.getWidth() - cropSize.getWidth()) >> 1;
            final int marginTop = (imageBmp.getHeight() - cropSize.getHeight()) >> 1;
            /*
             * Calculate scale factor
             * How big is our detectors input compared to the image preview? We'll use this to scale our input
             * appropriately
             */
            final float scaleX = mDetectorInputSize.getWidth() / (float)cropSize.getWidth();
            final float scaleY = mDetectorInputSize.getHeight() / (float)cropSize.getHeight();
            /*
             * Construct scaling matrix
             */
            final Matrix scalingMatrix = new Matrix();
            scalingMatrix.postScale(scaleX, scaleY);
            /*
             * Take scaled center cut of the image to run classification on
             *
             * marginLeft: defines left boundary of the crop area
             * marginTop: defines top  boundary of the crop area
             * width: how far right to read in the x axis from marginLeft start point
             * height: how far down to read in the y axis from marginTop start point
             * scalingMatrix: how to resize the image after it's been cropped. This will scale the crop to 300x300
             * boolean: whether or not to filter pixels, true provides smoothing
             */
            final Bitmap classificationCroppedBitmap = Bitmap.createBitmap(imageBmp,
                marginLeft,
                marginTop,
                cropSize.getWidth(),
                cropSize.getHeight(),
                scalingMatrix,
                true);
            /*
             * Perform classification using the detector
             */
            final List<Recognition> detectionResults = mDetector.recognizeImage(classificationCroppedBitmap);
            /*
             * Utility class renders the poses on imageBmp. We pass crop size, and base image size along with the
             * margins so that we know where to draw the relative coordinates from the detector on the base image
             */
            Renderer.render(new Canvas(imageBmp),
                detectionResults, EasySharedPreference.getInstance().getMinConfidenceLevel(),
                cropSize.getWidth(),
                cropSize.getHeight(),
                imageBmp.getWidth(),
                imageBmp.getHeight(),
                marginLeft,
                marginTop);
            /*
             * Send the image to the rest endpoint just as before
             */
            mRestEndPoint.setImage(imageBmp);

            // Publishes the results of the inference.

            if (!detectionResults.isEmpty()) {
                publishResults(detectionResults);
            }



        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void publishResults(final List<Recognition> detectionResults) {
        if (!mDataTrolley.isConnected()) {
            Log.e(LOGTAG, "MetadataTrolley is not connected. Cannot publish results.");
            return;
        }
        if (mObjectsChannel == null) {
            Log.e(LOGTAG, "mObjectsChannel is null. Cannot publish results.");
            return;
        }
        /*
         * Converts {@link Recognition} objects to {@link FramePayload} to be sent through the MetadataTrolley, which
         * encapsulates marshalling and unmarshalling data to send over the MessageBroker API.
         */
        final FramePayload fp = recognitionsToFramePayload(detectionResults);
        /*
         * Sends the results of the inference to all apps including HelloWorld, that listen to 'mObjectsChannel'.
         * Since 'mObjectsChannel' was configured to
         *    1. transform the metadata to an ONVIF XML, DataTrolley will perform this conversion and send the XML
         *       to a channel that video management systems can listen to.
         *    2. transform the metadata to JSON, DataTrolley will perform this conversion and send the JSON data in a
         *       separate channel (mObjectsChannel.getChannelPathForJSONPayload()).
         */
        try {
            //Log.d(LOGTAG,String.format("Publish results %s",mObjectsChannel.getChannelPath()));
            mDataTrolley.send(mObjectsChannel, fp);
        } catch (InvalidFramePayloadException e) {
            Log.e(LOGTAG, "Failed to send FramePayload: " + fp, e);
        }
    }

    /**
     * Converts {@link Recognition} objects to {@code FramePayload} to send over a {@link MetadataTrolley}.
     *
     * @param detectionResults A list of {@code Recognition} objects to convert
     * @return A {@code FramePayload} representing the {@code Recognition} objects.
     */
    private FramePayload recognitionsToFramePayload(final List<Recognition> detectionResults) {
        int id = 0;
        final FramePayload.Builder framePayloadBuilder = FramePayload.newBuilder();
        framePayloadBuilder.setTimestamp(System.currentTimeMillis());
        for (Recognition recognition : detectionResults) {

            final ObjectPayload objectPayload =
                    ObjectPayload.newBuilder()
                            .setId(String.valueOf(id++))
                            .setConfidence(recognition.getConfidence())
                            .setLabel(recognition.getLabel())
                            .build();
            framePayloadBuilder.addObjects(objectPayload);
        }
        // Convert the frame ID into bytes to store as custom data for the FramePayload.
        framePayloadBuilder.setCustomData(ByteString.copyFrom(longToBytes(mFrameId++)));
        return framePayloadBuilder.build();
    }

    private byte[] longToBytes(final long x) {
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * Given the width and height of a region, provides a cropped width and height that matches a target aspect ratio
     *
     * This is simply an application of the formula:
     * Ratio = width / height
     * We are given Ratio, then we hold either width or height constant and solve for the other to produce the cropped
     * value resulting in the desired ratio.
     *
     * @param width             Input region width
     * @param height            Input region height
     * @param targetAspectRatio The aspect ratio floating point representation i.e) 1:1 = 1.0, 16:9  = 1.7778, etc.
     * @return new width and height paired in a {@link Size} object that matches the target aspect ratio
     */
    private Size getCropArea(final int width, final int height, final float targetAspectRatio) {
        final Size res;
        final int targetH = Math.round(width / targetAspectRatio);
        if (targetH <= height) {
            /* either full size or height is cropped */
            res = new Size(width, targetH);
        } else {
            /* width is cropped */
            res = new Size(Math.round(height * targetAspectRatio), height);
        }
        return res;
    }

    /**
     * This callback would handle all tear-down logic, and is called when the {@link VideoSession} is stopped.
     * Five possible ways for the video session to be stopped are:
     *
     * 1. {@link VideoSession.CloseReason#SESSION_CLOSED}
     * 2. {@link VideoSession.CloseReason#VIRTUAL_CAMERA_CONFIGURATION_CHANGED}
     * 3. {@link VideoSession.CloseReason#VIRTUAL_CAMERA_CONFIGURATION_REMOVED}
     * 4. {@link VideoSession.CloseReason#RENDERING_FAILED}
     * 5. {@link VideoSession.CloseReason#BASE_CAMERA_CONFIGURATION_CHANGED}
     *
     * @param reason is the reason for closing the video pipeline
     */
    @Override
    @SuppressWarnings("MagicNumber")
    protected void onVideoClosed(final VideoSession.CloseReason reason) {
        Log.i(LOGTAG, "onVideoClosed(): reason " + reason.name());
        /*
         * BASE_CAMERA_CONFIGURATION_CHANGED reason is only available in API v5 and above.
         */
        if (Build.VERSION.MAX_API >= 5) {
            /*
             * Checks whether the video session closing reason was BASE_CAMERA_CONFIGURATION_CHANGED.If yes, the video session is restarted.
             */
            if (reason == VideoSession.CloseReason.BASE_CAMERA_CONFIGURATION_CHANGED) {
                Log.i(LOGTAG, "onVideoClosed(): Triggering the restart of the video session that got closed due to " + reason.name());
                /*
                 * The video session is restarted due to base camera configuration changed
                 * in the main thread for proper rendering
                 */
                UI_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        /*
                         * Restarts the video session from the main thread/UI thread
                         */
                        startVideoSession();
                    }
                });
            }
        }
    }

    /**
     * Gets the default videoCapture and starts the video session.
     */
    @SuppressWarnings("MagicNumber")
    private void startVideoSession() {
        /*
         * Gets a default VideoCapture instance which does not scale, rotate, or modify the images received from the Video Pipeline.
         */
        final VideoCapture capture = mVideoManager.getDefaultVideoCapture();
        Log.d(LOGTAG, String.format("getDefaultVideoCapture() with width %d and height %d",
            capture.getWidth(), capture.getHeight()));
        /*
         * RefreshRate specifies how often the image should be updated, in this case 5 frames per second (FPS_5).
         */
        openVideo(capture, capture.getWidth() / 2, capture.getHeight() / 2, RefreshRate.FPS_5, false);
        Log.d(LOGTAG, "onVideoAvailable(): openVideo() is called and video session is started");
    }

    /**
     * Disconnects the {@link WebServerConnector} when service is destroyed. This method is triggered when the application
     * is stopped.
     */
    @Override
    public void onDestroy() {
        mWebServerConnector.disconnect();
        super.onDestroy();
    }
}

