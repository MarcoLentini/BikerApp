package com.example.bikerapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bikerapp.Information.BikerInformationActivity;

import java.util.ArrayList;


class ReservationListAdapter extends RecyclerView.Adapter<ReservationListAdapter.ReservationViewHolder>{

    private Context context;
    private BikerInformationActivity fragmentActivity;
    private ArrayList<ReservationModel> reservationDataSet;
    private LayoutInflater mInflater;




    public ReservationListAdapter(Context context, ArrayList<ReservationModel> reservationList, MainActivity activity) {
            this.context = context;
            this.fragmentActivity = fragmentActivity;
            this.mInflater = LayoutInflater.from(context);
            this.reservationDataSet=reservationList;
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

        holder.nameRest.setText(reservationModel.getNameRest());
        holder.addrRest.setText(reservationModel.getAddrRest());
        holder.addrUser.setText(reservationModel.getAddrUser());
        holder.infoUser.setText(reservationModel.getInfoUser());
       // holder.timeRest.setText(reservationModel.getTimeRest());
        // holder.timeUser.setText(reservationModel.getTimeUser());
    }



    @Override
    public int getItemCount() {

        return reservationDataSet.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {



        TextView nameRest;
         TextView addrRest;
         TextView addrUser;
         TextView infoUser;
         TextView timeRest;
         TextView timeUser;
         ReservationViewHolder(View itemView) {

            super(itemView);

            nameRest = (TextView) itemView.findViewById(R.id.textViewRestaurantName);
            addrRest = (TextView) itemView.findViewById(R.id.textViewRestaurantAddress);
            addrUser = (TextView) itemView.findViewById(R.id.textViewUserAddress);
            infoUser = (TextView) itemView.findViewById(R.id.textViewUserInfo);
           // timeRest = (TextView) itemView.findViewById(R.id.textViewTimeRestaurant);
          //  timeUser = (TextView) itemView.findViewById(R.id.textViewTimeUser);

        }
    }

}
