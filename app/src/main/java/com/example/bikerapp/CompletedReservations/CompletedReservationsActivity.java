package com.example.bikerapp.CompletedReservations;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bikerapp.R;
import com.example.bikerapp.ReservationModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class CompletedReservationsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String bikerKey;
    private ArrayList<ReservationModel> reservationsData;
    private ReservationListAdapter reservationsAdapter;

    private ProgressBar pbGetCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_reservations);

        setSupportActionBar();
        pbGetCompleted = findViewById(R.id.progress_bar_get_completed);
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
        pbGetCompleted.setVisibility(View.VISIBLE);
        db.collection("reservations").whereEqualTo("biker_id", bikerKey).
            whereEqualTo("is_current_order", false).get().addOnCompleteListener(task -> {
                pbGetCompleted.setVisibility(View.GONE);
                reservationsData.clear();
                if(task.isSuccessful()) {
                    QuerySnapshot documents = task.getResult();
                    if(documents == null || documents.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "No delivery completed yet.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        for(DocumentSnapshot doc : documents) {
                            ReservationModel tmpReservationModel = new ReservationModel(doc.getLong("rs_id"),
                                    doc.getString("rest_name"),
                                    doc.getString("rest_address"),
                                    doc.getString("cust_address"),
                                    doc.getString("notes"),
                                    doc.getString("cust_name"),
                                    doc.getString("rest_id"),
                                    doc.getString("cust_id"),
                                    doc.getString("cust_phone"),
                                    doc.getTimestamp("delivery_time"),
                                    doc.getDouble("restaurant_distance"),
                                    doc.getDouble("user_distance"));
                            reservationsData.add(tmpReservationModel);
                        }
                        reservationsAdapter.notifyDataSetChanged();
                        Collections.sort(reservationsData); // ordino dal più recente al più vecchio
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Some problem occurred. Data cannot be retrieved!",
                            Toast.LENGTH_LONG).show();
                }
        });
    }

    private void setSupportActionBar() {
        if(getSupportActionBar() != null) {
            String title = getString(R.string.completed_reservations_activity_title);
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
}
