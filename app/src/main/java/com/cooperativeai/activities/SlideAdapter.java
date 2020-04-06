package com.cooperativeai.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.cooperativeai.R;

public class SlideAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;

    public SlideAdapter(Context context)
    {
        this.context = context;
    }

    //Arrays

    public int[] slide_images = {

            R.drawable.slide_img_3,
            R.drawable.slide_img_2,
            R.drawable.slide_img_1
    };

    public String [] slide_titles = {

            "Earn coins for\ntravelling",
            "Help Others\nHelp Youself",
            "Map Based"
    };

    public String [] slide_desc = {

            "Travel more with Road Ai,\near coins in your wallet",
            "Record while driving, spot potholes, share on social media",
            "Detect road condition using\nany smart phone"
    };


    @Override
    public int getCount() {
        return slide_titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (LinearLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container,false);


        ImageView slideimg = (ImageView) view.findViewById(R.id.slide_img);
        TextView slidetitle = (TextView) view.findViewById(R.id.slide_title);
        TextView slidedesc = (TextView) view.findViewById(R.id.slide_desc);

        slideimg.setImageResource(slide_images[position]);
        slidetitle.setText(slide_titles[position]);
        slidedesc.setText(slide_desc[position]);

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout)object);
    }
}
