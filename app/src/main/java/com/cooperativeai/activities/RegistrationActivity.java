
package com.cooperativeai.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {

    private EditText IsEmail, IsPass, IsConfirmPass, IsFullname, IsUsername;
    private Button SignUpBtn;
    private TextView SignInBtn;
    private String email, password, confirmpass, fullname, username;
    private FirebaseAuth firebaseAuth;
    private Dialog dialog;
    private DatabaseReference UsersDatabaseRef;
    private Dialog noconnectionDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        firebaseAuth = FirebaseAuth.getInstance();
        UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");

        IsEmail = findViewById(R.id.edittextemail);
        IsFullname = findViewById(R.id.edittextfullname);
        IsUsername = findViewById(R.id.edittextusername);
        IsPass = findViewById(R.id.edittextpassword);
        IsConfirmPass = findViewById(R.id.edittextconfirmpassword);
        SignUpBtn = findViewById(R.id.signupbtn);
        SignInBtn = findViewById(R.id.already_signinbtn);

        noconnectionDialog = UtilityMethods.showDialogAlert(RegistrationActivity.this, R.layout.dialog_box);
        dialog = UtilityMethods.showDialog(RegistrationActivity.this, R.layout.layout_loading_dialog);

        //sign in button click listener
        // Send User to sign in welcomepage
        SignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(RegistrationActivity.this, WelcomePage.class));
                overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                finish();
                dialog.show();

            }
        });

        //sign up button click listener
        SignUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                email = Objects.requireNonNull(IsEmail.getText()).toString().trim();
                password = Objects.requireNonNull(IsPass.getText()).toString();
                confirmpass = Objects.requireNonNull(IsConfirmPass.getText()).toString();
                fullname = Objects.requireNonNull(IsFullname.getText()).toString();
                username = Objects.requireNonNull(IsUsername.getText()).toString().trim();

                if (UtilityMethods.isInternetAvailable()) {
                    if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && !password.isEmpty() &&!confirmpass.isEmpty() && !fullname.isEmpty() && !username.isEmpty() && password.equals(confirmpass)) {
                        dialog.show();
                        CheckUsername();
                    } else {
                        if (email.isEmpty())
                            IsEmail.setError("Email is required");
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                            IsEmail.setError("Invalid email format");
                        if (password.isEmpty())
                            IsPass.setError("Password is required");
                        if (confirmpass.isEmpty())
                            IsConfirmPass.setError("Confirm Password is required");
                        if (fullname.isEmpty())
                            IsFullname.setError("Name is required");
                        if (username.isEmpty())
                            IsUsername.setError("Username is required");
                        if (!password.equals(confirmpass))
                            IsConfirmPass.setError("Password do not match");

                        dialog.dismiss();
                    }
                }
                else{
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
    //Check Username either it is exists in firebase database or not
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
                    CheckEmail();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Check Email either it is exists in firebase database or not
    private void CheckEmail()
    {
        Query query = UsersDatabaseRef.orderByChild("Email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    IsEmail.setError("Username already exists");
                    dialog.dismiss();
                }
                else
                {
                    startRegistration();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Sign up with email password using firebase auth
    private void startRegistration() {

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (firebaseUser != null)
                            {
                                SaveUserDataDatabase();
                            }
                        }

                        else
                        {
                            String message = task.getException().getMessage();
                            Toast.makeText(RegistrationActivity.this, "Error Occured:" + message, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }
                });
    }

    //Save user data in firebase and phone also
    private void SaveUserDataDatabase() {

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
                    SharedPreferenceManager.setUserLevel(RegistrationActivity.this,1);
                    if (dialog != null)
                        dialog.dismiss();
                    SendEmailVerification();
                }
                else
                {
                    String message = task.getException().getMessage();
                    Toast.makeText(RegistrationActivity.this,"Error Occured :"+message,Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    //Send Email Verification Link
    private void SendEmailVerification() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!= null)
        {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful())
                    {
                        startActivity(new Intent(RegistrationActivity.this, WelcomePage.class));
                        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                        finish();
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(RegistrationActivity.this, "We've sent you a verification mail. Please check your mail inbox!!",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        String error = task.getException().getMessage();
                        overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                        Toast.makeText(RegistrationActivity.this,"Error: "+error,Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }
                }
            });
        }


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
