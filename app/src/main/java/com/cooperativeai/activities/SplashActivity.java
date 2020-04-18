
package com.cooperativeai.activities;

import android.app.Dialog;
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
import com.timqi.sectorprogressview.ColorfulRingProgressView;

import static maes.tech.intentanim.CustomIntent.customType;

public class SplashActivity extends AppCompatActivity {


    private DatabaseReference UsersDatabaseRef;
    private String email, password, fullname,username,coins,wallet,pictures,lastuseddate,goalcheck;
    private int level;
    private Dialog noconnectionDialog;


    private ColorfulRingProgressView crpv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        //set activity full screen
        if (Build.VERSION.SDK_INT>15)
        {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);

        }

        noconnectionDialog = UtilityMethods.showDialogAlert(SplashActivity.this, R.layout.dialog_box);
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");

        crpv = (ColorfulRingProgressView) findViewById(R.id.crpv);
        crpv.animateIndeterminate();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                SharedPreferences sharedPreferences = getSharedPreferences("sharedPreferences", MODE_PRIVATE);
                final boolean firstTime = sharedPreferences.getBoolean("firstTime", true);
                if (firstTime)
                {
                    Intent IntroIntent = new Intent(SplashActivity.this, Intro.class);
                    IntroIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(IntroIntent);
                    finish();
                    overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                }
                else
                    {
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
                }
            }
        }, 2000);
    }

    //Check User data already exists in firebase database or not
    private void CheckUserExistence()
    {
        final String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UsersDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.hasChild(current_user_id))
                {
                    SendUserMainActivity();
                }
                else
                {
                    SendUserSetupAccount();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Send user to welcomepage
    private void SendUserSignInActivity()
    {
        Intent SignAct = new Intent(SplashActivity.this, WelcomePage.class);
        SignAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(SignAct);
        finish();
        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
    }

    //Send user to MainActivity and get values from firebase database ,update all SharedPreferences value
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
                        editor.commit();
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
        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);

    }

    //Send user to account setup activity
    private void SendUserSetupAccount()
    {
        Intent SetupIntent = new Intent(SplashActivity.this, SetupAccount.class);
        SetupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(SetupIntent);
        finish();
        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
    }
}
