package com.example.bikerapp.Statistics;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bikerapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class StatisticsActivity extends AppCompatActivity {

    private static final int DAYS = 7;
    private String bikerKey;
    private ArrayList<Double> lastSevenDays;
    private Double overallDistance, todayDistance, lastWeekDistance, lastMonthDistance;
    private LineChart chart;

    private TextView tvTodayDistanceValue, tvLastWeekDistanceValue, tvLastMonthDistanceValue, tvOverallDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        setSupportActionBar();
        bikerKey = (String) getIntent().getExtras().get("bikerKey");
        lastSevenDays = new ArrayList<>(DAYS);
        for(int i = 0; i < DAYS; i++)
            lastSevenDays.add(0, 0.0);
        overallDistance = 0.0;
        todayDistance = 0.0;
        lastWeekDistance = 0.0;
        lastMonthDistance = 0.0;
        tvTodayDistanceValue = findViewById(R.id.textViewTodayDistanceValue);
        tvLastWeekDistanceValue = findViewById(R.id.textViewLastWeekDistanceValue);
        tvLastMonthDistanceValue = findViewById(R.id.textViewLastMonthDistanceValue);
        tvOverallDistance = findViewById(R.id.textViewOverallDistanceValue);
        chart = findViewById(R.id.chart);
        Timestamp currentTimestamp = Timestamp.now();
        Log.d("SACT", currentTimestamp.toDate().toString());
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
                    setDataAndDrawChart(currentTimestamp);
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
        DecimalFormat format = new DecimalFormat("0.00");
        tvTodayDistanceValue.setText(String.valueOf(format.format(todayD)) + " km");
        tvLastWeekDistanceValue.setText(String.valueOf(format.format(lastWeekD)) + " km");
        tvLastMonthDistanceValue.setText(String.valueOf(format.format(lastMonthD)) + " km");
        tvOverallDistance.setText(String.valueOf(format.format(overallD)) + " km");

    }

    private void setDataAndDrawChart(Timestamp currentTimestamp) {
        ArrayList<Entry> entries = new ArrayList<>();
        for(int i = 0; i < DAYS; i++)
            entries.add(new Entry(i, lastSevenDays.get(DAYS -1 - i).floatValue()));

        LineDataSet dataSet = new LineDataSet(entries, "Km achieved last 7 days");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        //****
        // Controlling X axis
        XAxis xAxis = chart.getXAxis();
        // Set the xAxis position to bottom. Default is top
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //Customizing x axis value
        String week[] = new String[DAYS];
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("E"); // the day of the week abbreviated
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTimestamp.toDate());
        for(int i = 0; i < DAYS; i++) {
            cal.setTime(currentTimestamp.toDate());
            cal.add(Calendar.DATE, -i);
            String day = simpleDateformat.format(cal.getTime());
            Log.d("SACT", day);
            week[DAYS -1 - i] = day;
        }

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return week[(int) value];
            }
        };

        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);

        //***
        // Controlling right side of y axis
        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);

        //***
        // Controlling left side of y axis
        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setGranularity(1f);

        // Setting Data
        LineData data = new LineData(dataSet);
        chart.setData(data);
        chart.animateX(2500);
        //refresh
        chart.invalidate();
    }
}
