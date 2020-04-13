package com.cooperativeai.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cooperativeai.Map;
import com.cooperativeai.R;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

public class ProfileFragment extends Fragment {

    private TextView ProfileFullname, ProfileLocation, ProfileLevel,ProfileTotalPics,ProfileTotalCoins;
    private ImageButton Edit_Profile;
    private Dialog dialog;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        ProfileFullname = view.findViewById(R.id.profile_fullname);
        ProfileLocation = view.findViewById(R.id.profile_current_location);
        ProfileLevel = view.findViewById(R.id.profile_level);
        ProfileTotalPics = view.findViewById(R.id.profile_total_pictures);
        ProfileTotalCoins = view.findViewById(R.id.profile_total_coins);
        Edit_Profile = view.findViewById(R.id.profile_edit);

        dialog = UtilityMethods.showDialog(getActivity(), R.layout.layout_loading_dialog);


        //edit image button click listener
        Edit_Profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserEditProfile();
            }
        });

        //set Toolbar Title
        ((MainActivity) getActivity()).getSupportActionBar().setTitle("Profile");

        //get values from SharedPreference
        ProfileFullname.setText(SharedPreferenceManager.getUserFullName(getContext()));
        ProfileLocation.setText(SharedPreferenceManager.getUserLocation(getContext()));
        ProfileLevel.setText(""+SharedPreferenceManager.getUserLevel(getContext()));
        ProfileTotalCoins.setText(SharedPreferenceManager.getUserWallet(getContext()));
        ProfileTotalPics.setText(SharedPreferenceManager.getUserTotalPicturesCapture(getContext()));

        return view;
    }

    //Send user to edi profile activity
    private void SendUserEditProfile()
    {
        Intent EditProfileintent = new Intent(getContext(), Map.class);
        startActivity(EditProfileintent);
        getActivity().overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
        dialog.show();
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
