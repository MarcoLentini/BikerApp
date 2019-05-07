package com.example.bikerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.bikerapp.Information.BikerInformationActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    public static ArrayList<ReservationModel> ReservationsData;
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
            finish();
        }

        //Get Firestore instance
        db = FirebaseFirestore.getInstance();
        /*

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
 */
        ReservationsData = new ArrayList<>();
        fillWithData();
        loadFragment(new ReservationsMainFragment());
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

    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_container_main, fragment);
        transaction.commit();
    }

    private void fillWithData() {

        db.collection("reservations").whereEqualTo("biker_id", bikerKey).addSnapshotListener((EventListener<QuerySnapshot>) (document, e) -> {

            if (e != null)
                return;

            for(DocumentChange dc : document.getDocumentChanges())
                if(dc.getType() == DocumentChange.Type.ADDED)
                {//TODO incremento
                }
            if (!document.isEmpty()) {
                for (DocumentSnapshot doc : document) {

                    ReservationModel tmpReservationModel = new ReservationModel((Long) doc.get("rs_id"),
                            (String) doc.get("rest_name"),
                            (String) doc.get("rest_addr"),
                            (String) doc.get("cust_address"),
                            (String) doc.get("notes"),
                            (String) doc.get("cust_name"),
                            (String) doc.get("rest_id"),
                            (String) doc.get("cust_id"),
                            (String) doc.get("cust_phone"),
                            (Timestamp) doc.get("upload_time")


                    );


                            ReservationsData.add(tmpReservationModel);

                }
                Collections.sort(ReservationsData);

            } else {
                Log.d("QueryReservation", "No such document");
            }

        });



    }
}
