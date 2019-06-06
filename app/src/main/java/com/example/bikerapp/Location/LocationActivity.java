package com.example.bikerapp.Location;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.bikerapp.Helper.MyReceiver;
import com.example.bikerapp.MainActivity;
import com.example.bikerapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import javax.annotation.Nullable;

public class LocationActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 100;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    private RelativeLayout statusRelativeLayout;
    private RelativeLayout relativeLayoutOn;
    private RelativeLayout relativeLayoutOff;
    private RelativeLayout relativeLayoutChangingStatus;
    private Switch switchWorkingStatus;
    private ProgressBar changingProgressBar;
    private Boolean biker_status;
    private Boolean changing_status;
    private Boolean updating_status;

    private BroadcastReceiver MyReceiver = null;
    private ListenerRegistration bsListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        checkGPSOn();
        SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
        bikerKey = sharedPref.getString("bikerKey","");

        getSupportActionBar().setTitle(getString(R.string.working_status));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        MyReceiver = new MyReceiver(this);
        broadcastIntent();

        statusRelativeLayout = findViewById(R.id.statusRelativeLayout);
        relativeLayoutOn = findViewById(R.id.relativeLayoutOn);
        relativeLayoutOff = findViewById(R.id.relativeLayoutOff);
        relativeLayoutChangingStatus = findViewById(R.id.relativeLayoutChangingStatus);
        changingProgressBar = findViewById(R.id.progressBarChangeStatus);
        switchWorkingStatus = findViewById(R.id.switchWorkingStatus);
        getAndSetCurrentStatus();

        LocationActivity la = this;
        switchWorkingStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changing_status = true;
                hideLayoutOnOff();
                showProgressBar();
                if(isChecked) {
                    //Check whether this app has access to the location permission//
                    int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                    //If the location permission has been granted, then start the TrackerService//
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        updating_status = true;
                        setBackgroundStatusOn();
                        startTrackerService();
                        setStatusForActivityResult(true);
                    } else {
                        //If the app doesn’t currently have access to the user’s location, then request access//
                        ActivityCompat.requestPermissions(la,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST);
                    }
                } else {
                    updating_status = false;
                    setBackgroundStatusOff();
                    stopTrackingService();
                    setStatusForActivityResult(false);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { // TODO chiedere ad Alberto se gli serve questa callback

        //If the permission has been granted...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //...then start the GPS tracking service//
            startTrackerService();
        } else {
            //If the user denies the permission request, then display a snackBar with some more information//
            Toast.makeText(getApplicationContext(), "Please enable location services to allow GPS tracking",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(MyReceiver);
    }

    //Start the TrackerService//
    private void startTrackerService() {
        FirebaseFirestore.getInstance().collection("bikers").document(bikerKey).update("status",true).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Intent intent = new Intent(this, TrackingService.class);
                startService(intent);
                biker_status = true;
                changing_status = false;
                setStatusForActivityResult(false);
                hideProgressBar();
                setLayoutOn();
                Toast.makeText(getApplicationContext(), "Status ON.\nWait for delivery requests.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopTrackingService(){
        FirebaseFirestore.getInstance().collection("bikers").document(bikerKey).update("status", false).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Intent intent = new Intent(this, TrackingService.class);
                stopService(intent);
                biker_status = false;
                changing_status = false;
                setStatusForActivityResult(false);
                hideProgressBar();
                setLayoutOff();
                Toast.makeText(getApplicationContext(), "Status OFF.\nYou will not receive new delivery requests.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void getAndSetCurrentStatus() {
        biker_status = getIntent().getExtras().getBoolean("biker_status");
        changing_status = getIntent().getExtras().getBoolean("changing_status");
        updating_status = getIntent().getExtras().getBoolean("updating_status");
        if(changing_status) {
            hideLayoutOnOff();
            showProgressBar();
            if(updating_status)
                setBackgroundStatusOn();
            else
                setBackgroundStatusOff();
            listenForStatusChanged();
            switchWorkingStatus.setChecked(updating_status);
        }
        else {
            if(biker_status) {
                setLayoutOn();
            }
            else {
                setLayoutOff();
            }
            switchWorkingStatus.setChecked(biker_status);
        }
    }

    private void setBackgroundStatusOn() {
        statusRelativeLayout.setBackgroundColor(Color.parseColor("#068DE5"));
    }

    private void setBackgroundStatusOff() {
        statusRelativeLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
    }

    private void setLayoutOn() {
        setBackgroundStatusOn();
        relativeLayoutOff.setVisibility(View.INVISIBLE);
        relativeLayoutOn.setVisibility(View.VISIBLE);
    }

    private void setLayoutOff() {
        setBackgroundStatusOff();
        relativeLayoutOn.setVisibility(View.INVISIBLE);
        relativeLayoutOff.setVisibility(View.VISIBLE);
    }

    private void hideLayoutOnOff() {
        relativeLayoutOn.setVisibility(View.INVISIBLE);
        relativeLayoutOff.setVisibility(View.INVISIBLE);
    }

    private void setStatusForActivityResult(Boolean status) {
        Intent retIntent = new Intent(getApplicationContext(), MainActivity.class);
        Bundle bn = new Bundle();
        bn.putBoolean("biker_status", biker_status);
        bn.putBoolean("changing_status", changing_status);
        bn.putBoolean("updating_status", status);
        retIntent.putExtras(bn);
        setResult(RESULT_OK, retIntent);
    }

    public void broadcastIntent() {
        registerReceiver(MyReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void checkGPSOn() {
        //Check whether GPS tracking is enabled//
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(getApplicationContext(), "Please active GPS!",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void listenForStatusChanged() {
        EventListener<DocumentSnapshot> eventListener = new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null)
                    return;
                if(!documentSnapshot.getMetadata().isFromCache()) {
                    hideProgressBar();
                    //Boolean status = (Boolean) documentSnapshot.get("status");
                    Log.d("LOCACT", "listenForStatusChanged()");
                    if(biker_status)
                        setLayoutOn();
                    else
                        setLayoutOff();
                    removeListenerForStatusChanged();
                }
            }
        };
        if (bsListenerRegistration == null ) {
            bsListenerRegistration = FirebaseFirestore.getInstance().collection("bikers").document(bikerKey)
                    .addSnapshotListener(MetadataChanges.INCLUDE, eventListener);
            // MetadataChanges.INCLUDE: you will receive another snapshot with isFomCache()
            // equal to false once the client has received up-to-date data from the backend
        }
    }

    private void removeListenerForStatusChanged() {
        if (bsListenerRegistration != null) {
            bsListenerRegistration.remove();
            bsListenerRegistration = null;
        }
    }

    public void showProgressBar() {
        changingProgressBar.setVisibility(View.VISIBLE);
        relativeLayoutChangingStatus.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        if(changingProgressBar != null)
            changingProgressBar.setVisibility(View.GONE);
        if(relativeLayoutChangingStatus != null)
            relativeLayoutChangingStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListenerForStatusChanged();
    }
}
