package com.example.bikerapp.Statistics;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bikerapp.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class StatisticsActivity extends AppCompatActivity {

    private static final int DAYS = 7;
    private String bikerKey;
    private ArrayList<Double> lastSevenDays;
    private Double overallDistance, todayDistance, lastWeekDistance, lastMonthDistance;

    private TextView tvTodayDistanceValue, tvLastWeekDistanceValue, tvLastMonthDistanceValue, tvOverallDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        setSupportActionBar();
        bikerKey = (String) getIntent().getExtras().get("bikerKey");
        lastSevenDays = new ArrayList<>(DAYS);
        Collections.fill(lastSevenDays, 0.0);
        overallDistance = 0.0;
        todayDistance = 0.0;
        lastWeekDistance = 0.0;
        lastMonthDistance = 0.0;
        tvTodayDistanceValue = findViewById(R.id.textViewTodayDistanceValue);
        tvLastWeekDistanceValue = findViewById(R.id.textViewLastWeekDistanceValue);
        tvLastMonthDistanceValue = findViewById(R.id.textViewLastMonthDistanceValue);
        tvOverallDistance = findViewById(R.id.textViewOverallDistanceValue);
        Timestamp currentTimestamp = Timestamp.now();
        getBikerStatistics(currentTimestamp);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setSupportActionBar() {
        if(getSupportActionBar() != null) {
            String title = getString(R.string.statistics_activity_title);
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void getBikerStatistics(Timestamp currentTimestamp) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bikers_statistics").whereEqualTo("biker_key", bikerKey).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                QuerySnapshot documents = task.getResult();
                if(documents == null || documents.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "No delivery completed yet.",
                            Toast.LENGTH_LONG).show();
                } else {
                    for(DocumentSnapshot doc : documents) {
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        Double distance = doc.getDouble("distance");
                        int differenceInDays = getDays(currentTimestamp.toDate(), timestamp.toDate());
                        Log.d("SACT", "differenceInDays:" + differenceInDays);
                        addToStatistics(differenceInDays, distance);
                    }
                    setDistances(todayDistance, lastWeekDistance, lastMonthDistance, overallDistance);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Some problem occurred. Data cannot be retrieved!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private int getDays(Date current, Date old) {
        long difference = (current.getTime() - old.getTime()) / (24 * 60 * 60 * 1000);
        int ret = ((Long) Math.abs(difference)).intValue();
        return ret;
    }

    private void addToStatistics(int days, Double distance) {
        overallDistance += distance;
        if(days < 7) {
            lastWeekDistance += distance;
            lastSevenDays.add(days, lastSevenDays.get(days) + distance);
            if(days == 0)
                todayDistance += distance;
        }
        if(days <= 30)
            lastMonthDistance += distance;
    }

    private void setDistances(Double todayD, Double lastWeekD, Double lastMonthD, Double overallD) {
        tvTodayDistanceValue.setText(String.valueOf(todayD) + " km");
        tvLastWeekDistanceValue.setText(String.valueOf(lastWeekD) + " km");
        tvLastMonthDistanceValue.setText(String.valueOf(lastMonthD) + " km");
        tvOverallDistance.setText(String.valueOf(overallD) + " km");

    }
}
