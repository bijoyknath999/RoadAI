

package com.cooperativeai.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cooperativeai.R;
import com.cooperativeai.statemanagement.MS;
import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DatabasePreferenceManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static maes.tech.intentanim.CustomIntent.customType;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    @BindView(R.id.textureView_camera_activity)
    TextureView textureView;
    @BindView(R.id.button_take_picture)
    ImageView buttonClick;

    private CameraManager cameraManager;
    private int cameraFacing,currentLevel;
    private Size previewSize;
    private String cameraId;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private File galleryFolder;
    private boolean hasWritePermission,hasCameraPermission;
    private boolean wasCreated;
    private Timer timer,timer2;
    private TextView AutoCapture;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private boolean hasLocationPermission;
    private DetectorActivity detectorActivity;
    private Bitmap image;
    private LocationManager locationManager;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    protected MainStore mainstore;
    private double lattitude,longitude,lat,lon;
    private Dialog noconnectionDialog;
    private ImageView mapBTN;
    private File storageDirectory;
    private GpsLocation gpsLocation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsLocation = new GpsLocation(CameraActivity.this);
        //get reference to the "Users" node
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        noconnectionDialog = UtilityMethods.showDialogAlert(CameraActivity.this, R.layout.dialog_box);
        FirebaseUser mUser = firebaseAuth.getCurrentUser();
        mUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        lattitude = getIntent().getDoubleExtra("lat",0.0);
                        longitude = getIntent().getDoubleExtra("lon",0.0);
                        System.out.println("AUTH it");
                        mainstore = new MainStore(idToken,lattitude,longitude);
                        MS.setMainStore(mainstore);
                    } else {
                        System.out.println("In fireuse token" + task.getException());
                    }
                });

        //check permission for camera,external storage and location manager
        if (Build.VERSION.SDK_INT>=23)
        {
            if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(CameraActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(CameraActivity.this,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(CameraActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(CameraActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(CameraActivity.this,new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);
            }
            else
            {
                hasCameraPermission = true;
                hasLocationPermission = true;
                hasWritePermission = true;
            }
        }
        wasCreated = false;

        final AssetManager mngr =getApplicationContext().getAssets();
        detectorActivity= new DetectorActivity(mngr, this, hasLocationPermission, mainstore);


        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        AutoCapture = findViewById(R.id.camera_auto_capture);
        mapBTN = findViewById(R.id.button_map);
        mapBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                {
                    startActivity(new Intent(CameraActivity.this,Map.class));
                    customType(CameraActivity.this, "fadein-to-fadeout");
                }
                else
                {
                    gpsLocation.showSettingsAlert();
                }
            }
        });
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                setupCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };


        //auto picture in every 10 second
        AutoCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                builder.setTitle("Turn on Auto?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(CameraActivity.this, "Auto Click Enabled Every 10 second", Toast.LENGTH_SHORT).show();
                        dialogInterface.dismiss();
                        SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_ON);
                        startAutoCapture();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_OFF);
                        Toast.makeText(CameraActivity.this, "Auto capture turned off. Click to capture", Toast.LENGTH_LONG).show();
                    }
                });
                builder.show();
            }
        });


        int autoCaptureStatus = SharedPreferenceManager.getAutoCaptureStatus(CameraActivity.this);

        if (autoCaptureStatus == Constants.AUTO_CAPTURE_DISABLED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Turn on Auto?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(CameraActivity.this, "Auto Click Enabled Every 10 second", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                    SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_ON);
                    startAutoCapture();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_OFF);
                    Toast.makeText(CameraActivity.this, "Auto capture turned off. Click to capture", Toast.LENGTH_LONG).show();
                }
            });
            builder.show();
        }

        //if user gps is not enabled it will display a settings alert or get user current location
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            gpsLocation.showSettingsAlert();
        }
        else
        {
            if (gpsLocation!=null)
            {
                lat = gpsLocation.getLatitude();
                lon = gpsLocation.getLongitude();
                TimerTask timerTask2 = new TimerTask() {
                    @Override
                    public void run() {
                        if(mainstore != null){
                            mainstore.updateGps(lat,lon);
                        }
                    }
                };
                timer2 = new Timer();
                timer2.scheduleAtFixedRate(timerTask2, 1, 10000);
            }
        }
    }

    //Timer
    //auto picture in every 10 second
    private void startAutoCapture()
    {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                clickPicture();
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 3, Constants.AUTO_CAPTURE_DELAY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                if (cameraId != null) {
                    cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice cameraDevice) {
                            CameraActivity.this.cameraDevice = cameraDevice;
                            createPreviewSession();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                            cameraDevice.close();
                            CameraActivity.this.cameraDevice = null;
                        }

                        @Override
                        public void onError(@NonNull CameraDevice cameraDevice, int i) {
                            cameraDevice.close();
                            CameraActivity.this.cameraDevice = null;
                        }
                    }, backgroundHandler);
                }
            } else {
                Toast.makeText(CameraActivity.this, "Permissions were not granted", Toast.LENGTH_SHORT).show();
            }
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                                CameraActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void setupCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (streamConfigurationMap != null) {
                        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                        this.cameraId = cameraId;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.i("TAG", "setupCamera: " + e.getMessage());
            Toast.makeText(CameraActivity.this, "Error occurred in setup", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 1:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    hasWritePermission = true;
                    hasLocationPermission = true;
                    hasCameraPermission = true;
                }
                else
                {
                    finishAffinity();
                }
        }
    }

    //create folder
    //if user android version greater than 9
    //created folder directory is storage/Android/data/com.cooperativeai/files/RoadAI
    //if android version less than 10
    //created folder directory is storage/Pictures/RoadAI
    private void createGalleryFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
           storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        else
        {
            storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e(TAG, "createGalleryFolder: Folder could not be created");
            }
        } else
            wasCreated = true;
    }

    @OnClick(R.id.button_take_picture)
    public void clickPicture() {
        if (hasWritePermission && hasCameraPermission)
        {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                takePicture();
            }
            else
            {
                gpsLocation.showSettingsAlert();
            }
        }
        else
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraActivity.this, "Permissions were not granted", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //if it reaches the daily coins limit,
    //it will not increase the coins value
    private void takePicture() {
        createGalleryFolder();
        if (wasCreated) {
            lock();
            FileOutputStream fileOutputStream = null;
            try {
                File file = createImageFile();
                if (file != null) {
                    fileOutputStream = new FileOutputStream(file);
                    image=textureView.getBitmap();
                    image.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                    previewHeight=image.getHeight();
                    previewWidth=image.getWidth();
                    detectorActivity.detection(image);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            Toast.makeText(CameraActivity.this, "Picture Captured", Toast.LENGTH_SHORT).show();
                            int level = SharedPreferenceManager.getUserLevel(CameraActivity.this);
                            String coins = SharedPreferenceManager.getUserCoins(CameraActivity.this);
                            if (level == 1 && coins.equals("10.0") || level == 2 && coins.equals("15.0") || level == 3 && coins.equals("20.0")
                                    || level == 4 && coins.equals("25.0") || level == 5 && coins.equals("30.0"))
                                Log.i(TAG, "Coin daily limit reached!!");
                          else
                            if (UtilityMethods.isInternetAvailable())
                            {
                                increaseCoinCount();
                            }
                            else {
                                noconnectionDialog.show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (noconnectionDialog.isShowing())
                                        {
                                            noconnectionDialog.dismiss();
                                        }
                                    }
                                },2500);
                            }
                        }
                    });
                    Log.i(TAG, "takePicture: " + file.getAbsolutePath());
                } else {
                    Toast.makeText(CameraActivity.this, "File could not be created", Toast.LENGTH_SHORT).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                unlock();
                try {
                    if (fileOutputStream != null)
                        fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(CameraActivity.this, "Folder could not be created", Toast.LENGTH_SHORT).show();
        }
    }


    //if user capture pic it will increase coins value by 0.04 until it reaches max daily limit
    //if user goal value 2 and user reaches max daily coin limits it will increase user level by 1
    //and user maxmimum level 5
    private void increaseCoinCount() {

        SharedPreferenceManager.changePictureCount(CameraActivity.this,"add",Constants.BASE_PICTURE_CAPTURE_COUNT);
        if (SharedPreferenceManager.changeCoinCount(CameraActivity.this, "add", Constants.BASE_COIN_COUNT)){

            String currentCoinCount = SharedPreferenceManager.getUserCoins(CameraActivity.this);
            String currentgoal = SharedPreferenceManager.getUserGoalCheck(CameraActivity.this);
            currentLevel = SharedPreferenceManager.getUserLevel(CameraActivity.this);

            double cointInt = Double.parseDouble(currentCoinCount);

            if (currentLevel == 1 && currentCoinCount.equals("10.0") && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else if (currentLevel == 2 && currentCoinCount.equals("15.0") && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else if (currentLevel == 3 && currentCoinCount.equals("20.0") && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else if (currentLevel == 4 && currentCoinCount.equals("25.0") && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else if (currentLevel == 5 && currentCoinCount.equals("30.0") && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else if (currentLevel == 5 && cointInt > 30.0 && currentgoal.equals("2"))
                UpdateGoalUpdateLevel(currentCoinCount,currentgoal,currentLevel);
            else
            {
                SaveDataDatabase();
            }

            SharedPreferenceManager.setUserLevel(CameraActivity.this, currentLevel);
        }
        else{
            Toast.makeText(CameraActivity.this, "Coin addition failed", Toast.LENGTH_SHORT).show();
        }

    }

    //create image file in external storage
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("ddmmyyyy", Locale.getDefault()).format(new Date());
        String imageFileName = "CoAi_" + timeStamp;
        try {
            return File.createTempFile(imageFileName, ".jpeg", galleryFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gpsLocation.showSettingsAlert();
        if (!textureView.isAvailable())
            textureView.setSurfaceTextureListener(surfaceTextureListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
        if (timer != null)
            timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer2 != null)
            timer2.cancel();
        mainstore.getConnection().desposeListeners();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }

    private void UpdateGoalUpdateLevel(String currentCoinCount, String currentgoal, int currentLevel)
    {

        if (currentLevel == 5){
            currentLevel = 5;
            SharedPreferenceManager.setUserGoalCheck(CameraActivity.this,"");
            SharedPreferenceManager.setUserLevel(CameraActivity.this, currentLevel);
            SaveDataDatabase();
        }
        else{
            currentLevel += 1;

            if (currentLevel >= 5)
                currentLevel = 5;
            SharedPreferenceManager.setUserGoalCheck(CameraActivity.this,"");
            SharedPreferenceManager.setUserLevel(CameraActivity.this, currentLevel);

            SaveDataDatabase();
        }

    }

    private void SaveDataDatabase()
    {
        DatabasePreferenceManager.setDataDatabase4value(CameraActivity.this,"Level",
                "Coins","Goalcheck","Pictures",
                SharedPreferenceManager.getUserLevel(CameraActivity.this),SharedPreferenceManager.getUserCoins(CameraActivity.this),
                SharedPreferenceManager.getUserGoalCheck(CameraActivity.this),SharedPreferenceManager.getUserTotalPicturesCapture(CameraActivity.this));
    }
}
