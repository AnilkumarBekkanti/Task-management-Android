package com.example.loginscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import android.text.InputType;
import com.google.android.material.textfield.TextInputLayout;

public class Login extends AppCompatActivity {
    private static final String TAG = "Login";
    private EditText usernameEditText, passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupPasswordToggle();
        setupClickListeners();
    }

    private void initializeViews() {
        requestQueue = Volley.newRequestQueue(this);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpPrompt);
    }

    private void setupPasswordToggle() {
        TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        passwordLayout.setEndIconOnClickListener(v -> {
            int inputType = passwordEditText.getInputType();
            if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> login());
        signUpTextView.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });
    }

    private void login() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loginUser(username, password);
    }

    private void loginUser(String username, String password) {
        try {
            // Skip the connectivity test and proceed directly with login
            proceedWithLogin(username, password);
        } catch (Exception e) {
            Log.e(TAG, "Error in login process", e);
            showError("Error during login process");
        }
    }

    private void proceedWithLogin(String username, String password) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);

            Log.d(TAG, "Attempting login request");
            Log.d(TAG, "Request URL: " + Constants.LOGIN_URL);
            
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, Constants.LOGIN_URL, jsonBody,
                    response -> {
                        Log.d(TAG, "Login response received: " + response.toString());
                        handleLoginSuccess(response);
                    },
                    error -> {
                        Log.e(TAG, "Login error occurred");
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e(TAG, "Error Status Code: " + statusCode);
                            try {
                                String responseBody = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Error Response Body: " + responseBody);
                                
                                // Handle specific status codes
                                if (statusCode == 401) {
                                    showError("Invalid username or password");
                                } else if (statusCode == 404) {
                                    showError("Login service not available");
                                } else {
                                    showError("Login failed: " + responseBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                                showError("Server error occurred");
                            }
                        } else {
                            Log.e(TAG, "No network response", error);
                            showError("Network error. Please check your connection.");
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Set timeout and retry policy
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.REQUEST_TIMEOUT,
                    Constants.MAX_RETRIES,
                    Constants.BACKOFF_MULTIPLIER
            ));
            
            requestQueue.add(jsonRequest);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating login request", e);
            showError("Error creating login request");
        }
    }

    private void handleLoginSuccess(JSONObject response) {
        try {
            Log.d(TAG, "Login response: " + response.toString());

            // Extract token with null check
            String token = response.optString("access", response.optString("token"));
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "No token in response");
                showError("Authentication failed");
                return;
            }

            // Extract user data with null checks
            JSONObject user = response.optJSONObject("user");
            if (user == null) {
                Log.e(TAG, "No user object in response");
                showError("Invalid server response");
                return;
            }

            // Get user details with default values
            String userId = user.optString("id", "");
            String username = user.optString("username", "");
            String email = user.optString("email", "");
            boolean isSuperuser = user.optBoolean("is_superuser", false);
            String profileImageUrl = user.optString("profile_image", null);

            Log.d(TAG, "User details - Username: " + username + ", IsSuperuser: " + isSuperuser);

            // Save user data
            SharedPrefManager.getInstance(this)
                    .setToken(token)
                    .setUserId(userId)
                    .setUsername(username)
                    .setEmail(email)
                    .setIsSuperuser(isSuperuser);

            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                SharedPrefManager.getInstance(this).setProfileImageUrl(profileImageUrl);
            }

            // Show success message
            Toast.makeText(this, "Welcome " + username + "!", Toast.LENGTH_SHORT).show();

            // Create intent based on user type
            Intent intent;
            if (isSuperuser) {
                Log.d(TAG, "Navigating to admin dashboard");
                intent = new Intent(this, Dashboard.class);
            } else {
                Log.d(TAG, "Navigating to user dashboard");
                intent = new Intent(this, UserDashboard.class);
            }

            // Add flags to clear activity stack
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Log before starting activity
            Log.d(TAG, "Starting activity: " + intent.getComponent().getClassName());
            
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error handling login response", e);
            Log.e(TAG, "Response was: " + (response != null ? response.toString() : "null"));
            showError("Error processing login response");
        }
    }

    private void handleLoginError(com.android.volley.VolleyError error) {
        Log.e(TAG, "Login error: " + error.toString());
        String errorMessage = "Login failed: ";

        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                Log.e(TAG, "Error body: " + responseBody);
                JSONObject errorJson = new JSONObject(responseBody);

                if (errorJson.has("detail")) {
                    errorMessage += errorJson.getString("detail");
                } else if (errorJson.has("message")) {
                    errorMessage += errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    errorMessage += errorJson.getString("error");
                } else {
                    errorMessage += "Invalid credentials";
                }
            } catch (Exception e) {
                errorMessage += "Server error: " + error.networkResponse.statusCode;
                Log.e(TAG, "Error parsing error response", e);
            }
        } else {
            errorMessage += "Network error. Please check your connection.";
        }

        showError(errorMessage);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logResponseData(JSONObject response) {
        try {
            Log.d(TAG, "Full response: " + response.toString(2));
        } catch (JSONException e) {
            Log.e(TAG, "Error logging response", e);
        }
    }

    private boolean validateResponse(JSONObject response) {
        if (!response.has("user")) {
            Log.e(TAG, "Response missing user object");
            return false;
        }

        try {
            JSONObject user = response.getJSONObject("user");
            if (!user.has("username") || !user.has("email")) {
                Log.e(TAG, "User object missing required fields");
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error validating response", e);
            return false;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}