package com.example.bikerapp.Location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.example.bikerapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class LocationActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 100;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    private RelativeLayout statusRelativeLayout;
    private RelativeLayout relativeLayoutOn;
    private RelativeLayout relativeLayoutOff;
    private ConstraintLayout constraintLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        statusRelativeLayout = findViewById(R.id.statusRelativeLayout);
        relativeLayoutOn = findViewById(R.id.relativeLayoutOn);
        relativeLayoutOff = findViewById(R.id.relativeLayoutOff);
        progressBar = findViewById(R.id.progressBarChangeStatus);
        Switch switchWorkingStatus = findViewById(R.id.switchWorkingStatus);
        Boolean status = getIntent().getExtras().getBoolean("biker_status");
        if(status) {
            switchWorkingStatus.setChecked(true);
           setAppeareanceOn();
        }
        else {
            switchWorkingStatus.setChecked(false);
            setAppereanceOff();
        }
        getSupportActionBar().setTitle(getString(R.string.working_status));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
        bikerKey = sharedPref.getString("bikerKey","");

        LocationActivity la = this;
        constraintLayout = findViewById(R.id.locationConstraintLayout);
        switchWorkingStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    //Check whether GPS tracking is enabled//
                    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        Snackbar.make(constraintLayout, "Please active GPS!",
                                Snackbar.LENGTH_LONG).show();
                        finish();
                    }

                    //Check whether this app has access to the location permission//
                    int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

                    //If the location permission has been granted, then start the TrackerService//
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        startTrackerService();
                    } else {
                        //If the app doesn’t currently have access to the user’s location, then request access//
                        ActivityCompat.requestPermissions(la,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST);
                    }
                    setAppeareanceOn();
                } else {
                    stopTrackingService();
                    setAppereanceOff();
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //If the permission has been granted...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //...then start the GPS tracking service//
            startTrackerService();
        } else {
            //If the user denies the permission request, then display a snackBar with some more information//
            Snackbar.make(constraintLayout, "Please enable location services to allow GPS tracking",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    //Start the TrackerService//
    private void startTrackerService() {
        FirebaseFirestore.getInstance().collection("bikers").document(bikerKey).update("status",true).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Intent intent = new Intent(this, TrackingService.class);
                startService(intent);
                Snackbar.make(constraintLayout, "You are now AVAILABLE. Wait for delivery requests.",
                        Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(constraintLayout, "You are NOT available.",
                        Snackbar.LENGTH_LONG).show();
                /*AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Impossible to change your status!");
                alertBuilder.setMessage("Your status cannot be updated now. Check your connection. Your status will be updated as soon as possible and you will be informed"); // TODO string.xml
                alertBuilder.setPositiveButton("OK", (dialog, which) -> {                });
                alertBuilder.create().show();*/
            }
        });
    }

    private void stopTrackingService(){
        FirebaseFirestore.getInstance().collection("bikers").document(bikerKey).update("status", false).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Intent intent = new Intent(this, TrackingService.class);
                stopService(intent);
                Snackbar.make(constraintLayout, "Your state is disabled. You will not receive delivery requests.",
                        Snackbar.LENGTH_LONG).show();
            } else {
                // TODO manage task unsuccessfull
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setAppeareanceOn() {
        statusRelativeLayout.setBackgroundColor(Color.parseColor("#068DE5"));
        relativeLayoutOff.setVisibility(View.INVISIBLE);
        relativeLayoutOn.setVisibility(View.VISIBLE);
    }

    private void setAppereanceOff() {
        statusRelativeLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
        relativeLayoutOn.setVisibility(View.INVISIBLE);
        relativeLayoutOff.setVisibility(View.VISIBLE);
    }
}
