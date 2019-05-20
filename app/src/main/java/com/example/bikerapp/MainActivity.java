package com.example.bikerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.bikerapp.Information.BikerInformationActivity;
import com.example.bikerapp.Information.LoginActivity;
import com.example.bikerapp.Location.LocationActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    public static ArrayList<ReservationModel> reservationsData;
    private ReservationListAdapter reservationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                if(dc.getType() == DocumentChange.Type.ADDED)
                {
                    //TODO incremento
                }
            if (!document.isEmpty()) {
                for (DocumentSnapshot doc : document) {

                    ReservationModel tmpReservationModel = new ReservationModel((Long)doc.get("rs_id"),
                            (String) doc.get("rest_name"),
                            (String) doc.get("rest_address"),
                            (String) doc.get("cust_address"),
                            (String) doc.get("notes"),
                            (String) doc.get("cust_name"),
                            (String) doc.get("rest_id"),
                            (String) doc.get("cust_id"),
                            (String) doc.get("cust_phone"),
                            (Timestamp) doc.get("delivery_time")


                    );
                    reservationsData.add(tmpReservationModel);
                    reservationsAdapter.notifyDataSetChanged();

                }
                Collections.sort(reservationsData);

            } else {
                Log.d("QueryReservation", "No such document");
            }

        });



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
