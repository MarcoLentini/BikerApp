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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.support.constraint.Constraints.TAG;

public class SignupActivity extends AppCompatActivity {


    private static final int CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 3;
    private static final int STORAGE_PERMISSION_CODE = 4;
    private static final int CAMERA_PERMISSION_CODE = 5;
    private static final int PICK_IMAGE_REQUEST = 5;

    private Uri user_image = null;
    private Uri file_image = null;
    private static final String AuthorityFormat = "%s.fileprovider";



    private EditText inputEmail, inputPassword, inputUser,inputPhone;
    private Button btnSignIn, btnSignUp, btnResetPassword;
    private View btnImage;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private static final String bikerDataFile = "BikerDataFile";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        String title=getString(R.string.sign_up);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        btnSignIn = findViewById(R.id.sign_in_button1);
        btnSignUp = findViewById(R.id.sign_up_button1);
        inputEmail = findViewById(R.id.email_user1);
        inputPassword = findViewById(R.id.password_user1);
        inputUser = findViewById(R.id.user_name_user1);
        inputPhone = findViewById(R.id.user_phone_user1);
        progressBar = findViewById(R.id.progressBar1);

        btnSignIn.setOnClickListener(v -> finish());

        btnImage = findViewById(R.id.background_img_user1);

        btnImage.setOnClickListener(v -> invokeDialogImageProfile());
        btnSignUp.setOnClickListener(v -> {

            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String username = inputUser.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();


            if (TextUtils.isEmpty(username)) {
                Toast.makeText(getApplicationContext(), getString(R.string.insert_username), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidUsername(username)) {
                Toast.makeText(getApplicationContext(), getString(R.string.invalid_username), Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getApplicationContext(), getString(R.string.enter_email), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(getApplicationContext(), getString(R.string.invalid_mail), Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidPassword(password)) {
                Toast.makeText(getApplicationContext(), getString(R.string.invalid_password), Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(getApplicationContext(), getString(R.string.insert_phone_number), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPhone(phone)) {
                Toast.makeText(getApplicationContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show();
                return;
            }

      /*      if (user_image==null) {
                Toast.makeText(getApplicationContext(), "Enter Photo!", Toast.LENGTH_SHORT).show();
                btnImage.performClick();
                return;
            }*/
            progressBar.setVisibility(View.VISIBLE);
            //create user
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignupActivity.this, task -> {

                        progressBar.setVisibility(View.GONE);
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Authentication failed." + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                        } else {

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference bikerDRef = db.collection("bikers").document();

                            Map<String, Object> biker = new HashMap<>();
                            biker.put("user_id", auth.getCurrentUser().getUid());
                            biker.put("status", "disabled");
                            bikerDRef.set(biker)
                                    .addOnSuccessListener(documentReference -> {
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("username", username);
                                        user.put("email", email);
                                        user.put("phone", phone);
                                        user.put("biker_id",bikerDRef.getId());
                                    //    uploadOnFirebase(file_image);
                                      //  user.put("image_url", user_image.toString());
                                        db.collection("users").document(auth.getCurrentUser().getUid())
                                                .set(user)
                                                .addOnSuccessListener(documentReference1 -> {
                                                    SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                    editor.putString("bikerKey", bikerDRef.getId());
                                                    editor.commit();
                                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, getString(R.string.create_profile_failed), Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(SignupActivity.this,  getString(R.string.create_profile_failed), Toast.LENGTH_SHORT).show());


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
                user_image = file_image;
                Glide.with(this).load(user_image).placeholder(R.drawable.img_biker_1).into((ImageView) findViewById(R.id.img_profile));


            }
            if(requestCode == GALLERY_REQUEST){

                user_image = data.getData();
                Glide.with(this).load(user_image).placeholder(R.drawable.img_biker_1).into((ImageView) findViewById(R.id.img_profile));

            }
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
        if(user_image == null){
            user_image = file_image;
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

    public boolean isValidUsername(final String username) {

        if(username.length()>=3)
            return true;
        else return false;
    }

    public boolean isValidPassword(final String password) {

        Pattern pattern;
        Matcher matcher;

        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();

    }

    public boolean isValidEmail(final String email) {

        Pattern pattern;
        Matcher matcher;

        final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";;

        pattern = Pattern.compile(EMAIL_PATTERN);
        matcher = pattern.matcher(email);

        return matcher.matches();

    }
    public boolean isValidPhone(final String phone) {

        Pattern pattern;
        Matcher matcher;

        final String PHONE_PATTERN = "^[+]?[0-9]{10,13}$";

        pattern = Pattern.compile(PHONE_PATTERN);
        matcher = pattern.matcher(phone);

        return matcher.matches();

    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}