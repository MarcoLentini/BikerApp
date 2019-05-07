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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bikerapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, BikerInformationActivity.class));
            finish();
        }

        // set the view now
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        btnSignup = findViewById(R.id.btn_signup);
        btnLogin = findViewById(R.id.btn_login);
        btnReset = findViewById(R.id.btn_reset_password_log);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

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

            //authenticate user
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, task -> {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        progressBar.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            // there was an error
                            if (password.length() < 6) {
                                inputPassword.setError(getString(R.string.minimum_password));
                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
                            }
                        } else {


                            FirebaseFirestore.getInstance().collection("user").document(auth.getCurrentUser().getUid()).get()
                                    .addOnCompleteListener(taskBikerId -> {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = taskBikerId.getResult();
                                            if (document.exists()) {
                                                String bikerID = (String) document.get("biker_id");
                                                if (bikerID != null) {
                                                    SharedPreferences sharedPref = getSharedPreferences(bikerDataFile, Context.MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                    editor.putString("bikerKey", bikerID);
                                                    editor.commit();
                                                    Intent intent = new Intent(LoginActivity.this, BikerInformationActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    // TODO first time registration of biker
                                                    new AlertDialog.Builder(this)
                                                            .setTitle(getString(R.string.became_biker))
                                                            .setMessage(getString(R.string.became_biker2))
                                                            .setPositiveButton(getString(R.string.ok_string), (dialog, which) -> {
                                                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                                DocumentReference bikerDRef = db.collection("bikers").document();

                                                                Map<String, Object> biker = new HashMap<>();
                                                                biker.put("user_id", auth.getCurrentUser().getUid());
                                                                biker.put("status", "disabled");
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
                                                                                                Intent retIntent;
                                                                                                retIntent = new Intent(getApplicationContext(), BikerInformationActivity.class);
                                                                                                startActivity(new Intent(LoginActivity.this, BikerInformationActivity.class));
                                                                                                finish();
                                                                                            });
                                                                                } else {

                                                                                    Toast.makeText(LoginActivity.this, "Insert biker key failed." + task.getException(),
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
                                            }
                                        } else {
                                            Log.d("BikerID", "get failed with ", task.getException());
                                        }
                                    });

                        }
                    });
        });


    }
    //sign out method
    public void signOut() {
        auth.signOut();

    }
}
