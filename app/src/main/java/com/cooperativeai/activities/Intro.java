package com.cooperativeai.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cooperativeai.R;
import com.cooperativeai.utils.UtilityMethods;

public class Intro extends AppCompatActivity {

    private ViewPager SlidePager;
    private LinearLayout dotlayout;
    private Button SlideBTN;

    private TextView[] dots;

    private SlideAdapter slideAdapter;
    private Dialog dialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        SlidePager = (ViewPager) findViewById(R.id.slideviewer);
        dotlayout = (LinearLayout) findViewById(R.id.dotslayout);
        SlideBTN = (Button) findViewById(R.id.introbtn);

        slideAdapter = new SlideAdapter(this);
        SlidePager.setAdapter(slideAdapter);

        dialog = UtilityMethods.showDialog(Intro.this, R.layout.layout_loading_dialog);

        addDots(0);
        SlidePager.addOnPageChangeListener(viewListener);

        SlideBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent IntroIntent = new Intent(Intro.this, WelcomePage.class);
                IntroIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(IntroIntent);
                finish();
                dialog.show();
                overridePendingTransition(R.anim.slide_left_enter,R.anim.slide_left_exit);

                SharedPreferences sharedPreferences = getSharedPreferences("sharedPreferences",MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("firstTime",false);
                editor.apply();
            }
        });

    }

    public void addDots(int position)
    {
        dots = new TextView[3];

        dotlayout.removeAllViews();
        for (int i = 0;i<dots.length; i++)
        {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(R.color.color2));

            dotlayout.addView(dots[i]);
        }

        if (dots.length>0)
        {
            dots[position].setTextColor(getResources().getColor(R.color.color3));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            addDots(position);

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }
}
