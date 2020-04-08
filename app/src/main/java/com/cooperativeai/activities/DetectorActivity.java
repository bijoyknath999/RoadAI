/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.activities;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cooperativeai.env.ImageUtils;
import com.cooperativeai.env.Logger;
import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.statemanagement.StateProps.BoundingBox;
import com.cooperativeai.statemanagement.StateProps.Distress;
import com.cooperativeai.statemanagement.StateProps.GpsLatLon;
import com.cooperativeai.tflite.Classifier;
import com.cooperativeai.tflite.TFLiteObjectDetectionAPIModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


import java.io.IOException;
import java.util.List;





class DetectorActivity {
    public static final Logger LOGGER = new Logger();
    //Configuration values for the prepackaged SSD model
    private static final String TAG = "DetectorActivity";
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final int rotation = 90;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private Classifier detector;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;


    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;
    private Context context;

    private long timestamp = 0;
    private int numofThreads = 3;

    int previewWidth = 480;
    int previewHeight = 640;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private boolean hasLocationPermission;
    private double latitude = 0.0;
    private double longitude = 0.0;
    MainStore mainStore;

    DetectorActivity(final AssetManager mngr, final Context context, boolean hasLocationPermission, MainStore mainstore) {
        this.context = context;
        detectorsetup(mngr);
        this.hasLocationPermission = hasLocationPermission;
        this.mainStore=mainstore;
    }

    protected void detectorsetup(final AssetManager mngr) {
        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    mngr,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(context.getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);
        this.setNumThreads(numofThreads);

        int sensorOrientation = rotation - 90;
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    }


    public void sendtoDistressServer(final Classifier.Recognition result)
    {
        String distress = "";
        int severity = 0;
        double classScore = 0;
        distress=result.getTitle();
        classScore=result.getConfidence()*100;
        final RectF location = result.getLocation();
        BoundingBox boundingbox= new BoundingBox(location.left, location.top, location.right, location.bottom);
        GpsLatLon gps = new GpsLatLon(latitude,longitude);
        mainStore.addDistress(new Distress(gps, distress, severity, classScore, boundingbox));
    }

    protected void detection(Bitmap image) {
        ++timestamp;
        final long currTimestamp = timestamp;

        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");
        int cropSize = TF_OD_API_INPUT_SIZE;

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        90, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(image, frameToCropTransform, null);
        //Toast.makeText(context.getApplicationContext(),"HEllo"+croppedBitmap.getHeight()+croppedBitmap.getWidth(),Toast.LENGTH_LONG).show();
        openBackgroundThread();
        LOGGER.i("Running detection on image " + currTimestamp);
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        FusedLocationProviderClient client = new FusedLocationProviderClient(context);
        client.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Location Fetching failed", Toast.LENGTH_SHORT).show();
                    }
                });
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                try {
                    //ListDistress.put(jsonCreate(result));
                    sendtoDistressServer(result);
                } catch (Exception e) {
                }
            }
        }
        computingDetection = false;
        closeBackgroundThread();
    }


    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }


//  protected void setUseNNAPI(final boolean isChecked) {
//    runInBackground(() -> detector.setUseNNAPI(isChecked));
//  }

    protected void setNumThreads(final int numThreads) {
        detector.setNumThreads(numThreads);
    }

    private enum DetectorMode {
        TF_OD_API;
    }

//  public int getScreenOrientation() {
//    switch (getWindowManager().getDefaultDisplay().getRotation()) {
//      case Surface.ROTATION_270:
//        return 270;
//      case Surface.ROTATION_180:
//        return 180;
//      case Surface.ROTATION_90:
//        return 90;
//      default:
//        return 0;
//    }
//  }

    public synchronized void runInBackground(final Runnable r) {
        if (backgroundHandler != null) {
            backgroundHandler.post(r);
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }


}

