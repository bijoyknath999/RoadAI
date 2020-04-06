package com.cooperativeai.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cooperativeai.R;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgetPassword extends AppCompatActivity {

    private EditText IsEmail;
    private Button ResetBTN;
    private String Email;
    private FirebaseAuth firebaseAuth;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        IsEmail = findViewById(R.id.resetemail);
        ResetBTN = findViewById(R.id.resetbtn);

        firebaseAuth = FirebaseAuth.getInstance();
        dialog = UtilityMethods.showDialog(ForgetPassword.this, R.layout.layout_loading_dialog);

        ResetBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Email = Objects.requireNonNull(IsEmail.getText().toString()).trim();

                if (UtilityMethods.isInternetAvailable()) {
                    if (!Email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(Email).matches()) {
                        dialog.show();
                        ResetPassword();
                    } else {
                        if (Email.isEmpty())
                            IsEmail.setError("Email is required");
                        if (!Email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(Email).matches())
                            IsEmail.setError("Invalid email format");

                        dialog.dismiss();
                    }
                }
                else{
                    Toast.makeText(ForgetPassword.this, "No internet connection available", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void ResetPassword()
    {
        firebaseAuth.sendPasswordResetEmail(Email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if (task.isSuccessful())
                {
                    Toast.makeText(ForgetPassword.this,"Please check your mail inbox!!",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ForgetPassword.this,WelcomePage.class));
                    finish();
                }
                else {
                    String error = task.getException().getMessage();
                    Toast.makeText(ForgetPassword.this,"Error :"+error,Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
