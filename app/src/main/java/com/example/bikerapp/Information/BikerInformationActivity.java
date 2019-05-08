package com.example.bikerapp.Information;

import android.Manifest;
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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bikerapp.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static android.support.constraint.Constraints.TAG;


public class BikerInformationActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhoneNumber, btnRemoveUser;
    private TextView btnSignOut;
    private TextView tvUserPassword;
    private String userName;
    private String userEmail;
    private String userPhoneNumber;

    private String userPassword;
    private ImageView imageProfile;
    private Uri uriSelectedImage;

    private SharedPreferences sharedPref;
    private static final String userFile = "UserDataFile";

    private Uri user_image = null;
    private Uri file_image = null;
    private UserInformationModel userInfo;
    private static final int SECOND_ACTIVITY = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 3;
    private static final int STORAGE_PERMISSION_CODE = 4;
    private static final int CAMERA_PERMISSION_CODE = 5;
    private static final String AuthorityFormat = "%s.fileprovider";

    private FirebaseFirestore db;
    private String bikerKey;
    private static final String bikerDataFile = "BikerDataFile";
    private FirebaseAuth auth;
    // Unbalanced placeholders because i like more 1 and 2 ;)
    private int[] placeholders = { R.drawable.img_biker_1, R.drawable.img_biker_2, R.drawable.img_biker_3, R.drawable.img_biker_4 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biker_information);
        String title=getString(R.string.InfoTitle);
        getSupportActionBar().setTitle(title);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
        bikerKey = sharedPref.getString("bikerKey","");

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || bikerKey.equals("")) {
            finish();
        }

        //Get Firestore instance
        db = FirebaseFirestore.getInstance();

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                                userInfo= new UserInformationModel(
                                        (String) doc.get("username"),
                                        (String) doc.get("email"),
                                        (String) doc.get("phone")
                                       // Uri.parse((String)doc.get("image_url"))

                                );

                                // Image Profile

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


                            tvUserPassword = findViewById(R.id.textViewChangePassword);
                            tvUserPassword.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(getApplicationContext(), ModifyInfoActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString("field", "user_password");
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                }
                            });
                            btnRemoveUser=findViewById(R.id.textViewRemoveUser);
                            btnRemoveUser.setOnClickListener(v -> {
                                if (user != null) {
                                    //TODO: function su firebase che controlla se l'utente ha rest_id o user_id e lo elimini anche dall'auth
                                    db.collection("bikers").document(bikerKey).delete()
                                            .addOnSuccessListener(taskBikerId -> {
                                                db.collection("users").whereEqualTo("biker_id",bikerKey).get()
                                                        .addOnSuccessListener(document->{
                                                            SharedPreferences.Editor editor = sharedPref.edit();
                                                            editor.remove("bikerKey");
                                                            Toast.makeText(BikerInformationActivity.this, "Your biker profile is deleted:( Create a account now!", Toast.LENGTH_SHORT).show();
                                                            signOut();
                                                            finish();
                                                        })
                                                        .addOnFailureListener(taskFailId -> {
                                                            Log.d("BikerInfo", "failed delete bikerKey");
                                                            Toast.makeText(BikerInformationActivity.this, "Failed to delete biker your account!", Toast.LENGTH_SHORT).show();
                                                        });

                                            })
                                            .addOnFailureListener(taskFailId -> {
                                                Log.d("BikerInfo", "failed delete biker");

                                                Toast.makeText(BikerInformationActivity.this, "Failed to delete biker your account!", Toast.LENGTH_SHORT).show();
                                            });

                                }
                            });
                            btnSignOut=findViewById(R.id.textViewLogOut);
                            btnSignOut.setOnClickListener(v -> signOut());

                            if(userInfo!=null) {
                                userName = userInfo.getName();
                                if (!userName.equals(""))
                                    tvUserName.setText(userName);
                                userEmail = userInfo.getMail();
                                if (!userEmail.equals(""))
                                    tvUserEmail.setText(userEmail);
                                userPhoneNumber = userInfo.getPhone();
                                if (!userPhoneNumber.equals(""))
                                    tvUserPhoneNumber.setText(userPhoneNumber);
         /*  uriSelectedImage = userInfo.getImage();
           Glide.with(this).load(uriSelectedImage).placeholder(R.drawable.img_biker_1).into((ImageView) findViewById(R.id.img_profile));
           try {
               deleteImage();
           } catch (IOException e) {
               e.printStackTrace();
           }*/
                            }
                        } else {
                            Log.d("QueryReservation", "No such document");
                        }
                    } else {
                        Log.d("QueryReservation", "get failed with ", task.getException());
                    }
                });


    }






    private void invokeModifyInfoActivity(String fieldName, String fieldNameValue) {
        Intent intent = new Intent(getApplicationContext(), ModifyInfoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("field", fieldName);
        bundle.putString("value", fieldNameValue);
        intent.putExtras(bundle);
        startActivityForResult(intent, SECOND_ACTIVITY);
    }

    /** On Result **/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if (requestCode == SECOND_ACTIVITY) {
                //TODO: togliere shared preferences
                switch (data.getExtras().getString("field")) {
                    case "user_name":
                        userName = data.getExtras().getString("value");
                        if (!userName.equals("")) {
                            userInfo.setName(userName);
                            tvUserName.setText(userName);
                        }
                        break;
                    case "user_email":
                        userEmail = data.getExtras().getString("value");
                        if (!userEmail.equals("")) {
                             userInfo.setMail(userEmail);
                            tvUserEmail.setText(userEmail);
                        }
                        break;
                    case "user_phone_number":
                        userPhoneNumber = data.getExtras().getString("value");
                        if (!userPhoneNumber.equals("")) {
                            userInfo.setPhone(userPhoneNumber);
                            tvUserPhoneNumber.setText(userPhoneNumber);
                        }
                        break;

                /*    case "user_password":
                        userPassword = data.getExtras().getString("value");
                        if (!userPassword.equals("")) {
                            editor.putString("userPassword", userPassword);
                            editor.commit();
                        }

                        break;*/
                }
            } else if (requestCode == CAMERA_REQUEST) {
                uploadOnFirebase(file_image);
            } else if (requestCode == GALLERY_REQUEST) {
                uploadOnFirebase(data.getData());
            }
        }
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


    private void uploadOnFirebase(Uri fileUri){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference photoRef = storageRef.child("photos/" + auth.getCurrentUser().getUid())
                .child("UserImage.jpg");

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
            user_image = downloadUri;
            Glide.with(this).load(user_image).placeholder(R.drawable.img_biker_1).into((ImageView) findViewById(R.id.img_profile));
            try {
                deleteImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //TODO: ricontrollare che la update venga eseguita(functions)
            Map<String, Object> user_im = new HashMap<>();
            user_im.put("image_url", user_image.toString());
            userInfo.setImage(user_image);
            db.collection("users").document(auth.getCurrentUser().getUid()).update(user_im);

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
        String imageFileName = "UserImage";
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //sign out method
    public void signOut() {
        auth.signOut();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() == null || bikerKey.equals("")) {

            finish();

        }
    }






}
