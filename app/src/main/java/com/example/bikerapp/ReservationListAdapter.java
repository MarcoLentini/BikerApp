package com.example.bikerapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bikerapp.Information.BikerInformationActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;


class ReservationListAdapter extends RecyclerView.Adapter<ReservationListAdapter.ReservationViewHolder>{

    private Context context;
    private ArrayList<ReservationModel> reservationDataSet;
    private LayoutInflater mInflater;

    public ReservationListAdapter(Context context, ArrayList<ReservationModel> reservationList) {
            this.context = context;
            this.mInflater = LayoutInflater.from(context);
            this.reservationDataSet = reservationList;
    }

    @Override
    public ReservationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = mInflater.inflate(R.layout.reservation_item, parent, false);

        ReservationViewHolder viewHolder = new ReservationViewHolder(view);
        viewHolder.tvAddrRest.setOnClickListener(v -> {
            startGoogleMaps(viewHolder.tvAddrRest.getText().toString());
        });
        viewHolder.tvAddrUser.setOnClickListener(v -> {
            startGoogleMaps(viewHolder.tvAddrUser.getText().toString());
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        ReservationModel reservationModel = reservationDataSet.get(position);

        holder.tvReservationid.setText(reservationModel.getRsId().toString());
        holder.tvNameRest.setText(reservationModel.getNameRest());
        holder.tvAddrRest.setText(reservationModel.getAddrRest());
        holder.tvUserName.setText(reservationModel.getNameUser());
        holder.tvAddrUser.setText(reservationModel.getAddrUser());
        holder.tvUserNotes.setText(reservationModel.getInfoUser());
        if(position == 0) {
            if(reservationModel.getStateNew())
                holder.tvGotIt.setVisibility(View.VISIBLE);
            else
                holder.tvGotIt.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return reservationDataSet.size();
    }

    private void startGoogleMaps(String delivery_address) {
        delivery_address = delivery_address.replace(" ","+");
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + delivery_address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvReservationid;
        TextView tvGotIt;
        TextView tvNameRest;
        TextView tvAddrRest;
        TextView tvAddrUser;
        TextView tvUserName;
        TextView tvUserNotes;

        ReservationViewHolder(View itemView) {
             super(itemView);

             tvReservationid = itemView.findViewById(R.id.textViewReservationId);
             tvGotIt = itemView.findViewById(R.id.textViewGotIt);
             tvNameRest = itemView.findViewById(R.id.textViewRestaurantName);
             tvAddrRest = itemView.findViewById(R.id.textViewRestaurantAddress);
             tvUserName = itemView.findViewById(R.id.textViewUserName);
             tvAddrUser = itemView.findViewById(R.id.textViewUserAddress);
             tvUserNotes = itemView.findViewById(R.id.textViewUserNotes);
        }
    }

}
