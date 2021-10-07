# ElderCare

This application detects elder people daily activities from camera field of view.
In addition, it also creates front end web user interface for the application to display the images processed 
by the detector. The app can detect daily activities like :

- Walking.
- Sleeping.
- Eating.
- Using Inhalers.
- Falling from chair.
- etc...

### Install and run on Device

1. Obtain the release APK file (app-release.apk) from Azena App Store

2. Install the apk file on Azena Device using ADB:     
        `adb install -r -g app-release.apk`
3. From a web browser visit `https://<ip_address_of_camera>:8443`
4. Go to `Applications -> Overview` 
5. `ElderCare -> Go to App interface and configurations`
6. Verify the video is being streamed
7. Verify that activities are being detected and displayed
8. Connect to app logs `adb shell logact`
9. Verify that logs are being printed, e.g.:
   
    `10-06 19:28:39.374  3774  3798 I MainService: Predicted Class sleeping, Score 0.225707`

### Integration

The app supports DataTrolley, MessageBroker integration. 

1. DataTrolley integration, your subscriber app can receive information in the form of FramePayload.
2. MessageBroker integration, you can configure IOT Gateway to listen to `sst:/metadata/json/com.lushtech.eldercare.activity/other/person` and receive messages in json format.

Further details are in accompained `messagebroker_and_datatrolley.pdf`
