package com.example.bikerapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bikerapp.Helper.Haversine;
import com.example.bikerapp.Helper.MyReceiver;
import com.example.bikerapp.Information.BikerInformationActivity;
import com.example.bikerapp.Information.LoginActivity;
import com.example.bikerapp.Location.LocationActivity;
import com.example.bikerapp.Location.TrackingService;
import com.example.bikerapp.Statistics.StatisticsActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class MainActivity extends AppCompatActivity implements ISelectedCode {

    private static final int WORKING_STATUS = 1;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    private String NOTIFICATION_CHANNEL_ID = "com.example.bikerapp";
    private int unique_id = 1001;
    private NotificationCompat.Builder builder;
    private Long confirmationCode = -1L;
    private boolean read_status = false;
    private boolean connection_msg_read = false;
    private boolean biker_status = false;
    private boolean changing_status = false;
    private boolean updating_status = false;
    private boolean listener_activated = false;
    private String documentKey;
    private Double restaurantDistance = 0.0;
    private Double userDistance = 0.0;
    private ListenerRegistration listenerRegistration;
    private ListenerRegistration bsListenerRegistration;

    private ProgressBar pbInitialSynchronization;
    private CardView cvNoDelivery;
    private TextView tvNoDelivery;
    private LinearLayout reservationLinearLayout;
    private ConstraintLayout constraintLayout;
    private TextView tvReservationIdValue;
    private TextView tvNewReservation;
    private ImageView ivRestaurantLogo;
    private TextView tvRestaurantName;
    private TextView tvRestaurantAddress;
    private TextView tvUserName;
    private TextView tvUserAddress;
    private TextView tvUserNotes;

    private BroadcastReceiver MyReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pbInitialSynchronization = findViewById(R.id.progress_bar_initial_synchronization);
        cvNoDelivery = findViewById(R.id.cardViewNoDelivery);
        tvNoDelivery = findViewById(R.id.textViewNoDelivery);
        reservationLinearLayout = findViewById(R.id.reservationLinearLayout);
        constraintLayout = findViewById(R.id.main_layout);
        tvReservationIdValue = findViewById(R.id.textViewReservationIdValue);
        tvNewReservation = findViewById(R.id.textViewNewReservation);
        ivRestaurantLogo = findViewById(R.id.imageViewRestaurantLogo);
        tvRestaurantName = findViewById(R.id.textViewRestaurantName);
        tvRestaurantAddress = findViewById(R.id.textViewRestaurantAddress);
        tvUserName = findViewById(R.id.textViewUserName);
        tvUserAddress = findViewById(R.id.textViewUserAddress);
        tvUserNotes = findViewById(R.id.textViewUserNotes);
        tvRestaurantAddress.setOnClickListener(v -> {
            startGoogleMaps(tvRestaurantAddress.getText().toString());
        });
        tvUserAddress.setOnClickListener(v -> {
            startGoogleMaps(tvUserAddress.getText().toString());
        });
        Button btnConcludeDelivery = findViewById(R.id.buttonConcludeDelivery);
        CodePickerDialog pickerDialog = new CodePickerDialog();
        btnConcludeDelivery.setOnClickListener(v -> pickerDialog.show(getSupportFragmentManager(), "code picker"));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.reservation_title);
        setSupportActionBar(toolbar);

        MyReceiver = new MyReceiver(this);
        // broadcastIntent(); c'è già in onResume

        SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
        bikerKey = sharedPref.getString("bikerKey","");

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        setLayoutNoDelivery();
        //Get Firestore instance
        db = FirebaseFirestore.getInstance();
        fillWithData();

        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_restaurant)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.biker_logo))
                .setContentTitle("New order to delivered")
                .setContentIntent(pendingIntent)
                .setVisibility(VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if(savedInstanceState != null)
            connection_msg_read = savedInstanceState.getBoolean("connection_msg_read");
        if(savedInstanceState == null || !savedInstanceState.getBoolean("read_status")) {
            getAndUpdateBikerStatus();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.getBoolean("read_status")) {
            SynchronizeBikerStatus(savedInstanceState.getBoolean("biker_status"));
            Log.d("VITA", "onRestoreInstanceState(...) chiamato e mette biker_status:" + String.valueOf(savedInstanceState.getBoolean("biker_status")));
        }
    }

    public void broadcastIntent() {
        registerReceiver(MyReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_completed_reservations) {
            Intent completedReservations = new Intent(this, CompletedReservationsActivity.class);
            Bundle bn = new Bundle();
            bn.putString("bikerKey", bikerKey);
            completedReservations.putExtras(bn);
            startActivity(completedReservations);
        }

        if(id == R.id.location || id == R.id.current_status_biker){
            startLocationActivity();
        }
        if (id == R.id.action_statistics) {
            Intent statistics = new Intent(this, StatisticsActivity.class);
            Bundle bn = new Bundle();
            bn.putString("bikerKey", bikerKey);
            statistics.putExtras(bn);
            startActivity(statistics);
        }

        if (id == R.id.action_settings) {
            Intent information = new Intent(this, BikerInformationActivity.class);
            startActivity(information);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(read_status) {
            if(!menu.findItem(R.id.location).isEnabled()) {
                menu.findItem(R.id.current_status_biker).setVisible(true);
                menu.findItem(R.id.location).setEnabled(true);
            }
            if (biker_status)
                menu.findItem(R.id.current_status_biker).setIcon(android.R.drawable.button_onoff_indicator_on);
            else
                menu.findItem(R.id.current_status_biker).setIcon(android.R.drawable.button_onoff_indicator_off);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void startGoogleMaps(String delivery_address) {
        delivery_address = delivery_address.replace(" ","+");
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + delivery_address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void fillWithData() {
        db.collection("reservations").whereEqualTo("biker_id", bikerKey).whereEqualTo("is_current_order", true).addSnapshotListener((EventListener<QuerySnapshot>) (document, e) -> {

            if (e != null)
                return;

            for(DocumentChange dc : document.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    documentKey = dc.getDocument().getId();
                    DocumentSnapshot doc = dc.getDocument();
                    ReservationModel tmpReservationModel = new ReservationModel(doc.getLong("rs_id"),
                            doc.getString("rest_name"),
                            doc.getString("rest_address"),
                            doc.getString("cust_address"),
                            doc.getString("delivery_notes"),
                            doc.getString("cust_name"),
                            doc.getString("rest_id"),
                            doc.getString("cust_id"),
                            doc.getString("cust_phone"),
                            null);
                    if(doc.get("confirmation_code") != null) {
                        confirmationCode = doc.getLong("confirmation_code");
                    }
                    restaurantDistance = doc.getDouble("restaurant_distance");
                    userDistance = doc.getDouble("user_distance");

                    tvReservationIdValue.setText(String.valueOf(tmpReservationModel.getRsId()));
                    tvRestaurantName.setText(tmpReservationModel.getNameRest());
                    tvRestaurantAddress.setText(tmpReservationModel.getAddrRest());
                    tvUserName.setText(tmpReservationModel.getNameUser());
                    tvUserAddress.setText(tmpReservationModel.getAddrUser());
                    String notes = tmpReservationModel.getInfoUser();
                    if(notes.equals("")) {
                        tvUserNotes.setVisibility(View.INVISIBLE);
                    } else {
                        tvUserNotes.setText(notes);
                        tvUserNotes.setVisibility(View.VISIBLE);
                    }
                    String restaurantId = tmpReservationModel.getRestId();
                    db.collection("restaurant").document(restaurantId).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {

                                    DocumentSnapshot documentResult = task.getResult();
                                    if (documentResult.exists()) {
                                        String restaurantImage = documentResult.getString("rest_image");
                                        Uri tmpUri = Uri.parse(restaurantImage);
                                        Glide.with(getApplicationContext()).load(tmpUri).placeholder(R.drawable.img_biker_1).into(ivRestaurantLogo);
                                    } else {
                                        Log.d("QueryRestaurants", "No such document");
                                    }
                                } else {
                                    Log.d("QueryRestaurants", "get failed with ", task.getException());
                                }
                            });
                    Boolean biker_check = dc.getDocument().getBoolean("biker_check");
                    if(dc.getDocument().get("rs_status").equals("IN_PROGRESS") && !biker_check){
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(unique_id, builder.build());
                        createNewMissionSnackBar();
                    } else {
                        tvNewReservation.setVisibility(View.INVISIBLE);
                    }
                    setLayoutDelivery();
                }
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
        broadcastIntent();
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void createNewMissionSnackBar() {
        View.OnClickListener snackBarListener = v -> {
            db.collection("reservations").document(documentKey).update("biker_check", true);
            tvNewReservation.setVisibility(View.INVISIBLE);
            removeNewDeliveryNotification();
        };
        Snackbar.make(constraintLayout, "New order to be delivered!", Snackbar.LENGTH_INDEFINITE)
                .setAction("GOT IT", snackBarListener).show();
    }

    @Override
    public void onSelectedCode(String code) {
        int insertedCode = Integer.parseInt(code);
        if(insertedCode == confirmationCode) {
            Timestamp t = Timestamp.now();
            db.collection("reservations").document(documentKey).update("rs_status", "DELIVERED",
                    "is_current_order", false, "delivery_time", t);
            writeBikerDeliveryStatistics(t);
            removeNewDeliveryNotification();
            setLayoutNoDelivery();
            Snackbar.make(constraintLayout, "Delivery successfully completed",
                    Snackbar.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setTitle("Error!");
            alertBuilder.setMessage(R.string.msg_wrong_code);
            alertBuilder.setPositiveButton("OK", (dialog, which) -> {            });
            alertBuilder.create().show();
        }
    }

    private void getAndUpdateBikerStatus() {
        pbInitialSynchronization.setVisibility(View.VISIBLE);
        db.collection("bikers").document(bikerKey).get(Source.SERVER).addOnCompleteListener(task -> {

            if(!task.isSuccessful()) {
                showConnectionErrorDialog();
                EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
                    if (e != null)
                        return;
                    if(!documentSnapshot.getMetadata().isFromCache()) {
                        Boolean status = documentSnapshot.getBoolean("status");
                        SynchronizeBikerStatus(status);
                        changing_status = false;
                        if (listenerRegistration != null) {
                            listenerRegistration.remove();
                        }
                    }
                };
                if (listenerRegistration == null ) {
                    listenerRegistration = db.collection("bikers").document(bikerKey)
                            .addSnapshotListener(MetadataChanges.INCLUDE, eventListener);
                    // MetadataChanges.INCLUDE: you will receive another snapshot with isFomCache()
                    // equal to false once the client has received up-to-date data from the backend
                }
            } else {
                if(task.getResult() != null) {
                    Boolean status = task.getResult().getBoolean("status");
                    SynchronizeBikerStatus(status);
                }
            }
        });
    }

    private void showConnectionErrorDialog() {
        if(!connection_msg_read) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setTitle("Connection error!");
            alertBuilder.setMessage(getString(R.string.alert_connection_start));
            alertBuilder.setCancelable(false);
            alertBuilder.setPositiveButton("OK", (dialog, which) -> {
                connection_msg_read = true;
            });
            alertBuilder.create().show();
        }
    }

    private void listenForBikerStatus() {
        EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
            if (e != null)
                return;
            if(!documentSnapshot.getMetadata().isFromCache()) {
                Boolean status = documentSnapshot.getBoolean("status");
                SynchronizeBikerStatus(status);
                changing_status = false;
                if (bsListenerRegistration != null) {
                    bsListenerRegistration.remove();
                    bsListenerRegistration = null;
                }
            }
        };
        if (bsListenerRegistration == null ) {
            bsListenerRegistration = db.collection("bikers").document(bikerKey)
                    .addSnapshotListener(MetadataChanges.INCLUDE, eventListener);
            // MetadataChanges.INCLUDE: you will receive another snapshot with isFomCache()
            // equal to false once the client has received up-to-date data from the backend
        }
    }

    private void setLayoutDelivery() {
        cvNoDelivery.setVisibility(View.INVISIBLE);
        reservationLinearLayout.setVisibility(View.VISIBLE);
    }

    private void setLayoutNoDelivery() {
        reservationLinearLayout.setVisibility(View.INVISIBLE);
        cvNoDelivery.setVisibility(View.VISIBLE);
    }

    private void updateStatus(Boolean status) {
        biker_status = status;
        if (status) {
            tvNoDelivery.setText(R.string.msg_no_delivery);
        } else {
            tvNoDelivery.setText(R.string.msg_status_off);
        }
        invalidateOptionsMenu();
    }

    private void checkTrackingService(boolean status) {
        if(status) {
            Intent intent = new Intent(this, TrackingService.class);
            startService(intent);
        }
    }

    private void SynchronizeBikerStatus(Boolean status) {
        pbInitialSynchronization.setVisibility(View.GONE);
        read_status = true;
        updateStatus(status);
        checkTrackingService(status);
    }

    private void startLocationActivity() {
        Intent location = new Intent(this, LocationActivity.class);
        Bundle bn = new Bundle();
        bn.putBoolean("biker_status", biker_status);
        bn.putBoolean("changing_status", changing_status);
        bn.putBoolean("updating_status", updating_status);
        location.putExtras(bn);
        startActivityForResult(location, WORKING_STATUS);
    }

    private void removeNewDeliveryNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(unique_id);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @android.support.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if(requestCode == WORKING_STATUS) {
                if(data != null) {
                    Bundle bn = data.getExtras();
                    if (bn != null) {
                        Boolean status = bn.getBoolean("biker_status");
                        changing_status = bn.getBoolean("changing_status");
                        updating_status = bn.getBoolean("updating_status");
                        SynchronizeBikerStatus(status);
                        if(changing_status) {
                            listenForBikerStatus();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.d("VITA", String.valueOf(read_status));
        outState.putBoolean("connection_msg_read", connection_msg_read);
        outState.putBoolean("read_status", read_status);
        outState.putBoolean("biker_status", biker_status);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(MyReceiver);
    }

    private void writeBikerDeliveryStatistics(Timestamp timestamp) {
        Map<String, Object> resStatistics = new HashMap<>();
        resStatistics.put("biker_key", bikerKey);
        resStatistics.put("timestamp", timestamp);
        Double distance = restaurantDistance + userDistance;
        resStatistics.put("distance", distance);
        db.collection("bikers_statistics").document().set(resStatistics).addOnSuccessListener(aVoid -> Log.d("MACT", "Statistics correctly write to server"));
    }
}
