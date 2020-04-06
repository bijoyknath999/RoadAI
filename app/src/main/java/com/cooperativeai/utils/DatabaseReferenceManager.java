package com.cooperativeai.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DatabaseReferenceManager {

    public static void setDataDatabase9value(Context context, String DatabasePath1, String DatabasePath2, String DatabasePath3, String DatabasePath4,String DatabasePath5,String DatabasePath6,String DatabasePath7,String DatabasePath8,String DatabasePath9, int Val1, String Val2, String Val3, String Val4,String Val5,String Val6,String Val7,String Val8,String Val9)
    {
        DatabaseReference UsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UsersDatabaseRef.child(UID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    UsersDatabaseRef.child(UID).child(DatabasePath1).setValue(Val1);
                    UsersDatabaseRef.child(UID).child(DatabasePath2).setValue(Val2);
                    UsersDatabaseRef.child(UID).child(DatabasePath3).setValue(Val3);
                    UsersDatabaseRef.child(UID).child(DatabasePath4).setValue(Val4);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
