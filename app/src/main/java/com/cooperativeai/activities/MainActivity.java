
package com.cooperativeai.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DateTimeManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kwabenaberko.openweathermaplib.constants.Lang;
import com.kwabenaberko.openweathermaplib.constants.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;
import com.luseen.spacenavigation.SpaceItem;
import com.luseen.spacenavigation.SpaceNavigationView;
import com.luseen.spacenavigation.SpaceOnClickListener;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements LocationListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Fragment selectedFragment = null;
    private DatabaseReference UserAcc;
    private double latitude,longitude,lon,lat;
    private boolean hasLocationPermission;
    private static final int LOCATION_REQUEST_CODE = 31;
    private String city,country;
    private LocationManager locationManager;
    private Dialog noconnectionDialog;
    private Dialog dialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpaceNavigationView spaceNavigationView = (SpaceNavigationView) findViewById(R.id.bottom_nav);
        spaceNavigationView.initWithSaveInstanceState(savedInstanceState);
        spaceNavigationView.addSpaceItem(new SpaceItem("Home", R.drawable.ic_home));
        spaceNavigationView.addSpaceItem(new SpaceItem("Profile", R.drawable.ic_profile));


        spaceNavigationView.setSpaceOnClickListener(new SpaceOnClickListener() {
            @Override
            public void onCentreButtonClick() {

                Intent CamIntent = new Intent(MainActivity.this, CameraActivity.class);
                CamIntent.putExtra("lat",latitude);
                CamIntent.putExtra("lon",longitude);
                startActivity(CamIntent);
                overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                dialog.show();

            }

            @Override
            public void onItemClick(int itemIndex, String itemName) {

                if (itemIndex==0)
                {
                    selectedFragment = new HomeFragment();
                }

                else if (itemIndex==1)
                {
                    selectedFragment = new ProfileFragment();

                }

                if (selectedFragment != null)
                {
                    getSupportFragmentManager().beginTransaction().replace(R.id.home_container,selectedFragment).commit();
                }

            }

            @Override
            public void onItemReselected(int itemIndex, String itemName) {

            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.main_toolbar);

        noconnectionDialog = UtilityMethods.showDialogAlert(MainActivity.this, R.layout.dialog_box);
        dialog = UtilityMethods.showDialog(MainActivity.this, R.layout.layout_loading_dialog);




        UserAcc = FirebaseDatabase.getInstance().getReference().child("Users");

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Home");
        if (savedInstanceState == null)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.home_container,
                    new HomeFragment()).commit();
        }
        toolbar.setTitleTextColor(ContextCompat.getColor(this,R.color.color1));
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.drawer_open,R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.color1));
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)

            {
                UserMenuSelector (menuItem);
                return false;
            }
        });


        getPermission(Constants.LOCATION_PERMISSION);

        if (hasLocationPermission) {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            onLocationChanged(location);
            getlocation(location);

            FusedLocationProviderClient client = new FusedLocationProviderClient(MainActivity.this);
            client.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            getlocation(location);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Location Fetching failed", Toast.LENGTH_SHORT).show();
                        }
                    });

        }
        else
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Permissions were not granted", Toast.LENGTH_SHORT).show();
                }
            });
        }


        OpenWeatherMapHelper helper = new OpenWeatherMapHelper(getString(R.string.openweatherapi));
        helper.setUnits(Units.METRIC);
        helper.setLang(Lang.ENGLISH);
        helper.getCurrentWeatherByGeoCoordinates(latitude,longitude, new CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {

                SharedPreferenceManager.setUserLocationTemp(MainActivity.this,""+currentWeather.getMain().getTempMax()+"° Celsius");
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }


    //Toolbar and Drawer

    @Override
    public boolean onOptionsItemSelected(MenuItem item)

    {

        switch (item.getItemId())
        {
            case R.id.toolbar_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Invitation From Road.Ai App");
                shareIntent.putExtra(Intent.EXTRA_TEXT ,"Road.Ai is world's first real-time road condition \nmonitoring app based on artificial inteligence. \nSo let's get started, if you are a new user signup, it's free!" +
                        "\nInvitation From Road.Ai App \nDownload Link : https://play.google.com/store/apps/details?id="+getPackageName());
                startActivity(shareIntent,null);
                break;
        }


        if (actionBarDrawerToggle.onOptionsItemSelected(item))

        {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Navigation

    private void UserMenuSelector(MenuItem menuItem)

    {
        switch (menuItem.getItemId())
        {
            case R.id.menu_about_us:
                break;
            case R.id.menu_follow_us:
                break;
            case R.id.menu_logout:
                FirebaseAuth.getInstance().signOut();
                setData();
                dialog.show();
                finish();
                break;
        }

    }

    private void setData()
    {
        String checkRem = SharedPreferenceManager.getSignRemember(MainActivity.this);
        if (checkRem.equals("yes"))
        {
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(Constants.PREFS_USER_NAME);
            editor.remove(Constants.PREFS_USER_USERNAME);
            editor.remove(Constants.PREFS_USER_ID);
            editor.remove(Constants.PREFS_USER_COIN_COUNT);
            editor.remove(Constants.PREFS_USER_WALLET);
            editor.remove(Constants.PREFS_USER_TOTAL_PICTURES);
            editor.remove(Constants.PREFS_USER_GOAL_CHECK);
            editor.remove(Constants.PREFS_USER_LAST_ACCESSED);
            editor.commit();
        }
        else
        {
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(Constants.PREFS_USER_EMAIL);
            editor.remove(Constants.PREFS_USER_PASSWORD);
            editor.remove(Constants.PREFS_USER_NAME);
            editor.remove(Constants.PREFS_USER_USERNAME);
            editor.remove(Constants.PREFS_USER_ID);
            editor.remove(Constants.PREFS_USER_COIN_COUNT);
            editor.remove(Constants.PREFS_USER_WALLET);
            editor.remove(Constants.PREFS_USER_TOTAL_PICTURES);
            editor.remove(Constants.PREFS_USER_GOAL_CHECK);
            editor.remove(Constants.PREFS_USER_LAST_ACCESSED);
            editor.commit();
        }
        startActivity(new Intent(MainActivity.this,WelcomePage.class));
        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
    }


    private void getPermission(String writePermission) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        else
            hasLocationPermission = true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasLocationPermission = true;
                startActivity(new Intent(MainActivity.this,MainActivity.class));

            }
            else
            {
                finishAffinity();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        lon = location.getLongitude();
        lat = location.getLatitude();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void getlocation(Location location)
    {

        try {
            Geocoder geocoder = new Geocoder(MainActivity.this);
            List<Address> addresses = null;
            addresses = geocoder.getFromLocation(lat,lon,1);

            city = addresses.get(0).getLocality();
            country = addresses.get(0).getCountryName();
            SharedPreferenceManager.setUserLocation(MainActivity.this,city+", "+country);


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"Error : "+e,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!UtilityMethods.isInternetAvailable())
        {
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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialog!= null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }
}
