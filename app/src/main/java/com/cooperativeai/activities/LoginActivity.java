

package com.cooperativeai.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.UtilityMethods;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.editText_login_email)
    TextInputEditText editTextEmail;
    @BindView(R.id.editText_login_password)
    TextInputEditText editTextPassword;
    @BindView(R.id.button_login)
    Button buttonLogin;
    @BindView(R.id.textView_sign_up)
    TextView textViewSignUp;

    private String email;
    private String password;

    private FirebaseAuth firebaseAuth;

    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @OnClick(R.id.button_login)
    public void onClickLogin() {
        email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
        password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();

        if (UtilityMethods.isInternetAvailable()) {
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && !password.isEmpty()) {
                startLogin();
            } else {
                if (email.isEmpty())
                    editTextEmail.setError("Required field");
                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    editTextEmail.setError("Invalid Format");
                if (password.isEmpty())
                    editTextPassword.setError("Required Field");
            }
        }
    }

    @OnClick(R.id.textView_sign_up)
    public void goToSignUp() {
        startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        finish();
    }

    private void startLogin() {
        dialog = UtilityMethods.showDialog(LoginActivity.this, R.layout.layout_loading_dialog);
        dialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dialog.dismiss();
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && task.getResult().getUser() != null) {
                                SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(Constants.PREFS_USER_EMAIL, task.getResult().getUser().getEmail());
                                editor.putString(Constants.PREFS_USER_NAME, task.getResult().getUser().getDisplayName());
                                editor.putString(Constants.PREFS_USER_ID, task.getResult().getUser().getUid());
                                editor.apply();

                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Could not login", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
