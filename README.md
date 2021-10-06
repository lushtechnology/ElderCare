# TFLite Detector

This is the `TFLiteDetector` app. This application demonstrates how to use an Image from the Video Pipeline
and process it using a [TensorFlow lite](https://www.tensorflow.org/lite/guide) detector. In addition, it also
demonstrates how to create a front end web user interface for the application to display the images processed 
by the detector, and allows the user to configue the confidence threshold via a settings page.


This document assumes that [OpenJDK 8 is installed](https://openjdk.java.net/install/).

#### Tell gradle to use Security and Safety Things SDK

Set the following environment variables:

    export ANDROID_SDK_ROOT=<path_to_securityandsafetythingssdk>
    export ANDROID_NDK_HOME=<path_to_securityandsafetythingssdk>/ndk-bundle

### Building the TFLite Detector app

From the terminal, run:

    ./gradlew assembleDebug

### Install and run on Device

1. Obtain the generated APK file from: 

       ./app/build/outputs/apk/debug/app-debug.apk

2. Install the apk file on a Security and Safety Things Device using ADB
      
        adb install -r -g ./app/build/outputs/apk/debug/app-debug.apk
        
3. From a web browser visit `https://<ip_address_of_camera>:8443`       
4. Go to `Applications -> Overview` 
5. `TFLite Detector -> Go to application website`
6. Verify the video is being streamed
7. Verify that objects are being detected

### Generating a release APK

In order to generate the release APK, be sure to have your signing configuration setup as follows:

1. The following environment variables must be defined, for example:

        export SIGNING_KEY_ALIAS=keyalias # Choose any name for the signing key
        export SIGNING_KEY_PASSWORD=signingkeypassword # Choose a password for the signing key
        export SIGNING_KEYSTORE_PATH=~/key_name.keystore # Choose a path to store your keystore
        export SIGNING_KEYSTORE_PASSWORD=keystorepassword # Choose a password for the keystore

2. Run the following command to generate the keystore:

        keytool -genkey -v -keystore $SIGNING_KEYSTORE_PATH \
        -alias $SIGNING_KEY_ALIAS -keyalg RSA \-keysize 2048 \
        -validity 10000 -storepass $SIGNING_KEYSTORE_PASSWORD \
        -keypass $SIGNING_KEY_PASSWORD

    For more information on `keytool`, please refer to `man keytool` or [Oracle's keytool documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

3. From the terminal run:

        ./gradlew assembleRelease

### Model Selection

Identify the model that will be used in your app. For this example we used the
[ssd_mobilenet_v1_quantized](https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip)
model that comes from the TensorFlow Lite [Object Detection](https://www.tensorflow.org/lite/models/object_detection/overview)
examples. The model .zip file contains two files:
* detect.tflite
  * This is the model file we will use to run *inference*
    * Inference is the process through which a model creates output for an input. In our case the input is an image and
    the output is a set of bounding boxes, labels, and confidence values
* labelmap.txt
  * This provides the mapping from the numeric output of the model file to human readable categories (i.e. person, car,
  chair, etc.). For this model, if `n` is the output class prediction, then the label in row `n + 1` of labelmap.txt is
  the human readable label.

Be sure to move `detect.tflite` to `./app/src/main/assets/`. Also, move `labelmap.txt` to
`./app/src/main/res/raw/`. Create these directories if they do not already exist.

#### ObjectDetector

[ObjectDetector](./app/src/main/java/com/securityandsafetythings/examples/tflitedetector/detector/ObjectDetector.java)
is the class responsible for [preparing](#resourcehelper) and accessing a model. It is based on the example provided
by the TensorFlow team:
[TFLiteObjectDetectionAPIModel](https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/app/src/main/java/org/tensorflow/lite/examples/detection/tflite/TFLiteObjectDetectionAPIModel.java).

To simplify the creation of ObjectDetector we use
[ObjectDetectorBuilder](./app/src/main/java/com/securityandsafetythings/examples/tflitedetector/detector/ObjectDetectorBuilder.java)
which provides useful default values for most of the fields. For this app it's used in
[MainService](#mainservice) with
nearly all default values:
```java
mDetector = new ObjectDetectorBuilder(this)
    .setIsQuantized(true)
    .build();
```

ObjectDetector provides the api for running inference on a `Bitmap` retrieved from the Video Pipeline.
```java
public List<Recognition> recognizeImage(final Bitmap bitmap)
```
    
Finally, ObjectDetector provides a helper method `getRequiredImageSize` that returns a `Size` defining the height and
width of the image expected by the detector. This is useful when determining how the raw images will be processed. More 
on that available [here](#image-preprocessing).

#### ResourceHelper

[ResourceHelper](./app/src/main/java/com/securityandsafetythings/examples/tflitedetector/utilities/ResourceHelper.java)
is a helper class responsible for loading the model and its labels from app resources. It's used by
[ObjectDetector](#objectdetector) during initialization.

It provides two methods:
1. `loadModelFile`
2. `loadLabels`

#### MainService

Changes to 
[MainService](./app/src/main/java/com/securityandsafetythings/examples/tflitedetector/services/MainService.java) include
configuring and building a detector, [preprocessing](#image-preprocessing) the image, running [inference](#inference)
using the detector, and showing the [processed](#image-postprocessing) result image with the help of
[Renderer](#renderer).

The detector and size are initialized in `onCreate` along with other service setup tasks:
```java
mDetector = new ObjectDetectorBuilder(this)
    .setIsQuantized(true)
    .build();
mDetectorInputSize = mDetector.getRequiredImageSize();
```

##### Image Preprocessing

The example model operates on 300x300 pixel images. You may or may not recall that the image preview used in
`Helloworld` is full HD 1920x1080. We would not want to use just a small 300x300 crop of our preview image. Instead what
we can do is crop the full resolution image to the same aspect ratio as our model (1:1) and then rescale the cropped
image to the size our detector accepts. This way the resize operation does not warp the input, though this warping is
tolerable for some models, we won't use it now. Refer to `onImageAvailable` right up until `recognizeImage` is called:
Be sure to reference the [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap) documentation as well

##### Inference

For every bit as complex as preprocessing is, inference is simple:
```java
/*
 * Perform classification using the detector
 */
final List<Recognition> detectionResults = mDetector.recognizeImage(classificationCroppedBitmap);
```

##### Image Postprocessing

After we have generated a list of `Recognition` objects, we want to display them over our image. To do this we utilize
the [Renderer](#renderer) helper class. It performs all necessary rescaling and drawing on the image at the original HD
resolution.
```java
/*
 * Utility class renders the poses on imageBmp. We pass crop size, and base image size along with the
 * margins so that we know where to draw the relative coordinates from the detector on the base image
 */
Renderer.render(new Canvas(imageBmp),
    detectionResults,
    MIN_CONFIDENCE,
    cropSize.getWidth(),
    cropSize.getHeight(),
    imageBmp.getWidth(),
    imageBmp.getHeight(),
    marginX,
    marginY);
```
Finally, send the processed image to the rest endpoint just like in `Helloworld`

#### Renderer

[Renderer](./app/src/main/java/com/securityandsafetythings/examples/tflitedetector/utilities/Renderer.java) is a utility
class that performs drawing and manages paints. It even shades areas that were not used by our detector. It provides
only one method `render`.

A rectangle is drawn on each detected object. The color of said rectangle is determined by the
label of the object so that each class has a distinct color. The text label and confidence are rendered at the top of
each rectangle as well.

## References

1. `Helloworld` app
2. [ObjectDetector](#objectdetector) is based on the TensorFlow lite example 
[TFLiteDetectionAPIModel](https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/app/src/main/java/org/tensorflow/lite/examples/detection/tflite/TFLiteObjectDetectionAPIModel.java)
3. [DetectorActivity](https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/app/src/main/java/org/tensorflow/lite/examples/detection/DetectorActivity.java)
was used as reference for parts of the image preprocessing and rendering
4. More info on TFLite Object detection can be found at the [Object Detection Overview](https://www.tensorflow.org/lite/models/object_detection/overview)
