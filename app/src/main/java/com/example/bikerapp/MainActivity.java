package com.example.bikerapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bikerapp.Information.BikerInformationActivity;
import com.example.bikerapp.Information.LoginActivity;
import com.example.bikerapp.Location.LocationActivity;
import com.example.bikerapp.Location.TrackingService;
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
    private boolean biker_status = false;
    private String documentKey;
    private ListenerRegistration listenerRegistration;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("VITA", "onCreate(...) chiamato");
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
        // TODO call the following method only if it wasn't already called
        getAndUpdateBikerStatus();
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
            Bundle bn = new Bundle();
            bn.putBoolean("biker_status", biker_status);
            location.putExtras(bn);
            startActivityForResult(location, WORKING_STATUS);
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

            ReservationModel tmpReservationModel;
            for(DocumentChange dc : document.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    documentKey = dc.getDocument().getId();
                    DocumentSnapshot doc = dc.getDocument();
                    tmpReservationModel = new ReservationModel((Long) doc.get("rs_id"),
                            (String) doc.get("rest_name"),
                            (String) doc.get("rest_address"),
                            (String) doc.get("cust_address"),
                            (String) doc.get("notes"),
                            (String) doc.get("cust_name"),
                            (String) doc.get("rest_id"),
                            (String) doc.get("cust_id"),
                            (String) doc.get("cust_phone"),
                            (Timestamp) doc.get("delivery_time"));
                    if(doc.get("confirmation_code") != null) {
                        confirmationCode = (Long) doc.get("confirmation_code");
                        Log.d("conf", String.valueOf(confirmationCode));
                    }

                    Boolean biker_check = (Boolean)dc.getDocument().get("biker_check");
                    if(dc.getDocument().get("rs_status").equals("IN_PROGRESS") && biker_check){
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(unique_id, builder.build());
                    }

                    if(tmpReservationModel != null) {
                        tvReservationIdValue.setText(String.valueOf(tmpReservationModel.getRsId()));
                        tvRestaurantName.setText(tmpReservationModel.getNameRest());
                        tvRestaurantAddress.setText(tmpReservationModel.getAddrRest());
                        tvUserName.setText(tmpReservationModel.getNameUser());
                        tvUserAddress.setText(tmpReservationModel.getAddrUser());
                        tvUserNotes.setText(tmpReservationModel.getInfoUser());
                        String restaurantId = tmpReservationModel.getRestId();
                        db.collection("restaurant").document(restaurantId).get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {

                                        DocumentSnapshot documentResult = task.getResult();
                                        if (documentResult.exists()) {
                                            String restaurantImage = (String) documentResult.get("rest_image");
                                            Uri tmpUri = Uri.parse(restaurantImage);
                                            Glide.with(this).load(tmpUri).placeholder(R.drawable.img_biker_1).into(ivRestaurantLogo);
                                        } else {
                                            Log.d("QueryRestaurants", "No such document");
                                        }
                                    } else {
                                        Log.d("QueryRestaurants", "get failed with ", task.getException());
                                    }
                                });
                        if(dc.getDocument().get("rs_status").equals("IN_PROGRESS")){
                            createNewMissionSnackBar();
                        }
                        setLayoutDelivery();
                    }

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
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void createNewMissionSnackBar() {
        View.OnClickListener snackBarListener = v -> {
            db.collection("reservations").document(documentKey).update("biker_check", false);
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
            db.collection("reservations").document(documentKey).update("rs_status", "DELIVERED");
            db.collection("reservations").document(documentKey).update("is_current_order", false);
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
        db.collection("bikers").document(bikerKey).get(Source.SERVER).addOnCompleteListener(task -> {

            if(!task.isSuccessful()) { // TODO sostituire alert dialog con un normale progress che gira
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Connection error!");
                alertBuilder.setMessage(getString(R.string.alert_connection_start));
                alertBuilder.setCancelable(false);
                alertBuilder.setPositiveButton("OK", (dialog, which) -> {
                    EventListener<DocumentSnapshot> eventListener = new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            if (e != null)
                                return;
                            if(!documentSnapshot.getMetadata().isFromCache()) {
                                Boolean status = (Boolean) documentSnapshot.get("status");
                                SynchronizeBikerStatus(status);
                                if (listenerRegistration != null) {
                                    listenerRegistration.remove();
                                }
                            }
                        }
                    };
                    if (listenerRegistration == null ) {
                        listenerRegistration = db.collection("bikers").document(bikerKey)
                                .addSnapshotListener(MetadataChanges.INCLUDE, eventListener);
                    }
                });
                alertBuilder.create().show();

            } else {
                Boolean status = (Boolean) task.getResult().get("status");
                SynchronizeBikerStatus(status);
            }
        });
    }

    private void setLayoutDelivery() {
        tvNoDelivery.setVisibility(View.INVISIBLE);
        reservationLinearLayout.setVisibility(View.VISIBLE);
    }

    private void setLayoutNoDelivery() {
        reservationLinearLayout.setVisibility(View.INVISIBLE);
        tvNoDelivery.setVisibility(View.VISIBLE);
    }

    private void updateStatus(Boolean status) {
        if (status) {
            biker_status = true;
            tvNoDelivery.setText(R.string.msg_no_delivery);
            Toast.makeText(this, R.string.msg_tracking_activated,
                    Toast.LENGTH_LONG).show();
        } else {
            biker_status = false;
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
        read_status = true;
        updateStatus(status);
        checkTrackingService(status);
    }

    private void removeNewDeliveryNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(unique_id);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("VITA", "onStop(...) chiamato");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("VITA", "onDestroy(...) chiamato");
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
                        updateStatus(status);
                    }
                }
            }
        }
    }
}
