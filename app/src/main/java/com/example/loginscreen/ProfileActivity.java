package com.example.loginscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.bumptech.glide.Glide;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import android.widget.PopupMenu;

import com.android.volley.DefaultRetryPolicy;

import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_REQUEST = 2;
    private ImageView profileImage;
    private TextView userNameText, userEmailText;
    private TextInputEditText usernameInput, emailInput;
    private TextView totalTasksCount, completedTasksCount;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private TextInputEditText currentPasswordInput, newPasswordInput;
    private TextView inProgressTasksCount, yetToStartTasksCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_profile);

            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Profile");
            }

            // Initialize RequestQueue first
            requestQueue = Volley.newRequestQueue(this);

            // Initialize views
            initializeViews();

            SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
            String token = sharedPrefManager.getToken();

            Log.d(TAG, "=== Profile Activity Start ===");
            Log.d(TAG, "Token exists: " + (token != null && !token.isEmpty()));
            Log.d(TAG, "Is logged in: " + sharedPrefManager.isLoggedIn());
            Log.d(TAG, "Token value: " + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null"));

            if (!sharedPrefManager.isLoggedIn() || token == null || token.isEmpty()) {
                Log.e(TAG, "User not logged in or token missing");
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(this, Login.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                finish();
                return;
            }

            setupListeners();
            loadUserProfile();
            loadTaskStatistics();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        try {
            // Initialize progressBar first
            progressBar = findViewById(R.id.progressBar);
            if (progressBar == null) {
                Log.e(TAG, "ProgressBar not found in layout");
                return;
            }

            profileImage = findViewById(R.id.profileImage);
            userNameText = findViewById(R.id.userNameText);
            userEmailText = findViewById(R.id.userEmailText);
            usernameInput = findViewById(R.id.usernameInput);
            emailInput = findViewById(R.id.emailInput);
            currentPasswordInput = findViewById(R.id.currentPasswordInput);
            newPasswordInput = findViewById(R.id.newPasswordInput);

            // Initialize task count views with null checks
            totalTasksCount = findViewById(R.id.totalTasksCount);
            completedTasksCount = findViewById(R.id.completedTasksCount);
            inProgressTasksCount = findViewById(R.id.inProgressTasksCount);
            yetToStartTasksCount = findViewById(R.id.yetToStartTasksCount);

            // Verify critical views are present
            if (profileImage == null || userNameText == null || userEmailText == null) {
                throw new IllegalStateException("Required views not found in layout");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;
        }
    }

    private void setupListeners() {
        findViewById(R.id.updateProfileButton).setOnClickListener(v -> updateProfile());

        // Add click listener for edit icon
        findViewById(R.id.editProfilePhoto).setOnClickListener(v -> {
            showProfilePhotoOptions(v);
        });

        findViewById(R.id.changePasswordButton).setOnClickListener(v -> changePassword());
        findViewById(R.id.forgotPasswordText).setOnClickListener(v -> showForgotPasswordDialog());
    }

    private boolean checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.CAMERA
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                // Show a dialog explaining why we need the permission
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("This permission is required to select a profile picture. " +
                                "Please grant the permission in Settings.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            // Open app settings
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    uploadProfileImage(imageUri);
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    if (photo != null) {
                        // Convert bitmap to Uri
                        Uri imageUri = getImageUri(photo);
                        uploadProfileImage(imageUri);
                    }
                }
            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void uploadProfileImage(Uri imageUri) {
        showLoading();
        String url = Constants.UPDATE_PROFILE_IMAGE_URL;

        try {
            // Compress and convert image to base64
            String base64Image = compressAndConvertImage(imageUri);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("image", base64Image);

            Log.d(TAG, "Uploading image to: " + url);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        hideLoading();
                        try {
                            Log.d(TAG, "Image upload response: " + response.toString());
                            String imageUrl = response.getString("image_url");

                            // Save image URL to SharedPreferences
                            SharedPrefManager.getInstance(this)
                                    .setProfileImageUrl(imageUrl);

                            // Load image using Glide
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(profileImage);

                            Toast.makeText(this, Constants.SUCCESS_IMAGE_UPLOAD,
                                    Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing image response: " + e.getMessage());
                            showError("Error updating profile picture");
                        }
                    },
                    error -> {
                        hideLoading();
                        Log.e(TAG, "Error uploading image: " + error.toString());
                        if (error.networkResponse != null) {
                            String errorResponse = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            Log.e(TAG, "Error response: " + errorResponse);
                        }
                        handleError(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    String token = SharedPrefManager.getInstance(ProfileActivity.this).getToken();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.REQUEST_TIMEOUT,
                    Constants.MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (Exception e) {
            hideLoading();
            Log.e(TAG, "Error preparing image: " + e.getMessage(), e);
            showError("Error preparing image: " + e.getMessage());
        }
    }

    private String compressAndConvertImage(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

        if (originalBitmap == null) {
            throw new IOException("Failed to decode image");
        }

        // Calculate new dimensions while maintaining aspect ratio
        int maxDimension = 800; // Max width or height
        float scale = Math.min(
                (float) maxDimension / originalBitmap.getWidth(),
                (float) maxDimension / originalBitmap.getHeight()
        );

        int newWidth = Math.round(originalBitmap.getWidth() * scale);
        int newHeight = Math.round(originalBitmap.getHeight() * scale);

        // Resize the bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap, newWidth, newHeight, true);

        // Convert to base64
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Clean up
        if (!originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (!resizedBitmap.isRecycled()) {
            resizedBitmap.recycle();
        }
        byteArrayOutputStream.close();
        inputStream.close();

        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void loadUserProfile() {
        showLoading();
        String url = Constants.PROFILE_URL;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                this::handleProfileResponse,
                error -> {
                    Log.e(TAG, "Error loading profile: " + error.toString());
                    // Load from cache first
                    loadUserProfileFromCache();

                    // Only show network error if we don't have cached data
                    SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
                    String cachedUsername = sharedPrefManager.getUsername();
                    if (cachedUsername == null || cachedUsername.isEmpty()) {
                        handleError(error);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        // Add retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                Constants.REQUEST_TIMEOUT,
                Constants.MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void handleProfileResponse(JSONObject response) {
        try {
            Log.d(TAG, "=== Handling Profile Response ===");
            Log.d(TAG, "Raw Response: " + response.toString());

            // Extract user data
            String username = response.getString("username");
            String email = response.getString("email");

            Log.d(TAG, "Username from response: " + username);
            Log.d(TAG, "Email from response: " + email);

            // Update UI elements
            runOnUiThread(() -> {
                try {
                    // Update username displays
                    if (userNameText != null) {
                        userNameText.setText(username);
                        Log.d(TAG, "Set userNameText to: " + username);
                    } else {
                        Log.e(TAG, "userNameText is null");
                    }

                    if (usernameInput != null) {
                        usernameInput.setText(username);
                        Log.d(TAG, "Set usernameInput to: " + username);
                    } else {
                        Log.e(TAG, "usernameInput is null");
                    }

                    // Update email displays
                    if (userEmailText != null) {
                        userEmailText.setText(email);
                        Log.d(TAG, "Set userEmailText to: " + email);
                    } else {
                        Log.e(TAG, "userEmailText is null");
                    }

                    if (emailInput != null) {
                        emailInput.setText(email);
                        Log.d(TAG, "Set emailInput to: " + email);
                    } else {
                        Log.e(TAG, "emailInput is null");
                    }

                    hideLoading();
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI: " + e.getMessage());
                    showError("Error displaying profile data");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing profile data: " + e.getMessage());
            Log.e(TAG, "Response that caused error: " + response.toString());
            showError("Error loading profile data");
        }
    }

    private void handleProfileError(com.android.volley.VolleyError error) {
        hideLoading();
        Log.e(TAG, "=== Profile Error Details ===");
        if (error.networkResponse != null) {
            Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
            String responseData = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            Log.e(TAG, "Error response: " + responseData);
        }
        handleError(error);
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Use Glide to load and cache the image
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImage);
        }
    }

    private void updateProfile() {
        String newUsername = usernameInput.getText().toString().trim();
        String currentUsername = SharedPrefManager.getInstance(this).getUsername();

        // Validate username
        if (newUsername.isEmpty()) {
            showError("Username cannot be empty");
            return;
        }

        // Check if username actually changed
        if (newUsername.equals(currentUsername)) {
            showError("No changes made to username");
            return;
        }

        showLoading();
        String url = Constants.BASE_URL + "api/profile/update";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", newUsername);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonBody,
                    response -> {
                        hideLoading();
                        // Update SharedPreferences with new username
                        SharedPrefManager.getInstance(this).setUsername(newUsername);
                        // Update UI
                        userNameText.setText(newUsername);
                        Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        hideLoading();
                        Log.e(TAG, "Error updating username: " + error.toString());
                        handleError(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return getAuthHeaders();
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000, // 30 seconds timeout
                    2,     // 2 retries
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating update request", e);
            showError("Error updating profile");
        }
    }

    private void loadTaskStatistics() {
        showLoading();
        String url = Constants.TASK_STATS_URL;
        Log.d(TAG, "Fetching task statistics from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    hideLoading();
                    try {
                        Log.d(TAG, "Task statistics response: " + response.toString());

                        // Parse task counts from response
                        int totalTasks = response.getInt("total_tasks");
                        int completedTasks = response.getInt("completed");
                        int inProgressTasks = response.getInt("in_progress");
                        int yetToStartTasks = response.getInt("yet_to_start");
                        int onHoldTasks = response.getInt("on_hold");

                        // Update UI with task counts - Using runOnUiThread for safety
                        runOnUiThread(() -> {
                            try {
                                // Update each TextView with proper null checking
                                if (totalTasksCount != null) {
                                    totalTasksCount.setText(String.valueOf(totalTasks));
                                    Log.d(TAG, "Set totalTasksCount: " + totalTasks);
                                }
                                if (completedTasksCount != null) {
                                    completedTasksCount.setText(String.valueOf(completedTasks));
                                    Log.d(TAG, "Set completedTasksCount: " + completedTasks);
                                }
                                if (inProgressTasksCount != null) {
                                    inProgressTasksCount.setText(String.valueOf(inProgressTasks));
                                    Log.d(TAG, "Set inProgressTasksCount: " + inProgressTasks);
                                }
                                if (yetToStartTasksCount != null) {
                                    yetToStartTasksCount.setText(String.valueOf(yetToStartTasks));
                                    Log.d(TAG, "Set yetToStartTasksCount: " + yetToStartTasks);
                                }

                                // Update progress bar if it exists
                                if (totalTasks > 0) {
                                    int percentage = (completedTasks * 100) / totalTasks;
                                    ProgressBar taskProgressBar = findViewById(R.id.taskProgressBar);
                                    TextView percentageText = findViewById(R.id.completionPercentage);

                                    if (taskProgressBar != null) {
                                        animateProgressBar(taskProgressBar, percentage);
                                        Log.d(TAG, "Updated progress bar to: " + percentage + "%");
                                    }
                                    if (percentageText != null) {
                                        percentageText.setText(percentage + "% Complete");
                                        Log.d(TAG, "Updated percentage text to: " + percentage + "%");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with task statistics: " + e.getMessage());
                                showError("Error displaying task statistics");
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing task statistics: " + e.getMessage());
                        showError("Error loading task statistics");
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error fetching task statistics: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Error response: " + new String(error.networkResponse.data, StandardCharsets.UTF_8));
                    }
                    handleError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                Constants.REQUEST_TIMEOUT,
                Constants.MAX_RETRIES,
                Constants.BACKOFF_MULTIPLIER
        ));

        requestQueue.add(request);
    }

    private void animateProgressBar(ProgressBar progressBar, int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetProgress);
        animator.setDuration(1000); // 1 second duration
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBar.setProgress(progress);
        });
        animator.start();
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String token = SharedPrefManager.getInstance(this).getToken();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        hideLoading();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleError(com.android.volley.VolleyError error) {
        String message = Constants.ERROR_NETWORK;  // Default error message

        if (error.networkResponse != null) {
            try {
                String responseData = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject errorJson = new JSONObject(responseData);

                if (errorJson.has("detail")) {
                    message = errorJson.getString("detail");
                } else if (errorJson.has("message")) {
                    message = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    message = errorJson.getString("error");
                }

                if (error.networkResponse.statusCode == 404) {
                    // Don't show "Profile not found" message, just load the UI with cached data
                    loadUserProfileFromCache();
                    return;  // Return early to prevent showing error message
                } else if (error.networkResponse.statusCode >= 500) {
                    message = Constants.ERROR_SERVER;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response: " + e.getMessage());
            }
        } else if (error instanceof com.android.volley.NoConnectionError) {
            message = Constants.ERROR_NETWORK;
        } else if (error instanceof com.android.volley.TimeoutError) {
            message = Constants.ERROR_TIMEOUT;
        } else if (error instanceof com.android.volley.AuthFailureError) {
            message = Constants.ERROR_AUTH;
            SharedPrefManager.getInstance(this).logout();
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        showError(message);
    }

    private void showProfilePhotoOptions(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.profile_photo_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_select_photo) {
                if (checkPermission()) {
                    openImagePicker();
                } else {
                    requestPermission();
                }
                return true;
            } else if (itemId == R.id.action_change_photo) {
                if (checkPermission()) {
                    openCamera();
                } else {
                    requestPermission();
                }
                return true;
            } else if (itemId == R.id.action_remove_photo) {
                removeProfilePhoto();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void removeProfilePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Are you sure you want to remove your profile photo?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    showLoading();
                    String url = Constants.REMOVE_PROFILE_IMAGE_URL;

                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            null,
                            response -> {
                                hideLoading();
                                // Clear the cached image URL
                                SharedPrefManager.getInstance(this).setProfileImageUrl("");
                                // Reset the ImageView to default
                                profileImage.setImageResource(R.drawable.default_profile);
                                // Clear Glide cache for this image
                                Glide.with(this).clear(profileImage);
                                Toast.makeText(this, Constants.SUCCESS_IMAGE_REMOVE, Toast.LENGTH_SHORT).show();
                            },
                            error -> {
                                hideLoading();
                                handleError(error);
                            }
                    ) {
                        @Override
                        public Map<String, String> getHeaders() {
                            return getAuthHeaders();
                        }
                    };

                    requestQueue.add(request);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changePassword() {
        String currentPassword = currentPasswordInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();

        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            showError("All fields are required");
            return;
        }

        if (newPassword.length() < Constants.PASSWORD_MIN_LENGTH) {
            showError("New password must be at least " + Constants.PASSWORD_MIN_LENGTH + " characters");
            return;
        }

        showLoading();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("current_password", currentPassword);
            jsonBody.put("new_password", newPassword);

            Log.d(TAG, "Sending password update request to: " + Constants.UPDATE_PASSWORD_URL);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    Constants.UPDATE_PASSWORD_URL,
                    jsonBody,
                    response -> {
                        hideLoading();
                        try {
                            if (response.has("new_token")) {
                                String newToken = response.getString("new_token");
                                SharedPrefManager.getInstance(this).setToken(newToken);
                                Log.d(TAG, "New token received and saved");
                            }

                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            currentPasswordInput.setText("");
                            newPasswordInput.setText("");

                        } catch (JSONException e) {
                            Log.e(TAG, "Error processing response: " + e.getMessage());
                            showError("Error updating password");
                        }
                    },
                    error -> {
                        hideLoading();
                        Log.e(TAG, "Error updating password: " + error.toString());
                        if (error.networkResponse != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                                Log.e(TAG, "Error response: " + errorResponse);
                                JSONObject errorJson = new JSONObject(errorResponse);
                                if (errorJson.has("error")) {
                                    showError(errorJson.getString("error"));
                                    return;
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing error response: " + e.getMessage());
                            }
                        }
                        showError("Failed to update password. Please try again.");
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    String token = SharedPrefManager.getInstance(ProfileActivity.this).getToken();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.REQUEST_TIMEOUT,
                    Constants.MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating request: " + e.getMessage());
            showError("Error preparing request");
        }
    }

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Do you want to reset your password? We'll send instructions to your email.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    sendPasswordResetEmail();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void sendPasswordResetEmail() {
        showLoading();
        String url = Constants.BASE_URL + "api/auth/forgot-password";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", userEmailText.getText().toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        hideLoading();
                        Toast.makeText(this,
                                "Password reset instructions sent to your email",
                                Toast.LENGTH_LONG).show();
                    },
                    error -> {
                        hideLoading();
                        handleError(error);
                    }
            );

            requestQueue.add(request);

        } catch (JSONException e) {
            hideLoading();
            showError("Error preparing request");
        }
    }

    // Add this new method to load profile from cache
    private void loadUserProfileFromCache() {
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
        String cachedUsername = sharedPrefManager.getUsername();
        String cachedEmail = sharedPrefManager.getEmail();
        String cachedImageUrl = sharedPrefManager.getProfileImageUrl();

        // Set cached data
        if (!cachedUsername.isEmpty()) {
            userNameText.setText(cachedUsername);
            usernameInput.setText(cachedUsername);
        }
        if (!cachedEmail.isEmpty()) {
            userEmailText.setText(cachedEmail);
            emailInput.setText(cachedEmail);
        }
        if (cachedImageUrl != null && !cachedImageUrl.isEmpty()) {
            loadProfileImage(cachedImageUrl);
        }

        hideLoading();
    }
}