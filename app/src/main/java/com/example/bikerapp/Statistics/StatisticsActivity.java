package com.example.bikerapp.Statistics;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bikerapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        getBikerStatistics();
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

    private void getBikerStatistics() {
        Date today = getToday();
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
                        int differenceInDays = getDays(today, timestamp.toDate());
                        addToStatistics(differenceInDays, distance);
                    }
                    setDistances(todayDistance, lastWeekDistance, lastMonthDistance, overallDistance);
                    setDataAndDrawChart(today);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Some problem occurred. Data cannot be retrieved!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private Date getToday() {
        Calendar c = Calendar.getInstance();
        // set the calendar to start of today
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTime();
    }

    private int getDays(Date today, Date old) {
        long difference;

        if(old.before(today)) {
            difference = ((today.getTime() - old.getTime()) / (24 * 60 * 60 * 1000)) + 1;
        } else {
            difference = 0;
        }

        return ((Long) Math.abs(difference)).intValue();
    }

    private void addToStatistics(int days, Double distance) {
        overallDistance += distance;
        if(days < 7) {
            lastWeekDistance += distance;
            lastSevenDays.set(days, lastSevenDays.get(days) + distance);
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

    private void setDataAndDrawChart(Date today) {
        ArrayList<Entry> entries = new ArrayList<>();
        for(int i = 0; i < DAYS; i++)
            entries.add(new Entry(i, lastSevenDays.get(DAYS -1 - i).floatValue()));

        LineDataSet dataSet = new LineDataSet(entries, null);
        // Circle properties
        dataSet.setCircleRadius(7f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.colorAccent));
        dataSet.setValueTextSize(16f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        dataSet.setDrawCircleHole(false);
        // Line properties
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setLineWidth(3f);

        // Grid properties
        chart.setDescription(null);

        // X axis Properties
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextSize(16f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE); // Set the xAxis position to bottom. Default is top
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        //Customizing x axis value
        String week[] = new String[DAYS];
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("E"); // the day of the week abbreviated
        Calendar cal = Calendar.getInstance();
        for(int i = 0; i < DAYS; i++) {
            cal.setTime(today);
            cal.add(Calendar.DATE, -i);
            String day = simpleDateformat.format(cal.getTime());
            week[DAYS -1 - i] = day;
        }

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return week[(int) value];
            }
        };
        xAxis.setValueFormatter(formatter);

        // Controlling left side of y axis
        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setDrawGridLines(false);
        yAxisLeft.setTextSize(16f);
        yAxisLeft.setYOffset(10f);

        // Controlling right side of y axis
        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);

        // Legend properties
        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setTextSize(16f);
        legend.setFormSize(10f);
        LegendEntry[] legendEntries = new LegendEntry[1];
        LegendEntry legendEntry = new LegendEntry();
        legendEntry.label = "km";
        legendEntry.formColor = ContextCompat.getColor(this, R.color.colorAccent);
        legendEntries[0] = legendEntry;
        legend.setCustom(legendEntries);
        // Setting Data
        LineData data = new LineData(dataSet);
        chart.setData(data);
        //chart.animateX(2500);
        //refresh
        chart.invalidate();
    }
}
