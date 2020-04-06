
package com.cooperativeai.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class WelcomePage extends AppCompatActivity {

    private Button LoginBTN, SignupBTN;
    private Dialog dialog;
    private String email, password, fullname,username,coins,wallet,pictures,lastuseddate,goalcheck;
    private int level;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UsersDatabaseRef;
    private Boolean emailchecker;
    private CheckBox rememberCheck;
    private GoogleSignInClient googleSignInClient;
    private String TAG = "WelcomePage";
    private int RC_SIGN_IN = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_page);

        SignupBTN = (Button) findViewById(R.id.welcome_signup_btn);
        LoginBTN = (Button) findViewById(R.id.welcome_login_btn);
        firebaseAuth = FirebaseAuth.getInstance();
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");


        GoogleSignInOptions gso = new  GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this,gso);

        SignupBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WelcomePage.this, RegistrationActivity.class));
            }
        });

        LoginBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ShowDialog();
            }
        });
    }

    private void ShowDialog()
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
        View view = inflater.inflate(R.layout.login_layout,null);

        alertdialog.setView(view);
        alertdialog.setCancelable(true);
        AlertDialog dialog2 = alertdialog.create();
        dialog2.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog2.show();

        EditText Isemail = view.findViewById(R.id.login_email);
        EditText Ispassword = view.findViewById(R.id.login_password);
        TextView forgetPass = view.findViewById(R.id.sign_in_forget_pass);
        Button loginBTN = view.findViewById(R.id.signin_btn);
        Button loginwithGoogleBTN = view.findViewById(R.id.signin_google_btn);
        dialog = UtilityMethods.showDialog(WelcomePage.this, R.layout.layout_loading_dialog);
        rememberCheck = view.findViewById(R.id.remeber_check_box);

        String checkRem = SharedPreferenceManager.getSignRemember(WelcomePage.this);

        if (checkRem == "yes")
        {
            Isemail.setText(SharedPreferenceManager.getUserEmail(WelcomePage.this));
            Ispassword.setText(SharedPreferenceManager.getUserPassword(WelcomePage.this));
            rememberCheck.isChecked();
        }

        loginwithGoogleBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (UtilityMethods.isInternetAvailable())
                {
                    LoginWithGoogle();
                    dialog.show();
                    dialog2.dismiss();
                }
                else
                {
                    dialog.dismiss();
                    Toast.makeText(WelcomePage.this, "No internet connection available", Toast.LENGTH_SHORT).show();
                }
            }
        });


        forgetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(WelcomePage.this, ForgetPassword.class));
                dialog2.dismiss();

            }
        });

        loginBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = Objects.requireNonNull(Isemail.getText()).toString().trim();
                password = Objects.requireNonNull(Ispassword.getText()).toString();

                if (UtilityMethods.isInternetAvailable()) {
                    if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && !password.isEmpty()) {
                        StarLogin();
                        dialog2.dismiss();
                    }
                    else {
                        if (email.isEmpty())
                            Isemail.setError("Required field");
                        if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())
                            Isemail.setError("Invalid Format");
                        if (password.isEmpty())
                            Ispassword.setError("Password is required");
                    }
                }
                else{
                    Toast.makeText(WelcomePage.this, "No internet connection available", Toast.LENGTH_SHORT).show();
                }

            }
        });



    }

    private void LoginWithGoogle()
    {
        Intent signIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Toast.makeText(WelcomePage.this,"Signed In Successfully",Toast.LENGTH_LONG).show();
                FirebaseGoogleAuth(account);
            }
            catch (ApiException e)
            {
                Toast.makeText(WelcomePage.this,"Signed In Failed :"+e,Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        }
    }

    private void FirebaseGoogleAuth(GoogleSignInAccount account)
    {
        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                if (task.isSuccessful())
                {
                    final String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    UsersDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            if (!dataSnapshot.hasChild(current_user_id))
                            {
                                startActivity(new Intent(WelcomePage.this, SetupAccount.class));
                                finish();
                                dialog.dismiss();
                            }
                            else
                            {
                                SavaData();
                                startActivity(new Intent(WelcomePage.this, MainActivity.class));
                                finish();
                                dialog.dismiss();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }
        });
    }

    private void StarLogin()
    {
        dialog = UtilityMethods.showDialog(WelcomePage.this, R.layout.layout_loading_dialog);
        dialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dialog.dismiss();
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && task.getResult().getUser() != null)
                            {
                                VerifyEmail();

                            } else {
                                dialog.dismiss();
                                Toast.makeText(WelcomePage.this, "Could not login", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            dialog.dismiss();
                            Toast.makeText(WelcomePage.this, "Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void VerifyEmail()

    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        emailchecker = user.isEmailVerified();

        if (emailchecker)
        {

            final String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            UsersDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if (!dataSnapshot.hasChild(current_user_id))
                    {
                        startActivity(new Intent(WelcomePage.this, SetupAccount.class));
                        finish();
                        dialog.dismiss();

                        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(Constants.PREFS_USER_EMAIL, email);
                        if (rememberCheck.isChecked())
                        {
                            editor.putString(Constants.PREFS_SIGN_IN_REMEMBER,"yes");
                            editor.putString(Constants.PREFS_USER_PASSWORD,password);
                        }
                        else
                        {
                            editor.putString(Constants.PREFS_SIGN_IN_REMEMBER,"no");
                            editor.putString(Constants.PREFS_USER_PASSWORD,"");
                        }
                        editor.apply();


                    }
                    else
                    {
                        SavaData();
                        startActivity(new Intent(WelcomePage.this, MainActivity.class));
                        finish();
                        Toast.makeText(WelcomePage.this, "Loged in Successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        else
        {
            dialog.dismiss();
            Toast.makeText(this,"Please Verify your email first!!",Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
        }
    }

    private void SavaData()
    {
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UsersDatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
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
                    editor.putString(Constants.PREFS_USER_NAME,fullname);
                    editor.putString(Constants.PREFS_USER_USERNAME,username);
                    editor.putString(Constants.PREFS_USER_ID, UID);
                    editor.putString(Constants.PREFS_USER_COIN_COUNT, coins);
                    editor.putString(Constants.PREFS_USER_WALLET, wallet);
                    editor.putString(Constants.PREFS_USER_TOTAL_PICTURES, pictures);
                    editor.putString(Constants.PREFS_USER_GOAL_CHECK,goalcheck);
                    editor.putString(Constants.PREFS_USER_LAST_ACCESSED,lastuseddate);
                    SharedPreferenceManager.setUserLevel(WelcomePage.this,level);
                    editor.apply();

                    if (rememberCheck.isChecked())
                    {
                        editor.putString(Constants.PREFS_SIGN_IN_REMEMBER,"yes");
                        editor.putString(Constants.PREFS_USER_PASSWORD,password);
                    }
                    else
                    {
                        editor.putString(Constants.PREFS_SIGN_IN_REMEMBER,"no");
                        editor.putString(Constants.PREFS_USER_PASSWORD,"");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
