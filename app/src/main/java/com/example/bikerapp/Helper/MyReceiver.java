package com.example.bikerapp.Helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.bikerapp.MainActivity;
import com.example.bikerapp.R;

public class MyReceiver extends BroadcastReceiver {
    MainActivity ma; //a reference to activity's context

    public MyReceiver(MainActivity maContext){
        ma=maContext;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String status = NetworkUtil.getConnectivityStatusString(context);
        if(status.isEmpty()) {
            status="No Internet Connection";
        }

        if(status.equals("No internet is available") || status.equals("No Internet Connection")){
            ma.getSupportActionBar().setTitle("Waiting Network...");
        } else {
            ma.getSupportActionBar().setTitle(R.string.reservation_title);
        }

        Toast.makeText(context, status, Toast.LENGTH_LONG).show();
    }
}