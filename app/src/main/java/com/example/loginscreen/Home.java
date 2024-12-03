package com.example.loginscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import com.example.loginscreen.workers.DeadlineWorker;
import com.example.loginscreen.helpers.NotificationHelper;

import java.util.concurrent.TimeUnit;

public class Home extends AppCompatActivity {
    private static final String TAG = "Home";
    private Button Login, Signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_home);
        Log.d(TAG, "onCreate started");

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);

        // Schedule deadline checks
        scheduleDeadlineChecks();

        try {
            // Initialize buttons
            Signup = findViewById(R.id.signup);
            Login = findViewById(R.id.login);
            
            if (Signup == null) {
                Log.e(TAG, "Signup button not found!");
            } else {
                Log.d(TAG, "Signup button found successfully");
            }

            Signup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Signup button clicked");
                    try {
                        Intent intent = new Intent();
                        intent.setClassName(getPackageName(), getPackageName() + ".SignUp");
                        Log.d(TAG, "Created intent for SignUp activity with class: " + getPackageName() + ".SignUp");
                        
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                            Log.d(TAG, "Started SignUp activity");
                        } else {
                            Log.e(TAG, "SignUp activity not found in manifest");
                            Toast.makeText(Home.this, "Error: SignUp activity not found", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting SignUp activity: ", e);
                        Toast.makeText(Home.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            Login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Login button clicked");
                    try {
                        Intent intent = new Intent(Home.this, Login.class);
                        startActivity(intent);
                        Log.d(TAG, "Started Login activity");
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting Login activity: ", e);
                        Toast.makeText(Home.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleDeadlineChecks() {
        try {
            Log.d(TAG, "Scheduling deadline checks");
            
            // Create constraints
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

            // Create periodic work request that runs every 12 hours
            PeriodicWorkRequest deadlineCheckRequest =
                new PeriodicWorkRequest.Builder(DeadlineWorker.class,
                    12, TimeUnit.HOURS)  // Check twice per day
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES) // Start first check after 1 minute
                    .build();

            // Schedule the work
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "deadline_check",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    deadlineCheckRequest
                );

            Log.d(TAG, "Deadline checks scheduled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling deadline checks: ", e);
        }
    }
    
}