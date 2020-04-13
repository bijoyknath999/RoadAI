package com.cooperativeai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.cooperativeai.activities.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Map extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Geocoder geo;
    private double latitude = 0.0,longitude = 0.0;
    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        if (mMap != null) {
            geo = new Geocoder(Map.this, Locale.getDefault());
            FusedLocationProviderClient client = new FusedLocationProviderClient(Map.this);
            client.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location!=null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                latLng = new LatLng(latitude,longitude);
                                try {
                                    if (geo == null)
                                        geo = new Geocoder(Map.this, Locale.getDefault());
                                    List<Address> address = geo.getFromLocation(latitude,longitude, 1);
                                    Toast.makeText(Map.this,""+latLng,Toast.LENGTH_LONG).show();
                                    if (address.size() > 0) {
                                        mMap.addMarker(new MarkerOptions().position(latLng).title(""+address.get(0).getAddressLine(0)));
                                    }
                                } catch (IOException ex) {
                                    if (ex != null)
                                        Toast.makeText(Map.this, "Error:" + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(Map.this, "Location Fetching failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                }

    }
}
