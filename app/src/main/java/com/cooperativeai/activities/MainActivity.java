
package com.cooperativeai.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DateTimeManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.luseen.spacenavigation.SpaceItem;
import com.luseen.spacenavigation.SpaceNavigationView;
import com.luseen.spacenavigation.SpaceOnClickListener;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Fragment selectedFragment = null;
    private DatabaseReference UserAcc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpaceNavigationView spaceNavigationView = (SpaceNavigationView) findViewById(R.id.bottom_nav);
        spaceNavigationView.initWithSaveInstanceState(savedInstanceState);
        spaceNavigationView.addSpaceItem(new SpaceItem("Home", R.drawable.ic_home));
        spaceNavigationView.addSpaceItem(new SpaceItem("Profile", R.drawable.ic_profile));

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.main_toolbar);


        UserAcc = FirebaseDatabase.getInstance().getReference().child("Users");

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Home");
        toolbar.setTitleTextColor(ContextCompat.getColor(this,R.color.color1));
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.drawer_open,R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.color1));
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);


        getSupportFragmentManager().beginTransaction().replace(R.id.home_container,
                new HomeFragment()).commit();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)

            {
                UserMenuSelector (menuItem);
                return false;
            }
        });


        spaceNavigationView.setSpaceOnClickListener(new SpaceOnClickListener() {
            @Override
            public void onCentreButtonClick() {

                startActivity(new Intent(MainActivity.this,CameraActivity.class));

            }

            @Override
            public void onItemClick(int itemIndex, String itemName) {

                if (itemIndex==0)
                {
                    selectedFragment = new HomeFragment();
                }

                else if (itemIndex==1)
                {
                    selectedFragment = new ProfileFragment();

                }

                if (selectedFragment != null)
                {
                    getSupportFragmentManager().beginTransaction().replace(R.id.home_container,selectedFragment).commit();
                }

            }

            @Override
            public void onItemReselected(int itemIndex, String itemName) {

            }
        });

        if (selectedFragment == new HomeFragment())
        {

        }

        //Navugation Menu

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)

            {
                UserMenuSelector (menuItem);
                return false;
            }
        });

    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }


    //Toolbar and Drawer

    @Override
    public boolean onOptionsItemSelected(MenuItem item)

    {

        switch (item.getItemId())
        {
            case R.id.toolbar_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Invitation From Road.Ai App");
                shareIntent.putExtra(Intent.EXTRA_TEXT ,"Description" +
                        "\nInvitation From Road.Ai App \nDownload Link : https://play.google.com/store/apps/details?id="+getPackageName());
                startActivity(shareIntent,null);
                break;
        }


        if (actionBarDrawerToggle.onOptionsItemSelected(item))

        {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Navigation

    private void UserMenuSelector(MenuItem menuItem)

    {
        switch (menuItem.getItemId())
        {
            case R.id.menu_logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this,WelcomePage.class));
                finish();
                break;
        }

    }

}
