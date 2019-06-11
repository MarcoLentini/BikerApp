package com.example.bikerapp.Information;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bikerapp.MainActivity;
import com.example.bikerapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private Button btnSignup, btnLogin, btnReset;
    private static final String bikerDataFile = "BikerDataFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        auth.signOut();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
        // set the view now
        setContentView(R.layout.activity_login);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        btnSignup = findViewById(R.id.btn_signup);
        btnLogin = findViewById(R.id.btn_login);
        btnReset = findViewById(R.id.btn_reset_password_log);


        btnSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        btnReset.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));

        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString();
            final String password = inputPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                 return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);

            //authenticate user
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, task -> {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        progressBar.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            // there was an error
                            if(!isValidEmail(email))
                                inputPassword.setError(getString(R.string.invalid_mail));
                            else if (!isValidPassword(password)) {
                                inputPassword.setError(getString(R.string.invalid_password));
                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            register_biker();
                        }
                    });
        });


    }

    private void register_biker(){
            FirebaseFirestore.getInstance().collection("users").document(auth.getCurrentUser().getUid()).get()
                    .addOnCompleteListener(taskBikerId -> {
                        if (taskBikerId.isSuccessful()) {
                            DocumentSnapshot document = taskBikerId.getResult();
                            if (document.exists()) {
                                String bikerID = (String) document.get("biker_id");
                                if (bikerID != null) {
                                    SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("bikerKey", bikerID);
                                    editor.commit();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {

                                    new AlertDialog.Builder(this)
                                            .setTitle(getString(R.string.became_biker))
                                            .setMessage(getString(R.string.became_biker2))
                                            .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> {
                                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                DocumentReference bikerDRef = db.collection("bikers").document();

                                                Map<String, Object> biker = new HashMap<>();
                                                biker.put("user_id", auth.getCurrentUser().getUid());
                                                biker.put("status", false);
                                                bikerDRef.set(biker)
                                                        .addOnSuccessListener(documentReference -> {
                                                            Map<String, Object> biker_id = new HashMap<>();
                                                            biker_id.put("biker_id", bikerDRef.getId());
                                                            db.collection("users").document(auth.getCurrentUser().getUid()).update(biker_id).addOnCompleteListener(task1 -> {
                                                                if (task1.isSuccessful()) {
                                                                    SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
                                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                                    editor.putString("bikerKey", bikerDRef.getId());
                                                                    editor.commit();

                                                                    new AlertDialog.Builder(this)
                                                                            .setTitle(getString(R.string.became_biker))
                                                                            .setMessage(getString(R.string.welcome_biker))
                                                                            .setPositiveButton(getString(R.string.ok_string), (dialog1, which1) -> {
                                                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                                                dialog.dismiss();
                                                                                finish();
                                                                            })
                                                                            .create()
                                                                            .show();
                                                                } else {

                                                                    Toast.makeText(LoginActivity.this, "Insert biker key failed." + task1.getException(),
                                                                            Toast.LENGTH_SHORT).show();
                                                                    dialog.dismiss();
                                                                }
                                                            });
                                                        });
                                            })
                                            .setNegativeButton(getString(R.string.cancel_string), (dialog, which) -> {
                                                signOut();
                                                dialog.dismiss();
                                            })
                                            .create().show();
                                }
                            } else {
                                Log.d("BikerID", "No such document");
                                Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();


                            }
                        } else {
                            Log.d("BikerID", "get failed with ", taskBikerId.getException());
                            Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();

                        }
                    });

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
    //sign out method
    public void signOut() {
        auth.signOut();

    }
}
