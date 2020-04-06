

package com.cooperativeai.activities;

import android.Manifest;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cooperativeai.R;
import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 11;
    private static final int WRITE_REQUEST_CODE = 21;
    private static final int LOCATION_REQUEST_CODE = 31;

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
    private boolean hasWritePermission;
    private boolean wasCreated;
    private Timer timer;
    private TextView AutoCapture;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private String GoalCheck;
    private boolean hasLocationPermission;
    private DetectorActivity detectorActivity;
    private Bitmap image;

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private int[] intValues;

    protected MainStore mainstore;


    CameraActivity(MainStore mainStore)
    {
        this.mainstore = mainStore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);


        getPermission(Constants.CAMERA_PERMISSION);
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        firebaseAuth = FirebaseAuth.getInstance();

        hasWritePermission = false;
        wasCreated = false;

        final AssetManager mngr =getApplicationContext().getAssets();
        detectorActivity= new DetectorActivity(mngr, this, hasLocationPermission, mainstore);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        AutoCapture = findViewById(R.id.camera_auto_capture);
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

        AutoCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                builder.setTitle("Turn on Auto?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(CameraActivity.this, "Auto Click Enabled Every 1 minute", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(CameraActivity.this, "Auto Click Enabled Every 1 minute", Toast.LENGTH_SHORT).show();
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


    }

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
                getPermission(Constants.WRITE_PERMISSION);
            }
        } catch (CameraAccessException e) {
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
        } catch (CameraAccessException e) {
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
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("TAG", "setupCamera: " + e.getMessage());
            Toast.makeText(CameraActivity.this, "Error occurred in setup", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPermission(String writePermission) {
        if (writePermission.equalsIgnoreCase(Constants.CAMERA_PERMISSION)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else if (writePermission.equalsIgnoreCase(Constants.WRITE_PERMISSION)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
            else
                hasWritePermission = true;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        else
            hasLocationPermission = true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        } else if (requestCode == WRITE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasWritePermission = true;
            }
            else if (requestCode == LOCATION_REQUEST_CODE){
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasLocationPermission = true;
                }
            }
        }
    }

    private void createGalleryFolder() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
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
        getPermission(Constants.WRITE_PERMISSION);
        if (hasWritePermission) {

            int level = SharedPreferenceManager.getUserLevel(CameraActivity.this);
            String coins = SharedPreferenceManager.getUserCoins(CameraActivity.this);
            if (level == 1 && coins.equals("10.0"))
                Toast.makeText(CameraActivity.this,"Coin daily limit reached!!",Toast.LENGTH_LONG).show();
            else if (level == 2 && coins.equals("15.0"))
                Toast.makeText(CameraActivity.this,"Coin daily limit reached!!",Toast.LENGTH_LONG).show();
            else if (level == 3 && coins.equals("20.0"))
                Toast.makeText(CameraActivity.this,"Coin daily limit reached!!",Toast.LENGTH_LONG).show();
            else if(level == 4 && coins.equals("25.0"))
                Toast.makeText(CameraActivity.this,"Coin daily limit reached!!",Toast.LENGTH_LONG).show();
            else if (level == 5 && coins.equals("30.0"))
                Toast.makeText(CameraActivity.this,"Coin daily limit reached!!",Toast.LENGTH_LONG).show();
            else
            if (UtilityMethods.isInternetAvailable())
            {
                takePicture();
            }
            else {
                Toast.makeText(CameraActivity.this, "No internet connection available", Toast.LENGTH_SHORT).show();
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
                        public void run() {

                            increaseCoinCount();
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

    private void increaseCoinCount() {

        SharedPreferenceManager.changePictureCount(CameraActivity.this,"add",Constants.BASE_PICTURE_CAPTURE_COUNT);
        Toast.makeText(CameraActivity.this, "Picture Captured", Toast.LENGTH_SHORT).show();
        if (SharedPreferenceManager.changeCoinCount(CameraActivity.this, "add", Constants.BASE_COIN_COUNT)){

            String currentCoinCount = SharedPreferenceManager.getUserCoins(CameraActivity.this);
            String currentgoal = SharedPreferenceManager.getUserGoalCheck(CameraActivity.this);
            currentLevel = SharedPreferenceManager.getUserLevel(CameraActivity.this);

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
            else
            {
                Toast.makeText(CameraActivity.this,"test1",Toast.LENGTH_SHORT).show();
                SaveDataDatabase();
            }

            SharedPreferenceManager.setUserLevel(CameraActivity.this, currentLevel);
        }
        else{
            Toast.makeText(CameraActivity.this, "Coin addition failed", Toast.LENGTH_SHORT).show();
        }

    }

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
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
        if (timer != null)
            timer.cancel();
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
        startActivity(new Intent(CameraActivity.this, MainActivity.class));
        finish();
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
        SharedPreferenceManager.setDataDatabase4value(CameraActivity.this,"Level",
                "Coins","Goalcheck","Pictures",
                SharedPreferenceManager.getUserLevel(CameraActivity.this),SharedPreferenceManager.getUserCoins(CameraActivity.this),
                SharedPreferenceManager.getUserGoalCheck(CameraActivity.this),SharedPreferenceManager.getUserTotalPicturesCapture(CameraActivity.this));
    }

}
