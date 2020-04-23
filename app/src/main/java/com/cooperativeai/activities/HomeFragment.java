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

            if (!lastUsedDateAsString.isEmpty())
                SaveInDatabase();

            if (lastUsedDateAsString.isEmpty())
                lastUsedDateAsString = currentDateAsString;
            lastUsedDate = DateTimeManager.convertStringToDate(lastUsedDateAsString);

            //it will check difference b/w last accessed date to current date
            if (DateTimeManager.diffInDate(currentDate, lastUsedDate) > Constants.LEVEL_CHECK_DELAY) {
                //if the difference is greater than 2,then one level will be reduced
                userCurrentLevel = reduceLevelCount();
                //also decrease goal value by 1 if goal value is 1
                DecreaeseGoal();
            }
            else
            {
                //if the difference is not greater than 2, and coin value is greater than zero
                //if last accessed date not equal to current data then coin will be converted into wallet
                ConvertCoinToWalletSaveLastUsedDateSaveLevel();
                //also increase goal value by 1
                IcreaseGoalAndSaveData();
            }

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

        TextUsername.setText("Welcome, " + SharedPreferenceManager.getUserUsername(getContext()));
        TextCurrentTime.setText(""+DateTimeManager.getMonthNameWithDate());
        TextCoins.setText("" + SharedPreferenceManager.getUserCoins(getActivity()));
        TextLevel.setText("" + SharedPreferenceManager.getUserLevel(getActivity()));
        TextTemp.setText(""+SharedPreferenceManager.getUserLocationTemp(getContext()));
        TextCurrentLocation.setText(""+SharedPreferenceManager.getUserLocation(getContext()));

        return view;
    }


    //convert coin into wallet
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

    //decrease goal value
    private void DecreaeseGoal()
    {
        if (UserCurrentGoalCheck.isEmpty() || UserCurrentGoalCheck.equals("1"))
        SharedPreferenceManager.setUserGoalCheck(getActivity(),"");
    }

    //increase goal value
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

    //reduce user level
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

    private void SaveInDatabase()
    {
        HashMap Usermap = new HashMap();
        Usermap.put("Lastuseddate",SharedPreferenceManager.getLastUsedDate(getActivity()));
        Usermap.put("Coins",SharedPreferenceManager.getUserCoins(getActivity()));
        Usermap.put("Level",SharedPreferenceManager.getUserLevel(getActivity()));
        Usermap.put("Wallet",SharedPreferenceManager.getUserWallet(getActivity()));
        Usermap.put("Goalcheck",SharedPreferenceManager.getUserGoalCheck(getActivity()));
        //to updating user data in firebase database
        UsersDatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(Usermap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firebaseAuth.getCurrentUser() != null)
        {
            if (!lastUsedDateAsString.isEmpty())
                SaveInDatabase();
        }
    }
}
