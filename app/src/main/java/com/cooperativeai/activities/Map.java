package com.cooperativeai.activities;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.app.ActionBar;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Display;
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
    private double latitude = 0.0,longitude = 0.0;
    private LatLng latLng;
    private ActionBar actionBar;
    private Context context;
    MainStore mainstore = MS.mainStore;
    MarkerOptions markerOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        actionBar = getActionBar();
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);

        OnPiP();

        if (mainstore!=null)
        {
            mainstore.getConnection().getSocket().on("MAP_RESPONSE", onMapResponse);
            mainstore.getDataForMap();
        }
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

    private Emitter.Listener onMapResponse = (args -> {
        new Thread(() -> {
            LinkedList<Distress> distressList = getList((String) args[0],Distress.class);
            if (distressList!=null)
            for (int i = 0; i<distressList.size();i++)
            {
                GpsLatLon gps = distressList.get(i).getGps();
                latitude = gps.getLat();
                longitude = gps.getLon();
                latLng = new LatLng(latitude, longitude);
                markerOptions = new MarkerOptions().position(latLng).title("");
            }
            System.out.println(args[0]);
        }).start();
    });

    public <Distress> LinkedList<Distress> getList(String jsonArray, Class<Distress> clazz) {
        Type typeOfT = TypeToken.getParameterized(LinkedList.class, clazz).getType();
        return new Gson().fromJson(jsonArray, typeOfT);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        if (markerOptions!=null) {
            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainstore!=null)
        mainstore.getConnection().getSocket().off("MAP_RESPONSE",onMapResponse);
    }

    @Override
    protected void onStart() {
        super.onStart();
        OnPiP();
    }

    @Override
    protected void onPause() {
        super.onPause();
        OnPiP();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        OnPiP();
    }
}
