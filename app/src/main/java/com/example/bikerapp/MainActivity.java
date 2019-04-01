package com.example.bikerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhoneNumber;
    private TextView tvUserDescription;
    private String userName;
    private String userEmail;
    private String userPhoneNumber;
    private String userDescription;
    private ImageView imageProfile;
    private Uri uriSelectedImage;

    private SharedPreferences sharedPref;
    private static final String userFile = "UserDataFile";
    private static final int SECOND_ACTIVITY = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 3;
    private static final int STORAGE_PERMISSION_CODE = 4;
    private static final String AuthorityFormat = "%s.fileprovider";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddButton = findViewById(R.id.img_plus);
        imageAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = {"Take a picture", "Pick from gallery", "Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select photo");
                builder.setItems(items, (d, i) -> {
                    if(items[i].equals("Take a picture")) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        uriSelectedImage = setImageUri(getApplicationContext());
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSelectedImage);
                        startActivityForResult(intent, CAMERA_REQUEST);
                    } else if (items[i].equals("Pick from gallery")) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, GALLERY_REQUEST);
                    } else if (items[i].equals("Cancel")) {
                        d.dismiss();
                    }
                });
                builder.show();
            }
        });

        tvUserName = findViewById(R.id.textViewUserName);
        tvUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String idField = getString(R.string.name_field_id);
                invokeModifyInfoActivity(idField, userName);
            }
        });

        tvUserEmail = findViewById(R.id.textViewUserEmail);
        tvUserEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String idField = getString(R.string.email_field_id);
                invokeModifyInfoActivity(idField, userEmail);
            }
        });

        tvUserPhoneNumber = findViewById(R.id.textViewUserPhoneNumber);
        tvUserPhoneNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String idField = getString(R.string.phone_number__field_id);
                invokeModifyInfoActivity(idField, userPhoneNumber);
            }
        });

        tvUserDescription = findViewById(R.id.textViewUserDescription);
        tvUserDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String idField = getString(R.string.description_field_id);
                invokeModifyInfoActivity(idField, userDescription);
            }
        });

        imageProfile = findViewById(R.id.img_profile);
        sharedPref = getSharedPreferences(userFile, Context.MODE_PRIVATE);
        userName = sharedPref.getString("userName", "");
        if(!userName.equals(""))
            tvUserName.setText(userName);
        userEmail = sharedPref.getString("userEmail", "");
        if(!userEmail.equals(""))
            tvUserEmail.setText(userEmail);
        userPhoneNumber = sharedPref.getString("userPhoneNumber", "");
        if(!userPhoneNumber.equals(""))
            tvUserPhoneNumber.setText(userPhoneNumber);
        userDescription = sharedPref.getString("userDescription", "");
        if(!userDescription.equals(""))
            tvUserDescription.setText(userDescription);
        uriSelectedImage = Uri.parse(sharedPref.getString("userImage", ""));
        if(!uriSelectedImage.equals("")) {
            imageProfile.setImageURI(uriSelectedImage);
        }

    }

    private void invokeModifyInfoActivity(String fieldName, String fieldNameValue) {
        Intent intent = new Intent(getApplicationContext(), ModifyInfoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("field", fieldName);
        bundle.putString("value", fieldNameValue);
        intent.putExtras(bundle);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences.Editor editor = sharedPref.edit();
        if(resultCode == SECOND_ACTIVITY) {
            switch (data.getExtras().getString("field")) {
                case "user_name":
                    userName = data.getExtras().getString("value");
                    if(!userName.equals("")) {
                        editor.putString("userName", userName);
                        editor.commit();
                        tvUserName.setText(userName);
                    }
                    break;
                case "user_email":
                    userEmail = data.getExtras().getString("value");
                    if(!userEmail.equals("")) {
                        editor.putString("userEmail", userEmail);
                        editor.commit();
                        tvUserEmail.setText(userEmail);
                    }
                    break;
                case "user_phone_number":
                    userPhoneNumber = data.getExtras().getString("value");
                    if(!userPhoneNumber.equals("")) {
                        editor.putString("userPhoneNumber", userPhoneNumber);
                        editor.commit();
                        tvUserPhoneNumber.setText(userPhoneNumber);
                    }
                    break;
                case "user_description":
                    userDescription = data.getExtras().getString("value");
                    if(!userDescription.equals("")) {
                        editor.putString("userDescription", userDescription);
                        editor.commit();
                        tvUserDescription.setText(userDescription);
                    }
                    break;
            }
        } else if(resultCode == CAMERA_REQUEST) {
            imageProfile.setImageURI(uriSelectedImage);
        } else if(resultCode == GALLERY_REQUEST) {
            uriSelectedImage = data.getData();
            imageProfile.setImageURI(uriSelectedImage);
        }
    }

    private static Uri setImageUri(Context context) {
        String autorithy = String.format(Locale.getDefault(), AuthorityFormat, context.getPackageName());
        return FileProvider.getUriForFile(context, autorithy, () -> {
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
            String imageFileName = "IMG_" + timeStamp + "_";
            return (File) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        });
    }
}
