package com.example.bikerapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class CompletedReservationsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String bikerKey;
    private ArrayList<ReservationModel> reservationsData;
    private ReservationListAdapter reservationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_reservations);

        String title = getString(R.string.completed_reservations_activity_title);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        db = FirebaseFirestore.getInstance();
        bikerKey = (String) getIntent().getExtras().get("bikerKey");
        reservationsData = new ArrayList<>();
        fillWithData();
        RecyclerView recyclerView = findViewById(R.id.reservationsRecyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        reservationsAdapter = new ReservationListAdapter(this, reservationsData);
        recyclerView.setAdapter(reservationsAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fillWithData() {
        // TODO - secondo me non serve il real time qua
        // TODO - Passare su firebase ad aggiungere il campo current_order e metterlo a false a tutte le reservation
        db.collection("reservations").whereEqualTo("biker_id", bikerKey).whereEqualTo("current_order", false).addSnapshotListener((EventListener<QuerySnapshot>) (document, e) -> {

            if (e != null)
                return;
            reservationsData.clear();

            for(DocumentChange dc : document.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    DocumentSnapshot doc = dc.getDocument();
                    ReservationModel tmpReservationModel = new ReservationModel((Long) doc.get("rs_id"),
                            (String) doc.get("rest_name"),
                            (String) doc.get("rest_address"),
                            (String) doc.get("cust_address"),
                            (String) doc.get("notes"),
                            (String) doc.get("cust_name"),
                            (String) doc.get("rest_id"),
                            (String) doc.get("cust_id"),
                            (String) doc.get("cust_phone"),
                            (Timestamp) doc.get("delivery_time"));
                    reservationsData.add(tmpReservationModel);
                    reservationsAdapter.notifyDataSetChanged();

                    Collections.sort(reservationsData); // TODO valutare se serve
                }
            }

        });
    }
}
