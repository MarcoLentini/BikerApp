package com.example.bikerapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.bikerapp.Information.BikerInformationActivity;
import com.example.bikerapp.Information.LoginActivity;
import com.example.bikerapp.Location.LocationActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    public static ArrayList<ReservationModel> reservationsData;
    private ReservationListAdapter reservationsAdapter;
    private ConstraintLayout constraintLayout;
    private String NOTIFICATION_CHANNEL_ID = "com.example.bikerapp";
    private int unique_id = 1;
    private NotificationCompat.Builder builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        constraintLayout = findViewById(R.id.main_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.reservation_title);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
        bikerKey = sharedPref.getString("bikerKey","");

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        //Get Firestore instance
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_restaurant)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.biker_logo))
                .setContentTitle("New Order to Delivery")
                .setContentIntent(pendingIntent)
                .setVisibility(VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        reservationsData = new ArrayList<>();
        fillWithData();
        RecyclerView recyclerView = findViewById(R.id.reservationsRecyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        reservationsAdapter = new ReservationListAdapter(this, MainActivity.reservationsData);
        recyclerView.setAdapter(reservationsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {

            Intent information = new Intent(this, BikerInformationActivity.class);
            startActivity(information);
        }
        if(id == android.R.id.home){
            onBackPressed();
            //getSupportFragmentManager().popBackStack();
        }

        if(id == R.id.location){
            Intent location = new Intent(this, LocationActivity.class);
            startActivity(location);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            super.onBackPressed();
        }
    }

    private void fillWithData() {
        db.collection("reservations").whereEqualTo("biker_id", bikerKey).addSnapshotListener((EventListener<QuerySnapshot>) (document, e) -> {

            if (e != null)
                return;
            reservationsData.clear();

            for(DocumentChange dc : document.getDocumentChanges())
                if(dc.getType() == DocumentChange.Type.ADDED){
                    DocumentSnapshot doc = dc.getDocument();
                    ReservationModel tmpReservationModel = new ReservationModel((Long)doc.get("rs_id"),
                            (String) doc.get("rest_name"),
                            (String) doc.get("rest_address"),
                            (String) doc.get("cust_address"),
                            (String) doc.get("notes"),
                            (String) doc.get("cust_name"),
                            (String) doc.get("rest_id"),
                            (String) doc.get("cust_id"),
                            (String) doc.get("cust_phone"),
                            (Timestamp) doc.get("delivery_time"),
                            true);
                    reservationsData.add(tmpReservationModel);
                    reservationsAdapter.notifyDataSetChanged();
                                        /*

                    View.OnClickListener snackbarListener = v -> {
                                tmpReservationModel.setStateNew(false);
                                reservationsAdapter.notifyItemChanged(0);
                            };
                            Snackbar.make(constraintLayout, "New order to be delivered!", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("GOT IT", snackbarListener).show();
                     */
                    Collections.sort(reservationsData);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(unique_id, builder.build());
                    unique_id++;
                }
        });



    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "Non so dove finisca questa descrizione";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BikerApp", importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();

        }

    }
}
