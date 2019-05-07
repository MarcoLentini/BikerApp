package com.example.bikerapp.Information;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bikerapp.MainActivity;
import com.example.bikerapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.support.constraint.Constraints.TAG;

public class RegisterBikerActivity extends Activity {
    private EditText inputName, inputAddr, inputDescr;
    private Button btnSignUp;
    private View btnImage;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    private static final int CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 3;
    private static final int STORAGE_PERMISSION_CODE = 4;
    private static final int CAMERA_PERMISSION_CODE = 5;
    private static final int PICK_IMAGE_REQUEST = 5;

    private Uri restaurant_image = null;
    private Uri file_image = null;
    private static final String AuthorityFormat = "%s.fileprovider";

    private static final String restaurantDataFile = "BikerDataFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_biker);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        btnSignUp = findViewById(R.id.complete_biker_btn);
        inputName = findViewById(R.id.biker_name);
        inputAddr = findViewById(R.id.biker_phone);
        inputDescr = findViewById(R.id.biker_descr);
        progressBar = findViewById(R.id.progressBarBiker);
        btnImage = findViewById(R.id.background_img_biker);

        btnImage.setOnClickListener(v-> invokeDialogImageProfile());

        btnSignUp.setOnClickListener(v -> {

            String name = inputName.getText().toString().trim();
            String address = inputAddr.getText().toString().trim();
            String description = inputDescr.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getApplicationContext(), "Enter your name!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(address)) {
                Toast.makeText(getApplicationContext(), "Enter your address!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(description)) {
                Toast.makeText(getApplicationContext(), "Enter a short description!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (restaurant_image == null){
                Toast.makeText(getApplicationContext(), "Select an Image", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            String userID = auth.getCurrentUser().getUid();


            final Map<String, Object> restaurant = new HashMap<>();
            restaurant.put("user_id", userID);
            restaurant.put("rest_address", address);
            restaurant.put("rest_name", name);
            restaurant.put("rest_descr", description);
            restaurant.put("rest_image", restaurant_image.toString());

            // Get a new write batch
            WriteBatch batch = db.batch();

            DocumentReference restDRef = db.collection("restaurant").document();
            batch.set(restDRef, restaurant);

            DocumentReference userDRef = db.collection("users").document(userID);
            batch.update(userDRef, "rest_id", restDRef.getId());


            // Commit the batch
            batch.commit().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    SharedPreferences sharedPref = getSharedPreferences(restaurantDataFile, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("restaurantKey", restDRef.getId());
                    editor.commit();
                    Intent intent = new Intent(RegisterBikerActivity.this, BikerInformationActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("RegisterRest", "Failed batch write");
                }
            });

        });
    }

    private void invokeDialogImageProfile(){
        final String[] items = { getString(R.string.take_a_picture), getString(R.string.pick_from_gallery), getString(R.string.cancel_string)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            if(intent.resolveActivity(getPackageManager()) != null){
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e("[Camera Error]", ex.getMessage());
                }

                if(photoFile != null){
                    String authority = String.format(Locale.getDefault(), AuthorityFormat, this.getPackageName());
                    file_image = FileProvider.getUriForFile(this, authority, photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, file_image);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, CAMERA_REQUEST);
                }
            }
        }
    }
    private void invokeGallery(){
        if (hasPermission("Storage")) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if(requestCode == CAMERA_REQUEST)  {
                uploadOnFirebase(file_image);
            }
            if(requestCode == GALLERY_REQUEST){
                uploadOnFirebase(data.getData());
            }
        }
    }

    private void uploadOnFirebase(Uri fileUri){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference photoRef = storageRef.child("photos/" + auth.getCurrentUser().getUid())
                .child("ImageProfile.jpg");

        photoRef.putFile(fileUri).continueWithTask(task -> {
            // Forward any exceptions
            if (!task.isSuccessful())
                throw task.getException();

            Log.d(TAG, "uploadFromUri: upload success");

            // Request the public download URL
            return photoRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            // Upload succeeded
            Log.d(TAG, "uploadFromUri: getDownloadUri success");
            restaurant_image = downloadUri;
            Glide.with(this).load(restaurant_image).placeholder(R.drawable.img_biker_1).into((ImageView) findViewById(R.id.biker_image));
            try {
                deleteImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(exception -> {
            // Upload failed
            Log.w(TAG, "uploadFromUri:onFailure", exception);
        });
    }

    public void deleteImage() throws IOException {
        File fdelete = createImageFile();
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                callBroadCast();
            } else {
            }
        }
    }

    private void callBroadCast() {
        if (Build.VERSION.SDK_INT >= 14) {
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStorageDirectory().toString()}, null, (path, uri) -> {
                Log.e("ExternalStorage", "Scanned " + path + ":");
                Log.e("ExternalStorage", "-> uri=" + uri);
            });
        } else {
            Log.e("-->", " < 14");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    /** Permission Function **/

    // Image profile still valid?
    private boolean imageProfileIsValid() {
        File img_prof = null;
        try {
            img_prof = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img_prof.exists();
    }
    // For Image Profile -- URI
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "ImageProfile";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir + File.separator + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        //currentPhotoPath = image.getAbsolutePath();
        return image;
    }

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
        int result = ContextCompat.checkSelfPermission(this, perm);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission(String type) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.perm_needed))
                    .setMessage(getString(R.string.perm_why_1))
                    .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE))
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
                    .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE))
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
}