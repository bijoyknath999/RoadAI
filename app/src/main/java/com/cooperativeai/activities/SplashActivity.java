
package com.cooperativeai.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {


    private DatabaseReference UsersDatabaseRef;
    private String email, password, fullname,username,coins,wallet,pictures,lastuseddate,goalcheck;
    private int level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        if (Build.VERSION.SDK_INT>15)
        {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);

        }

        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                SharedPreferences sharedPreferences = getSharedPreferences("sharedPreferences", MODE_PRIVATE);
                final boolean firstTime = sharedPreferences.getBoolean("firstTime", true);
                if (firstTime) {
                    Intent IntroIntent = new Intent(SplashActivity.this, Intro.class);
                    IntroIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(IntroIntent);
                    finish();
                }
                else{
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    if (currentUser == null)
                    {
                        SendUserSignInActivity();
                    }
                    else
                    {
                        if (UtilityMethods.isInternetAvailable())
                        {
                            CheckUserExistence();
                        }
                        else
                        {
                            CheckUserExistence();
                            Toast.makeText(SplashActivity.this, "No internet connection available", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }, 1000);
    }

    private void CheckUserExistence()
    {
        final String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UsersDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (!dataSnapshot.hasChild(current_user_id))
                {
                    SendUserSetupAccount();
                }
                else
                {
                    SendUserMainActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserSignInActivity()
    {
        Intent SignAct = new Intent(SplashActivity.this, WelcomePage.class);
        SignAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(SignAct);
        finish();
    }

    private void SendUserMainActivity()
    {
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UsersDatabaseRef.child(UID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && !databaseList().equals(null)) {
                        fullname = dataSnapshot.child("Fullname").getValue().toString();
                        username = dataSnapshot.child("Username").getValue().toString();
                        coins = dataSnapshot.child("Coins").getValue().toString();
                        wallet = dataSnapshot.child("Wallet").getValue().toString();
                        pictures = dataSnapshot.child("Pictures").getValue().toString();
                        lastuseddate = dataSnapshot.child("Lastuseddate").getValue().toString();
                        goalcheck = dataSnapshot.child("Goalcheck").getValue().toString();
                        level = dataSnapshot.child("Level").getValue(Integer.class);
                        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(Constants.PREFS_USER_EMAIL, email);
                        editor.putString(Constants.PREFS_USER_NAME, fullname);
                        editor.putString(Constants.PREFS_USER_USERNAME, username);
                        editor.putString(Constants.PREFS_USER_ID, UID);
                        editor.putString(Constants.PREFS_USER_COIN_COUNT, coins);
                        editor.putString(Constants.PREFS_USER_WALLET, wallet);
                        editor.putString(Constants.PREFS_USER_TOTAL_PICTURES, pictures);
                        editor.putString(Constants.PREFS_USER_GOAL_CHECK, goalcheck);
                        editor.putString(Constants.PREFS_USER_LAST_ACCESSED, lastuseddate);
                        editor.apply();
                        SharedPreferenceManager.setUserLevel(SplashActivity.this, level);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    String message = databaseError.getMessage();
                    Toast.makeText(SplashActivity.this, "Error Occured :" + message, Toast.LENGTH_SHORT).show();
                }
            });

        Intent NextIntent = new Intent(SplashActivity.this, MainActivity.class);
        NextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(NextIntent);
        finish();

    }

    private void SendUserSetupAccount()
    {
        Intent SetupIntent = new Intent(SplashActivity.this, SetupAccount.class);
        SetupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(SetupIntent);
        finish();
    }
}
