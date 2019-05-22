package com.example.bikerapp.Location;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.example.bikerapp.R;

public class LocationActivity extends AppCompatActivity {

private static final int PERMISSIONS_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        Button start = findViewById(R.id.startLocation);
        start.setOnClickListener(v -> {
            //Check whether GPS tracking is enabled//
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this, "Please active GPS!", Toast.LENGTH_SHORT).show();
                finish();
            }

            //Check whether this app has access to the location permission//
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

            //If the location permission has been granted, then start the TrackerService//
            if (permission == PackageManager.PERMISSION_GRANTED) {
                startTrackerService();
            } else {
                //If the app doesn’t currently have access to the user’s location, then request access//
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST);
            }
        });

        Button stop = findViewById(R.id.stopLocation);
        stop.setOnClickListener(v->stopTrackingService());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //If the permission has been granted...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //...then start the GPS tracking service//
            startTrackerService();
        } else {
            //If the user denies the permission request, then display a toast with some more information//
            Toast.makeText(this, "Please enable location services to allow GPS tracking", Toast.LENGTH_SHORT).show();
        }
    }

    //Start the TrackerService//
    private void startTrackerService() {
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);

        //Notify the user that tracking has been enabled//
        Toast.makeText(this, "Start Working", Toast.LENGTH_SHORT).show();

        //Close MainActivity//
        finish();
    }

    private void stopTrackingService(){
        Intent intent = new Intent(this, TrackingService.class);
        stopService(intent);

        Toast.makeText(this, "Stop Working", Toast.LENGTH_SHORT).show();

        finish();
    }
}
