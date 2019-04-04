package com.example.bikerapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhoneNumber;
    private TextView tvUserDescription;
    private TextView tvUserPassword;
    private String userName;
    private String userEmail;
    private String userPhoneNumber;
    private String userDescription;
    private String userPassword;
    private ImageView imageProfile;
    private Uri uriSelectedImage = null;

    private SharedPreferences sharedPref;
    private static final String userFile = "UserDataFile";

    private static final int SECOND_ACTIVITY = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 3;
    private static final int STORAGE_PERMISSION_CODE = 4;
    private static final int CAMERA_PERMISSION_CODE = 5;
    private static final String AuthorityFormat = "%s.fileprovider";

    // Unbalanced placeholders because i like more 1 and 2 ;)
    private int[] placeholders = { R.drawable.img_biker_1, R.drawable.img_biker_2, R.drawable.img_biker_3, R.drawable.img_biker_4 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String title=getString(R.string.InfoTitle);
        getSupportActionBar().setTitle(title);

        // Image Profile
        imageProfile = findViewById(R.id.img_profile);
        imageProfile.setImageResource(placeholders[(int)(Math.random()*placeholders.length)]);
        ImageView imageAddButton = findViewById(R.id.background_img);
        imageAddButton.setOnClickListener(v -> {
            invokeDialogImageProfile();
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

        tvUserPassword = findViewById(R.id.textViewChangePassword);
        tvUserPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userPassword.equals(""))
                    invokeChangePwdActivity(getString(R.string.pwd_field_id));
                else{
                    String idField = getString(R.string.pwd_field_id);
                    invokeModifyInfoActivity(idField, userPassword);

                }
            }
        });

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
            tvUserDescription.setText(R.string.description_title);
        userPassword = sharedPref.getString("userPassword", "");
        uriSelectedImage = Uri.parse(sharedPref.getString("userImage", ""));
        if(!uriSelectedImage.toString().equals("")) {
            imageProfile.setImageURI(uriSelectedImage);
            if(imageProfile.getDrawable() == null)
                imageProfile.setImageResource(placeholders[(int)(Math.random()*placeholders.length)]);
        }
    }

    /** Invoke Intent **/

    // Open dialog to choose if take a selfie or pick a photo on gallery
    private void invokeDialogImageProfile(){
        final String[] items = { getString(R.string.take_a_picture), getString(R.string.pick_from_gallery), getString(R.string.cancel_string)};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.select_photo));
        builder.setItems(items, (d, i) -> {
            if (items[i].equals(getString(R.string.take_a_picture))) {
                invokeTakePicture();
            } else if (items[i].equals(getString(R.string.pick_from_gallery))) {
                invokeGallery();
            } else if (items[i].equals(getString(R.string.cancel_string))) {
                d.dismiss();
            }
        });
        builder.show();
    }
    private void invokeTakePicture(){
        if(hasPermission("Camera")){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            uriSelectedImage = setUriForImage(getApplicationContext());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSelectedImage);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }
    private void invokeGallery(){
        if (hasPermission("Storage")) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST);
        }
    }
    private void invokeModifyInfoActivity(String fieldName, String fieldNameValue) {
        Intent intent = new Intent(getApplicationContext(), ModifyInfoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("field", fieldName);
        bundle.putString("value", fieldNameValue);
        intent.putExtras(bundle);
        startActivityForResult(intent, SECOND_ACTIVITY);
    }
    private void invokeChangePwdActivity( String fieldName) {
        Intent intent = new Intent(getApplicationContext(), ChangePwdActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("field", fieldName);

        intent.putExtras(bundle);
        startActivityForResult(intent, SECOND_ACTIVITY);
    }

    /** On Result **/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences.Editor editor = sharedPref.edit();
        if(resultCode == RESULT_OK) {
            if (requestCode == SECOND_ACTIVITY) {
                switch (data.getExtras().getString("field")) {
                    case "user_name":
                        userName = data.getExtras().getString("value");
                        if (!userName.equals("")) {
                            editor.putString("userName", userName);
                            editor.commit();
                            tvUserName.setText(userName);
                        }
                        break;
                    case "user_email":
                        userEmail = data.getExtras().getString("value");
                        if (!userEmail.equals("")) {
                            editor.putString("userEmail", userEmail);
                            editor.commit();
                            tvUserEmail.setText(userEmail);
                        }
                        break;
                    case "user_phone_number":
                        userPhoneNumber = data.getExtras().getString("value");
                        if (!userPhoneNumber.equals("")) {
                            editor.putString("userPhoneNumber", userPhoneNumber);
                            editor.commit();
                            tvUserPhoneNumber.setText(userPhoneNumber);
                        }
                        break;
                    case "user_description":
                        userDescription = data.getExtras().getString("value");
                        if (!userDescription.equals("")) {
                            editor.putString("userDescription", userDescription);
                            editor.commit();
                            tvUserDescription.setText(R.string.description_title);
                        }
                        break;
                    case "user_password":
                        userPassword = data.getExtras().getString("value");
                        if (!userPassword.equals("")) {
                            editor.putString("userPassword", userPassword);
                            editor.commit();
                        }

                        break;
                }
            } else if (requestCode == CAMERA_REQUEST) {
                editor.putString("userImage", uriSelectedImage.toString());
                editor.commit();
                imageProfile.setImageURI(uriSelectedImage);
            } else if (requestCode == GALLERY_REQUEST) {
                uriSelectedImage = data.getData();
                editor.putString("userImage", uriSelectedImage.toString());
                editor.commit();
                imageProfile.setImageURI(uriSelectedImage);
            }
        }
    }

    /** On Resume **/

    @Override
    protected void onResume() {
        super.onResume();
        if(uriSelectedImage == null || uriSelectedImage.toString().equals(""))
            imageProfile.setImageResource(placeholders[(int)(Math.random()*placeholders.length)]);
        else{
            imageProfile.setImageURI(uriSelectedImage);
            if(imageProfile.getDrawable() == null)
                imageProfile.setImageResource(placeholders[(int)(Math.random()*placeholders.length)]);
        }
    }

    /** Permission Function **/

    private boolean hasPermission(String perm){
        if (Build.VERSION.SDK_INT >= 23) {
            if(perm.equals("Storage")) {
                if (checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    return true;
                else
                    requestStoragePermission("Gallery");
            } else if(perm.equals("Camera")){
                if (checkPermission(android.Manifest.permission.CAMERA) && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    return true;
                else
                    requestCameraPermission();
            }
        }
        else {
            return true;
        }
        return false;
    }

    private boolean checkPermission(String perm) {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, perm);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission(String type) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.perm_needed))
                    .setMessage(getString(R.string.perm_why_1))
                    .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE))
                    .setNegativeButton(getString(R.string.cancel_string), (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private void requestCameraPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.perm_needed))
                    .setMessage(getString(R.string.perm_why_2))
                    .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE))
                    .setNegativeButton(getString(R.string.cancel_string), (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    invokeGallery();
                else
                    Toast.makeText(this, getString(R.string.perm_denied), Toast.LENGTH_SHORT).show();
                break;
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    invokeTakePicture();
                else
                    Toast.makeText(this, getString(R.string.perm_denied), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /** Helper Function **/

    // For Image Profile -- URI
    private static Uri setUriForImage(Context context) {
        String authority = String.format(Locale.getDefault(), AuthorityFormat, context.getPackageName());
        return FileProvider.getUriForFile(context, authority, getStoragePathForFile());
    }

    private static File getStoragePathForFile(){
        String timeStamp =  new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if(!storageDir.exists())
            if(!storageDir.mkdirs()) {
                Log.e("IMAGE PROFILE", "Error image photo!");
                return null;
            }
        return new File(storageDir.getPath() + File.separator + imageFileName);
    }
}
