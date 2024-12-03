package com.example.loginscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.text.InputType;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUp";
    private EditText usernameEditText, passwordEditText, emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        
        Log.d(TAG, "SignUp Activity onCreate called");
        
        // Initialize back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // Navigate back to Home activity
            Intent intent = new Intent(SignUp.this, Home.class);
            startActivity(intent);
            finish();
        });

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        emailEditText = findViewById(R.id.email);

        // Initialize password toggle
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        passwordLayout.setEndIconOnClickListener(v -> {
            // Toggle password visibility
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show password
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // Hide password
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Move cursor to end of text
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // Add email validation listener
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail(emailEditText.getText().toString());
            }
        });

        // Test server connection on startup
        testServerConnection();

        // Handle sign-up button click
        findViewById(R.id.signupButton).setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection available", Toast.LENGTH_LONG).show();
                return;
            }

            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validateEmail(email)) {
                return;
            }

            registerUser(username, password, email);
        });

        // Handle login navigation
        TextView loginTextView = findViewById(R.id.loginPrompt);
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click in action bar
            Intent intent = new Intent(SignUp.this, Home.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void testServerConnection() {
        String url = Constants.REGISTER_URL;
        Log.d(TAG, "Testing connection to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> Log.d(TAG, "Server is reachable"),
                error -> {
                    Log.e(TAG, "Server error: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,  // 5 second timeout
                0,     // no retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return false;
        }
        emailEditText.setError(null);
        return true;
    }

    private void registerUser(String username, String password, String email) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!validateEmail(email)) {
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);
            jsonBody.put("email", email);

            String url = Constants.BASE_URL + "api/users/register/";
            Log.d(TAG, "Registration URL: " + url);
            Log.d(TAG, "Registration data: " + jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        Log.d(TAG, "Registration successful: " + response.toString());
                        Toast.makeText(SignUp.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUp.this, Login.class);
                        startActivity(intent);
                        finish();
                    },
                    this::handleRegistrationError
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(this).getRequestQueue().add(request);

        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage(), e);
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleRegistrationError(VolleyError error) {
        String errorMessage = "Registration failed";
        Log.e(TAG, "Registration error", error);

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "Error Status Code: " + statusCode);

            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                Log.e(TAG, "Error Response Body: " + responseBody);
                
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("error")) {
                    errorMessage = errorJson.getString("error");
                } else if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response", e);
                if (statusCode == 404) {
                    errorMessage = "Server not found. Please check the server URL.";
                } else if (statusCode == 400) {
                    errorMessage = "Invalid registration data.";
                } else {
                    errorMessage = "Server error: " + statusCode;
                }
            }
        } else {
            // No network response
            if (error instanceof TimeoutError) {
                errorMessage = "Connection timed out. Please check the server URL.";
            } else if (error instanceof NoConnectionError) {
                errorMessage = "Cannot connect to server. Please check the URL and your network connection.";
            } else {
                errorMessage = "Network error: " + error.getMessage();
            }
            Log.e(TAG, "Network Error Type: " + error.getClass().getSimpleName());
            Log.e(TAG, "Network Error Message: " + error.getMessage());
        }

        final String finalErrorMessage = errorMessage;
        runOnUiThread(() -> {
            Toast.makeText(SignUp.this, finalErrorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Final Error Message: " + finalErrorMessage);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Home activity
        Intent intent = new Intent(SignUp.this, Home.class);
        startActivity(intent);
        finish();
    }



    
}
