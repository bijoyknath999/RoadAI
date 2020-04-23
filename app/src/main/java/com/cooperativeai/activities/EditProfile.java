package com.cooperativeai.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

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
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class EditProfile extends AppCompatActivity {


    private EditText FullName, UserName;
    private Button UpdateBTN;

    private String fullname, username,Isfullname,Isusername;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference UserRef;
    private Dialog noconnectionDialog,dialog;
    private Toolbar toolbar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        FullName = (EditText) findViewById(R.id.Editfullname);
        UserName = (EditText) findViewById(R.id.Editusername);
        UpdateBTN = (Button) findViewById(R.id.updatebtn);
        firebaseAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        noconnectionDialog = UtilityMethods.showDialogAlert(EditProfile.this, R.layout.dialog_box);
        dialog = UtilityMethods.showDialog(EditProfile.this, R.layout.layout_loading_dialog);
        toolbar = findViewById(R.id.edit_profile_toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        toolbar.setTitleTextColor(ContextCompat.getColor(this,R.color.color1));

        UserRef.child(firebaseAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    fullname = dataSnapshot.child("Fullname").getValue().toString();
                    username = dataSnapshot.child("Username").getValue().toString();
                    FullName.setText(fullname);
                    UserName.setText(username);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        UpdateBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UtilityMethods.isInternetAvailable()) {

                    Isfullname = Objects.requireNonNull(FullName.getText()).toString().trim();
                    Isusername = Objects.requireNonNull(UserName.getText()).toString().trim();
                    if (!Isfullname.isEmpty() && !username.isEmpty()) {
                        dialog.show();
                        UpdateData();
                    }
                    else {
                        if (Isfullname.isEmpty())
                            FullName.setError("Name is required");
                        if (Isusername.isEmpty())
                            UserName.setError("Username is required");
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
                    },2500);
                }
            }
        });

    }

    private void UpdateData()
    {
        String Ufullname = FullName.getText().toString();
        String Uusername = UserName.getText().toString().trim();
        HashMap UserMap = new HashMap();
        UserMap.put("Fullname",Ufullname);
        UserMap.put("Username",Uusername);

        String firebaseUserid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UserRef.child(firebaseUserid).updateChildren(UserMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {

                if (task.isSuccessful())
                {

                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.PREFS_USER_NAME, fullname);
                    editor.putString(Constants.PREFS_USER_USERNAME, username);
                    editor.apply();
                    if (dialog != null)
                        dialog.dismiss();
                    startActivity(new Intent(EditProfile.this,MainActivity.class));
                    overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);
                    finish();
                }
                else
                {
                    dialog.dismiss();
                    String message = task.getException().getMessage();
                    Toast.makeText(EditProfile.this,"Error Occured :"+message,Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
        return true;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }
}
