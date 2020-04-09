package com.cooperativeai.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class SetupAccount extends AppCompatActivity {

    private EditText Isemail, IsUsername, Isfullname;
    private Button SaveBTN, LogoutBTN;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private Dialog dialog;
    String getemail,email,username,fullname;
    private Dialog noconnectionDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_account);

        Isemail = findViewById(R.id.setupemail);
        Isfullname = findViewById(R.id.setupfullname);
        IsUsername = findViewById(R.id.setupusername);
        SaveBTN = findViewById(R.id.savebtn);
        LogoutBTN = findViewById(R.id.signoutbtn);
        noconnectionDialog = UtilityMethods.showDialogAlert(SetupAccount.this, R.layout.dialog_box);


        firebaseAuth = FirebaseAuth.getInstance();
        dialog = UtilityMethods.showDialog(SetupAccount.this, R.layout.layout_loading_dialog);
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        getemail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Isemail.setText(getemail);

        LogoutBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (UtilityMethods.isInternetAvailable()) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(SetupAccount.this, WelcomePage.class));
                    overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                    finish();
                    dialog.show();
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
                    },2500);                }
            }
        });

        SaveBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = Objects.requireNonNull(Isemail.getText()).toString().trim();
                fullname = Objects.requireNonNull(Isfullname.getText()).toString();
                username = Objects.requireNonNull(IsUsername.getText()).toString().trim();

                if (UtilityMethods.isInternetAvailable()) {
                    if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && !fullname.isEmpty() && !username.isEmpty()) {
                        dialog.show();
                        CheckUsername();
                    }
                    else {
                        if (email.isEmpty())
                            Isemail.setError("Email is required");
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                            Isemail.setError("Invalid email format");
                        if (fullname.isEmpty())
                            IsUsername.setError("Name is required");
                        if (username.isEmpty())
                            IsUsername.setError("Username is required");
                        dialog.dismiss();
                    }
                }
                else {
                    noconnectionDialog.show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (noconnectionDialog.isShowing())
                            {
                                noconnectionDialog.dismiss();
                            }
                        }
                    },2500);                }
            }
        });


    }

    private void CheckUsername()
    {
        Query query = UsersDatabaseRef.orderByChild("Username").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    IsUsername.setError("Username already exists");
                    dialog.dismiss();
                }
                else
                {
                    SaveUserInfo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void SaveUserInfo()
    {


        HashMap UserMap = new HashMap();
        UserMap.put("Fullname",fullname);
        UserMap.put("Username",username);
        UserMap.put("Email",email);
        UserMap.put("Coins","0");
        UserMap.put("Pictures","0");
        UserMap.put("Lastuseddate","");
        UserMap.put("Level",1);
        UserMap.put("Wallet","0");
        UserMap.put("Goalcheck","");


        String firebaseUserid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UsersDatabaseRef.child(firebaseUserid).updateChildren(UserMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {

                if (task.isSuccessful())
                {

                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.PREFS_USER_EMAIL, email);
                    editor.putString(Constants.PREFS_USER_NAME, fullname);
                    editor.putString(Constants.PREFS_USER_USERNAME, username);
                    editor.putString(Constants.PREFS_USER_ID, firebaseUserid);
                    editor.putString(Constants.PREFS_USER_COIN_COUNT, "0");
                    editor.putString(Constants.PREFS_USER_WALLET, "0");
                    editor.putString(Constants.PREFS_USER_TOTAL_PICTURES, "0");
                    editor.putString(Constants.PREFS_USER_GOAL_CHECK, "");
                    editor.putString(Constants.PREFS_USER_LAST_ACCESSED, "");
                    editor.apply();
                    SharedPreferenceManager.setUserLevel(SetupAccount.this,1);
                    if (dialog != null)
                        dialog.dismiss();
                    startActivity(new Intent(SetupAccount.this,MainActivity.class));
                    overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                    finish();
                }
                else
                {
                    dialog.dismiss();
                    String message = task.getException().getMessage();
                    Toast.makeText(SetupAccount.this,"Error Occured :"+message,Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
