package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.andrewtakao.alight.databinding.ActivityLoginBinding;
import com.andrewtakao.alight.databinding.ActivitySignupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    private final String TAG = SignupActivity.class.getSimpleName();
    ActivitySignupBinding binding;
    Context context;
    final int minLength = 5;
    FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        database = Utils.getDatabase();
        context = getBaseContext();
        mAuth = FirebaseAuth.getInstance();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup);

        binding.mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });

        binding.btSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.etEmail.getText().toString();
                String password = binding.etPassword.getText().toString();
                String confirmPassword = binding.etConfirmPassword.getText().toString();
                String username = binding.etUsername.getText().toString();
                if (email.length()<=minLength){
                    Toast.makeText(context, "Email too short", Toast.LENGTH_LONG).show();
                }
                else if (password.length()<=minLength) {
                    Toast.makeText(context, "Password too short", Toast.LENGTH_LONG).show();
                }
                else if (confirmPassword.length()<=minLength) {
                    Toast.makeText(context, "Confirm Password too short", Toast.LENGTH_LONG).show();
                }
                else if (username.length()<=minLength) {
                    Toast.makeText(context, "Username too short", Toast.LENGTH_LONG).show();
                }
                else if (!password.equals(confirmPassword)) {
                    Toast.makeText(context, "Passwords don't match", Toast.LENGTH_LONG).show();
                }
                else {
                    Log.d(TAG,"Signin");
                    createUser(email, password, username);
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        updateUI(currentUser);
    }

    public void getCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
//            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();
        }
    }

    public void createUser(final String email, String password, final String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
// save the user's profile into Firebase so we can list users,
// use them in Security and Firebase Rules, and show profiles
                            String userId = user.getUid();
                            User userToDatabase = new User(userId, username);

                            database.getReference("users/" + userId).setValue(
                                    userToDatabase
                            );
                            Intent intent = new Intent(context, LanguageActivity.class);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(context, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }

                        // ...
                    }
                });
    }
}