package com.cooperativeai.activities;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.app.ActionBar;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.cooperativeai.R;
import com.cooperativeai.communication.SocketConnection;
import com.cooperativeai.statemanagement.MS;
import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.statemanagement.StateProps.Distress;
import com.cooperativeai.statemanagement.StateProps.GpsLatLon;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kwabenaberko.openweathermaplib.models.common.Main;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import io.socket.emitter.Emitter;

public class Map extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Geocoder geo;
    private double latitude = 0.0,longitude = 0.0,latitude2= 0.0,longitude2 = 0.0;
    private LatLng latLng, latLng2;
    private ActionBar actionBar;
    private Context context;
    MainStore mainstore = MS.mainStore;
    MarkerOptions markerOptions;
    private static final String TAG = "Map";
    private GpsLocation gpsLocation;
    private LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsLocation = new GpsLocation(Map.this);
        actionBar = getActionBar();
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gpsLocation.showSettingsAlert();
        else
            OnPiP();
    }

    private void OnPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;*/
            Rational aspectratio = new Rational(5,8);
            PictureInPictureParams.Builder mPictureInPicture = new PictureInPictureParams.Builder();
            mPictureInPicture.setAspectRatio(aspectratio).build();
            enterPictureInPictureMode(mPictureInPicture.build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    private Emitter.Listener onMapResponse = args -> {
        runOnUiThread(() -> {
            LinkedList<Distress> distressList = getList((String) args[0],Distress.class);
            System.out.println(args[0]);
            if (distressList!=null)
                for (int i = 0; i<distressList.size();i++)
                {
                    GpsLatLon gps = distressList.get(i).getGps();
                    String distress = distressList.get(i).getDistress();
                    latitude = gps.getLat();
                    longitude = gps.getLon();
                    latLng = new LatLng(latitude, longitude);
                    Bitmap bitmap;
                    if (distress.equals("pothole"))
                    {
                        bitmap = getMarkerBitmapFromView(R.drawable.ic_circle_pothole);;
                    }
                    else if (distress.equals("crack"))
                    {
                        bitmap = getMarkerBitmapFromView(R.drawable.ic_circle_crack);;
                    }
                    else if (distress.equals("ravelling"))
                    {
                        bitmap = getMarkerBitmapFromView(R.drawable.ic_circle_ravelling);;
                    }
                    else
                    {
                        bitmap = getMarkerBitmapFromView(R.drawable.ic_circle_patche);;
                    }
                    mMap.addMarker(new MarkerOptions().position(latLng).title("").icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                }
        });
    };

    public <Distress> LinkedList<Distress> getList(String jsonArray, Class<Distress> clazz) {
        Type typeOfT = TypeToken.getParameterized(LinkedList.class, clazz).getType();
        return new Gson().fromJson(jsonArray, typeOfT);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        geo = new Geocoder(Map.this, Locale.getDefault());
         if (gpsLocation!=null) {
                latitude2 = gpsLocation.getLatitude();
                longitude2 = gpsLocation.getLongitude();
            }

        try {
            if (geo == null)
                geo = new Geocoder(Map.this, Locale.getDefault());
            List<Address> address = geo.getFromLocation(latitude2,longitude2, 1);
            latLng2 = new LatLng(latitude2,longitude2);
            mMap.addMarker(new MarkerOptions().position(latLng2).title(address.get(0).getAddressLine(0)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng2));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng2));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng2, 15));
        } catch (IOException ex) {
            if (ex != null)
                Toast.makeText(Map.this, "Error:" + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
        }
        if (mainstore!=null && mMap!=null)
        {
            mainstore.getConnection().getSocket().on("MAP_RESPONSE", onMapResponse);
            mainstore.getDataForMap();
        }

    }

    private Bitmap getMarkerBitmapFromView(@DrawableRes int resId) {

        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_custom_marker, null);
        ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        markerImageView.setImageResource(resId);
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainstore!=null)
            mainstore.getConnection().getSocket().off("MAP_RESPONSE",onMapResponse);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gpsLocation.showSettingsAlert();
        else
            OnPiP();
    }

    @Override
    public void onBackPressed() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gpsLocation.showSettingsAlert();
        else
            OnPiP();
    }
}