

package com.cooperativeai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.cooperativeai.models.eventmodels.CoinConvertintoWalletModel;
import com.cooperativeai.models.eventmodels.CoinCountChangedModel;
import com.cooperativeai.models.eventmodels.PictureCountChangeModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

public class SharedPreferenceManager {
    private static SharedPreferences sharedPreferences;

    public static void saveUserRegistrationDetails(){
    }

    public static String getUserEmail(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_EMAIL, "");
    }

    public static String getUserFullName(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_NAME, "");
    }

    public static String getUserPassword(Context context)
    {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_PASSWORD,"");
    }

    public static String getUserUsername(Context context)
    {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_USERNAME,"");
    }

    public static String getUserLocation(Context context)
    {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_LOCATION,"");
    }
    public static String getSignRemember(Context context)
    {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_SIGN_IN_REMEMBER,"");
    }

    public static String getUserTotalPicturesCapture(Context context)
    {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_TOTAL_PICTURES,"0");
    }


    public static boolean ConvertCoinToWallet(Context context, String mod_type, double value){
        try {
            if (sharedPreferences == null)
                sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);

            double current_al = Double.parseDouble(sharedPreferences.getString(Constants.PREFS_USER_COIN_COUNT, "00000000.0"));

            double final_val;

            if (mod_type.equalsIgnoreCase("add"))
                final_val = value + current_al;
            else
                final_val = current_al - value;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREFS_USER_WALLET, String.valueOf(final_val));
            editor.putString(Constants.PREFS_USER_COIN_COUNT,"0");
            editor.apply();

            // Throw event every time. Respective catchers will automatically do their job
            CoinConvertintoWalletModel model = new CoinConvertintoWalletModel();
            model.setWalletcoin_val(final_val);
            EventBus.getDefault().post(model);

            return true;
        }
        catch (Exception e){
            return false;
        }
    }



    public static boolean changeCoinCount(Context context, String mod_type, double value){
        try {
            if (sharedPreferences == null)
                sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);

            double current_al = Double.parseDouble(sharedPreferences.getString(Constants.PREFS_USER_COIN_COUNT, "00000000.0"));

            double final_val;

            if (mod_type.equalsIgnoreCase("add"))
                final_val = current_al + value;
            else
                final_val = current_al - value;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREFS_USER_COIN_COUNT, String.valueOf(final_val));
            editor.apply();

            // Throw event every time. Respective catchers will automatically do their job
            CoinCountChangedModel model = new CoinCountChangedModel();
            model.setCoin_val(final_val);
            EventBus.getDefault().post(model);

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public static boolean changePictureCount(Context context, String mod_type, int value){
        try {
            if (sharedPreferences == null)
                sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);

            int current_al = Integer.parseInt(sharedPreferences.getString(Constants.PREFS_USER_TOTAL_PICTURES, "0"));

            int final_val;

            if (mod_type.equalsIgnoreCase("add"))
                final_val = current_al + value;
            else
                final_val = current_al - value;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREFS_USER_TOTAL_PICTURES, String.valueOf(final_val));
            editor.apply();

            // Throw event every time. Respective catchers will automatically do their job
            PictureCountChangeModel model = new PictureCountChangeModel();
            model.setPicture_val(final_val);
            EventBus.getDefault().post(model);

            return true;
        }
        catch (Exception e){
            return false;
        }
    }


    public static String getUserCoins(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_COIN_COUNT, "0");
    }

    public static String getUserWallet(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_WALLET, "0");
    }

    public static String getLastUsedDate(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_LAST_ACCESSED, "");
    }

    public static String getUserGoalCheck(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREFS_USER_GOAL_CHECK, "");
    }

    public static void setUserGoalCheck(Context context, String GoalAsString){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_GOAL_CHECK, GoalAsString);
        editor.apply();
    }

    public static void setLastUsedDate(Context context, String dateAsString){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_LAST_ACCESSED, dateAsString);
        editor.apply();
    }

    public static void setUserLocation(Context context, String Value){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_LOCATION, Value);
        editor.apply();
    }

    public static int getUserLevel(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.PREFS_USER_CURRENT_LEVEL, Constants.DEFAULT_USER_LEVEL);
    }

    public static void setUserLevel(Context context, int level){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREFS_USER_CURRENT_LEVEL, level);
        editor.apply();
        editor.commit();
    }

    public static void setUserWallet(Context context, String value){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_WALLET, value);
        editor.apply();
    }

    public static void setUserCoins(Context context, String value){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFS_USER_COIN_COUNT, value);
        editor.apply();
    }
    public static int getAutoCaptureStatus(Context context){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Constants.PREF_AUTO_CAPTURE_PREF, Constants.AUTO_CAPTURE_DISABLED);
    }

    public static void setAutoCaptureStatus(Context context, int status){
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_AUTO_CAPTURE_PREF, status);
        editor.apply();
    }



    public static void setDataDatabaseString(Context context,String DatabasePath1,String Val1)
    {
        DatabaseReference UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UsersDatabaseRef.child(UID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    UsersDatabaseRef.child(UID).child(DatabasePath1).setValue(Val1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void setDataDatabaseInt(Context context,String DatabasePath1,int Val1)
    {
        DatabaseReference UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UsersDatabaseRef.child(UID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    UsersDatabaseRef.child(UID).child(DatabasePath1).setValue(Val1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void setDataDatabase4value(Context context,String DatabasePath1,String DatabasePath2,String DatabasePath3,String DatabasePath4,int Val1,String Val2,String Val3,String Val4)
    {
        DatabaseReference UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UsersDatabaseRef.child(UID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    UsersDatabaseRef.child(UID).child(DatabasePath1).setValue(Val1);
                    UsersDatabaseRef.child(UID).child(DatabasePath2).setValue(Val2);
                    UsersDatabaseRef.child(UID).child(DatabasePath3).setValue(Val3);
                    UsersDatabaseRef.child(UID).child(DatabasePath4).setValue(Val4);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
