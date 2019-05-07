package com.example.bikerapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class ReservationsMainFragment extends Fragment {
    private RecyclerView.Adapter ReservationsAdapter;

    private DatabaseReference databaseReference;
    public static final String Database_Path = "users";//TODO: inserire il path corretto
    private ArrayList<ReservationModel> ReservationList = new ArrayList<>();




    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reservations_main, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.reservationsRecyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        /*recyclerView.setHasFixedSize(true);*/
        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
// specify an Adapter
        ReservationsAdapter = new ReservationListAdapter(getContext(),
                MainActivity.ReservationsData, (MainActivity)getActivity());
        recyclerView.setAdapter(ReservationsAdapter);

        return view;
    }

}
