package com.example.bikerapp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

public class CodePickerDialog extends DialogFragment {
    private static final int DIGITS = 4;
    private ISelectedCode mCallback;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final NumberPicker[] numberPickers = new NumberPicker[DIGITS];

        for(int i=0; i< DIGITS; i++) {
            numberPickers[i] = new NumberPicker(getActivity());
            numberPickers[i].setMinValue(0);
            numberPickers[i].setMaxValue(9);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("CODE REQUESTED");
        builder.setMessage("Ask the user for the CONFIRMATION CODE and insert it to conclude your delivery:");

        builder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String code = "";
                for(int i=0; i< DIGITS; i++) {
                    code += numberPickers[i].getValue();
                }
                mCallback.onSelectedCode(code);
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        for(int i=0; i< DIGITS; i++)
            linearLayout.addView(numberPickers[i]);

        builder.setView(linearLayout);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (ISelectedCode) getActivity();
    }
}
