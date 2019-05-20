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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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
import java.util.Map;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    private ConstraintLayout constraintLayout;
    private String NOTIFICATION_CHANNEL_ID = "com.example.bikerapp";
    private int unique_id = 1;
    private NotificationCompat.Builder builder;

    private TextView tvReservationId;
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

        constraintLayout = findViewById(R.id.main_layout);
        tvReservationId = findViewById(R.id.textViewReservationId);
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
        fillWithData();

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

    private void startGoogleMaps(String delivery_address) {
        delivery_address = delivery_address.replace(" ","+");
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + delivery_address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void fillWithData() {
        db.collection("reservations").whereEqualTo("biker_id", bikerKey).addSnapshotListener((EventListener<QuerySnapshot>) (document, e) -> {

            if (e != null)
                return;

            // TODO questo andrà cambiato(chiedere ad Alberto come prendere l'unico eventuale ordine)
            // TODO invece gli ordini già completati vengono messi nella CompletedReservationsActivity dove c'è il recyclerview
            ReservationModel tmpReservationModel = null;
            for(DocumentChange dc : document.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
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

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(unique_id, builder.build());
                    unique_id++;
                }
            }

            if(tmpReservationModel != null) {
                tvReservationId.setText(String.valueOf(tmpReservationModel.getRsId()));
                tvRestaurantName.setText(tmpReservationModel.getNameRest());
                tvRestaurantAddress.setText(tmpReservationModel.getAddrRest());
                tvUserName.setText(tmpReservationModel.getNameUser());
                tvUserAddress.setText(tmpReservationModel.getAddrUser());
                tvUserNotes.setText(tmpReservationModel.getInfoUser());
                // TODO prendere l'immagine del ristorante e caricarla
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
                createNewMissionSnackBar();
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
            tvNewReservation.setVisibility(View.INVISIBLE);
        };
        Snackbar.make(constraintLayout, "New order to be delivered!", Snackbar.LENGTH_INDEFINITE)
                .setAction("GOT IT", snackBarListener).show();
    }
}
