package com.cooperativeai.activities;

import android.app.Dialog;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DateTimeManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Date;
import java.util.HashMap;

public class HomeFragment extends Fragment {

    private TextView TextTemp, TextUsername, TextLevel, TextCoins, TextCurrentTime, TextCurrentLocation;
    private Date lastUsedDate;
    private String lastUsedDateAsString;
    private Date currentDate;
    private int userCurrentLevel;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private String currentDateAsString,UserCurrentGoalCheck,CurrentCoins,CurrentWallet,Username;
    private double wallet;
    private Dialog noconnectionDialog;

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

        noconnectionDialog = UtilityMethods.showDialogAlert(getContext(), R.layout.dialog_box);

        //Set Toolbar Title
        ((MainActivity) getActivity()).getSupportActionBar().setTitle("Home");

        //get values from sharedpreference
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
            noconnectionDialog.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (noconnectionDialog.isShowing())
                    {
                        noconnectionDialog.dismiss();
                    }
                }
            },2500);        }

        //set all values in textview which is from SharedPreference
        TextUsername.setText("Welcome, " + SharedPreferenceManager.getUserUsername(getContext()));
        TextCurrentTime.setText(""+DateTimeManager.getMonthNameWithDate());
        TextCoins.setText("" + SharedPreferenceManager.getUserCoins(getActivity()));
        TextLevel.setText("" + SharedPreferenceManager.getUserLevel(getActivity()));
        TextTemp.setText(""+SharedPreferenceManager.getUserLocationTemp(getContext()));
        TextCurrentLocation.setText(""+SharedPreferenceManager.getUserLocation(getContext()));

        return view;
    }


    //if last used date not equal to current date and current coins not equal to zero,
    // then it will convert coins to wallet and set coins value zero
    //also set last used date and level
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

    //if user would not use app for 2 days it will make goal value empty
    private void DecreaeseGoal()
    {
        if (UserCurrentGoalCheck.isEmpty() || UserCurrentGoalCheck.equals("1"))
        SharedPreferenceManager.setUserGoalCheck(getActivity(),"");
    }

    //if user using app everyday it will increase goal value 1+
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

    //if user not using app for 2< then it will reduce level 1-
    private int reduceLevelCount(){
        if (userCurrentLevel == 1){
            ConvertCoinToWalletSaveLastUsedDateSaveLevel();
            return userCurrentLevel;
        }
        else{
            userCurrentLevel -= 1;

            if (userCurrentLevel <= 1)
                userCurrentLevel = 1;
            ConvertCoinToWalletSaveLastUsedDateSaveLevel();
            return userCurrentLevel;
        }

    }

    //get values from SharedPreference and update all values in firebase database
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
