package com.cooperativeai.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.cooperativeai.R;
import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DateTimeManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class HomeFragment extends Fragment implements LocationListener {

    private TextView TextTemp, TextUsername, TextLevel, TextCoins, TextCurrentTime, TextCurrentLocation;
    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;    private Date lastUsedDate;
    private String lastUsedDateAsString;
    private Date currentDate;
    private int userCurrentLevel,level;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private String currentDateAsString,city,country,UserCurrentGoalCheck,CurrentCoins,CurrentWallet,Username;
    double wallet;
    private Timer timer;
    private MainStore mainStore;
    private CameraActivity cameraActivity;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        firebaseAuth = FirebaseAuth.getInstance();

        TextTemp = view.findViewById(R.id.texttemp);
        TextUsername = view.findViewById(R.id.home_username);
        TextCoins = view.findViewById(R.id.home_coins);
        TextLevel = view.findViewById(R.id.home_level);
        TextCurrentTime = view.findViewById(R.id.home_current_date_time);
        TextCurrentLocation = view.findViewById(R.id.home_current_location);


        ((MainActivity) getActivity()).getSupportActionBar().setTitle("Home");
        lastUsedDateAsString = SharedPreferenceManager.getLastUsedDate(getActivity());
        userCurrentLevel = SharedPreferenceManager.getUserLevel(getActivity());
        CurrentCoins = SharedPreferenceManager.getUserCoins(getActivity());
        CurrentWallet = SharedPreferenceManager.getUserWallet(getActivity());
        UserCurrentGoalCheck = SharedPreferenceManager.getUserGoalCheck(getActivity());
        currentDate = new Date();
        currentDateAsString = DateTimeManager.converDateToString(currentDate);
        if (UtilityMethods.isInternetAvailable())
        {

            if (lastUsedDateAsString.isEmpty())
                lastUsedDateAsString = currentDateAsString;
            lastUsedDate = DateTimeManager.convertStringToDate(lastUsedDateAsString);

            if (DateTimeManager.diffInDate(currentDate, lastUsedDate) > Constants.LEVEL_CHECK_DELAY) {
                userCurrentLevel = reduceLevelCount();
                DecreaeseGoal();
            }
            else
            {
                ConvertCoinToWalletSaveLastUsedDateSaveLevel();
                IcreaseGoalAndSaveData();
            }

            SaveInDatabase();
        }
        else
        {
            Toast.makeText(getContext(), "No internet connection available", Toast.LENGTH_SHORT).show();
        }

        TextUsername.setText("Welcome, " + SharedPreferenceManager.getUserUsername(getContext()));
        TextCurrentTime.setText(""+DateTimeManager.getMonthNameWithDate());
        TextCoins.setText("" + SharedPreferenceManager.getUserCoins(getActivity()));
        TextLevel.setText("" + SharedPreferenceManager.getUserLevel(getActivity()));



        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity()
                ,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        onLocationChanged(location);
        getlocation(location);




        OpenWeatherMapHelper helper = new OpenWeatherMapHelper(getString(R.string.openweatherapi));
        helper.setUnits(Units.METRIC);
        helper.setLang(Lang.ENGLISH);
        helper.getCurrentWeatherByGeoCoordinates(latitude,longitude, new CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {

                TextTemp.setText(""+currentWeather.getMain().getTempMax()+"° Celsius");

            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });

        return view;
    }


    private void ConvertCoinToWalletSaveLastUsedDateSaveLevel()
    {

        if (!lastUsedDateAsString.equals(currentDateAsString))

        {
            if (!CurrentCoins.equals("0")) {

                wallet = Double.valueOf((CurrentWallet));
                SharedPreferenceManager.ConvertCoinToWallet(getContext(),"add", wallet);
            }
            SharedPreferenceManager.setLastUsedDate(getContext(),currentDateAsString);
            SharedPreferenceManager.setUserLevel(getContext(),userCurrentLevel);

        }
        else
        {
            SharedPreferenceManager.setLastUsedDate(getContext(),currentDateAsString);
            SharedPreferenceManager.setUserLevel(getContext(),userCurrentLevel);
        }
    }

    private void DecreaeseGoal()
    {
        if (UserCurrentGoalCheck.isEmpty() || UserCurrentGoalCheck.equals("1"))
        SharedPreferenceManager.setUserGoalCheck(getActivity(),"");
    }

    private void IcreaseGoalAndSaveData()
    {

        if (!lastUsedDateAsString.equals(currentDateAsString))
                if (UserCurrentGoalCheck.isEmpty() || UserCurrentGoalCheck.equals("1")) {

                    if (UserCurrentGoalCheck.isEmpty())
                        UserCurrentGoalCheck = "0";

                    int Goal = Integer.parseInt(UserCurrentGoalCheck);
                    Goal += 1;
                    SharedPreferenceManager.setUserGoalCheck(getContext(), String.valueOf(Goal));

                }

    }

    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                mainStore.updateGps(latitude,longitude);
                cameraActivity = new CameraActivity(mainStore);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 10000);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void getlocation(Location location)
    {

        try {
            Geocoder geocoder = new Geocoder(getActivity());
            List<Address> addresses = null;
            addresses = geocoder.getFromLocation(latitude,longitude,1);

            city = addresses.get(0).getLocality();
            country = addresses.get(0).getCountryName();
            TextCurrentLocation.setText(""+city+", "+country);
            SharedPreferenceManager.setUserLocation(getActivity(),city+", "+country);


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(),"Error : "+e,Toast.LENGTH_SHORT).show();
        }
    }

    private int reduceLevelCount(){
        if (userCurrentLevel == 1){
            ConvertCoinToWalletSaveLastUsedDateSaveLevel();
            return userCurrentLevel;
        }
        else{
            long reduceCount = DateTimeManager.diffInDate(currentDate, lastUsedDate);
            userCurrentLevel -= reduceCount;

            if (userCurrentLevel <= 1)
                userCurrentLevel = 1;
            ConvertCoinToWalletSaveLastUsedDateSaveLevel();
            return userCurrentLevel;
        }

    }

    private void SaveInDatabase()
    {
        HashMap Usermap = new HashMap();
        Usermap.put("Lastuseddate",SharedPreferenceManager.getLastUsedDate(getActivity()));
        Usermap.put("Coins",SharedPreferenceManager.getUserCoins(getActivity()));
        Usermap.put("Level",SharedPreferenceManager.getUserLevel(getActivity()));
        Usermap.put("Wallet",SharedPreferenceManager.getUserWallet(getActivity()));
        Usermap.put("Goalcheck",SharedPreferenceManager.getUserGoalCheck(getActivity()));
        UsersDatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(Usermap);


    }

}
