
package com.cooperativeai.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
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
    private double latitude = 0.0,longitude = 0.0,lon = 0.0,lat = 0.0;
    private boolean hasLocationPermission;
    private static final int LOCATION_REQUEST_CODE = 31;
    private String city,country;
    private LocationManager locationManager;
    private Dialog noconnectionDialog;
    private GpsLocation gpsLocation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT>=29)
        {
            if (
                    ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);
            }
            else
            {
                hasLocationPermission = true;
            }
        }
        else if (Build.VERSION.SDK_INT>=23)
        {
            if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);
            }
            else
            {
                hasLocationPermission = true;
            }
        }

        gpsLocation = new GpsLocation(MainActivity.this);

        //Firebase Database Reference
        UserAcc = FirebaseDatabase.getInstance().getReference().child("Users");
        noconnectionDialog = UtilityMethods.showDialogAlert(MainActivity.this, R.layout.dialog_box);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Home");
        toolbar.setTitleTextColor(ContextCompat.getColor(this,R.color.color1));
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.drawer_open,R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.color1));
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);

        //For Bottom Navigation
        SpaceNavigationView spaceNavigationView = (SpaceNavigationView) findViewById(R.id.bottom_nav);
        spaceNavigationView.initWithSaveInstanceState(savedInstanceState);
        spaceNavigationView.addSpaceItem(new SpaceItem("Home", R.drawable.ic_home));
        spaceNavigationView.addSpaceItem(new SpaceItem("Profile", R.drawable.ic_profile));

        //For Drawer Navigation
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)

            {
                UserMenuSelector (menuItem);
                return false;
            }
        });


        if (hasLocationPermission)
        {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                gpsLocation.showSettingsAlert();
            }
            else
            {
                onLocationChanged(location);
                latitude = gpsLocation.getLatitude();
                longitude = gpsLocation.getLongitude();
            }
        }
        else
        {
            Toast.makeText(MainActivity.this, "Permissions were not granted", Toast.LENGTH_SHORT).show();
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

        spaceNavigationView.setSpaceOnClickListener(new SpaceOnClickListener() {
            @Override
            public void onCentreButtonClick() {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Intent CamIntent = new Intent(MainActivity.this, CameraActivity.class);
                    CamIntent.putExtra("lat", latitude);
                    CamIntent.putExtra("lon", longitude);
                    startActivity(CamIntent);
                    overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_exit);
                }
                else
                {
                    gpsLocation.showSettingsAlert();
                }

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

        //Set Default Fragment Activity
        if (savedInstanceState == null)
        getSupportFragmentManager().beginTransaction().replace(R.id.home_container,
                new HomeFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

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

    private void UserMenuSelector(MenuItem menuItem)

    {
        switch (menuItem.getItemId())
        {
            case R.id.menu_market_place:
                Toast.makeText(MainActivity.this,"Coming soon",Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_about_us:
                showdialog();
                break;
            case R.id.menu_follow_us:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/roadai1")));
                break;
            case R.id.menu_logout:
                logout();
                break;
        }

    }

    private void logout()
    {
        FirebaseAuth.getInstance().signOut();
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
        editor.remove(Constants.PREFS_USER_CURRENT_LEVEL);
        String remembercheck = SharedPreferenceManager.getSignRemember(MainActivity.this);
        if (remembercheck.equals("no"))
        {
            editor.remove(Constants.PREFS_SIGN_IN_REMEMBER);
            editor.remove(Constants.PREFS_USER_EMAIL);
            editor.remove(Constants.PREFS_USER_PASSWORD);
        }
        editor.commit();
        startActivity(new Intent(MainActivity.this,WelcomePage.class));
        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
        finish();
    }

    private void showdialog()
    {
        AlertDialog.Builder alertdialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            alertdialog = new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert);
        }
        else
        {
            alertdialog = new AlertDialog.Builder(this);
        }

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.about_us,null);

        alertdialog.setView(view);
        alertdialog.setCancelable(true);
        AlertDialog dialog2 = alertdialog.create();
        dialog2.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog2.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 1:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    hasLocationPermission = true;
                }
                else
                {
                    finishAffinity();
                }
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        if (location!=null) {
            lon = location.getLongitude();
            lat = location.getLatitude();

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

    //Checking net connection when app will open
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
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gpsLocation.showSettingsAlert();
    }
}
