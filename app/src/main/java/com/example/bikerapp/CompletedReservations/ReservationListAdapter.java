package com.example.bikerapp.CompletedReservations;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bikerapp.R;
import com.example.bikerapp.ReservationModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


class ReservationListAdapter extends RecyclerView.Adapter<ReservationListAdapter.ReservationViewHolder>{

    private Context context;
    private ArrayList<ReservationModel> reservationDataSet;
    private LayoutInflater mInflater;
    private SimpleDateFormat dateFormat;

    public ReservationListAdapter(Context context, ArrayList<ReservationModel> reservationList) {
            this.context = context;
            this.mInflater = LayoutInflater.from(context);
            this.reservationDataSet = reservationList;
            this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
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
        holder.tvUserName.setText(reservationModel.getNameUser());
        Date date = reservationModel.getTimestamp().toDate();
        holder.tvTimestamp.setText(dateFormat.format(date));
        Double distance = reservationModel.getRestDist() + reservationModel.getUserDist();
        holder.tvKmTravelled.setText(String.valueOf(distance) + " km");
    }

    @Override
    public int getItemCount() {
        return reservationDataSet.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvReservationid;
        TextView tvNameRest;
        TextView tvUserName;
        TextView tvTimestamp;
        TextView tvKmTravelled;

        ReservationViewHolder(View itemView) {
             super(itemView);

             tvReservationid = itemView.findViewById(R.id.textViewReservationId);
             tvNameRest = itemView.findViewById(R.id.textViewRestaurantName);
             tvUserName = itemView.findViewById(R.id.textViewUserName);
             tvTimestamp = itemView.findViewById(R.id.textViewTimestamp);
             tvKmTravelled = itemView.findViewById(R.id.textViewKmTravelled);
        }
    }

}
