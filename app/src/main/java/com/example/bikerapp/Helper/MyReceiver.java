package com.example.bikerapp.Helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.example.bikerapp.R;

public class MyReceiver extends BroadcastReceiver {
    AppCompatActivity myActivity; //a reference to activity's context

    public MyReceiver(AppCompatActivity myActivity){
        if(myActivity instanceof AppCompatActivity)
            this.myActivity = myActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = NetworkUtil.getConnectivityStatusString(context);
        if(status.isEmpty()) {
            status = "No Internet Connection";
        }

        if(status.equals("No internet is available") || status.equals("No Internet Connection")){
            myActivity.getSupportActionBar().setTitle("Waiting Network...");
        } else {
            myActivity.getSupportActionBar().setTitle(R.string.reservation_title);
        }
    }
}