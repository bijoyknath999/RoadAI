/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;

import com.cooperativeai.R;

import java.io.IOException;

public class UtilityMethods {

    public static boolean isInternetAvailable() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Dialog showDialog(Context context, int resourceId){
        View dialogView = ((Activity) context).getLayoutInflater().inflate(resourceId, null);
        Dialog dialog = new Dialog(context, R.style.loadingDialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        return dialog;
    }
}
