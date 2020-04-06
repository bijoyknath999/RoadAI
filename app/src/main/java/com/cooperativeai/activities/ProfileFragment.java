package com.cooperativeai.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.cooperativeai.R;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

public class ProfileFragment extends Fragment {

    private TextView ProfileFullname, ProfileLocation, ProfileLevel,ProfileTotalPics,ProfileTotalCoins;
    private DatabaseReference UsersRef;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        ProfileFullname = view.findViewById(R.id.profile_fullname);
        ProfileLocation = view.findViewById(R.id.profile_current_location);
        ProfileLevel = view.findViewById(R.id.profile_level);
        ProfileTotalPics = view.findViewById(R.id.profile_total_pictures);
        ProfileTotalCoins = view.findViewById(R.id.profile_total_coins);

        ((MainActivity) getActivity()).getSupportActionBar().setTitle("Profile");


        ProfileFullname.setText(SharedPreferenceManager.getUserFullName(getContext()));
        ProfileLocation.setText(SharedPreferenceManager.getUserLocation(getContext()));
        ProfileLevel.setText(""+SharedPreferenceManager.getUserLevel(getContext()));
        ProfileTotalCoins.setText(SharedPreferenceManager.getUserWallet(getContext()));
        ProfileTotalPics.setText(SharedPreferenceManager.getUserTotalPicturesCapture(getContext()));

        return view;

    }
}
