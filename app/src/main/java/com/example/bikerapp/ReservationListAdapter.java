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
    }

    @Override
    public int getItemCount() {
        return reservationDataSet.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvReservationid;
        TextView tvNameRest;
        TextView tvAddrRest;
        TextView tvAddrUser;
        TextView tvUserName;
        TextView tvUserNotes;

        ReservationViewHolder(View itemView) {
             super(itemView);

             tvReservationid = itemView.findViewById(R.id.textViewReservationId);
             tvNameRest = itemView.findViewById(R.id.textViewRestaurantName);
             tvAddrRest = itemView.findViewById(R.id.textViewRestaurantAddress);
             tvUserName = itemView.findViewById(R.id.textViewUserName);
             tvAddrUser = itemView.findViewById(R.id.textViewUserAddress);
             tvUserNotes = itemView.findViewById(R.id.textViewUserNotes);
        }
    }

}
