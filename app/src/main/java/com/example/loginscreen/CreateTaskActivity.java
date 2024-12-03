package com.example.loginscreen;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity {
    private static final String TAG = "CreateTaskActivity";

    private EditText taskNameEditText;
    private EditText descriptionEditText;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private EditText deadlineEditText;
    private Button saveTaskButton;
    private RequestQueue requestQueue;
    private LinearLayout assignTaskLayout;
    private EditText assignToEditText;
    private Button verifyUserButton;
    private String verifiedUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Initialize back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // This will go back to the previous activity
        });

        requestQueue = Volley.newRequestQueue(this);
        initializeViews();
        setupSpinners();
        setupDatePicker();
        setupSaveButton();

        // Initialize assign task views
        assignTaskLayout = findViewById(R.id.assignTaskLayout);
        assignToEditText = findViewById(R.id.assignToEditText);
        verifyUserButton = findViewById(R.id.verifyUserButton);

        // Show assign task layout only for admin
        if (SharedPrefManager.getInstance(this).getIsSuperuser()) {
            assignTaskLayout.setVisibility(View.VISIBLE);
        }

        // Setup verify user button click
        verifyUserButton.setOnClickListener(v -> verifyUser());
    }

    private void initializeViews() {
        taskNameEditText = findViewById(R.id.taskNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        statusSpinner = findViewById(R.id.statusSpinner);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        deadlineEditText = findViewById(R.id.deadlineEditText);
        saveTaskButton = findViewById(R.id.saveTaskButton);
    }

    private void setupSpinners() {
        // Setup Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Yet to Start", "In Progress", "Completed", "On Hold")
        );
        statusSpinner.setAdapter(statusAdapter);

        // Setup Priority Spinner
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Low", "Medium", "High")
        );
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void setupDatePicker() {
        deadlineEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
                        deadlineEditText.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupSaveButton() {
        saveTaskButton.setOnClickListener(v -> createTask());
    }

    private void createTask() {
        String taskName = taskNameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String status = statusSpinner.getSelectedItem().toString();
        String priority = prioritySpinner.getSelectedItem().toString();
        String deadline = deadlineEditText.getText().toString().trim();

        if (taskName.isEmpty()) {
            taskNameEditText.setError("Task name is required");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("task_name", taskName);
            requestBody.put("description", description);
            requestBody.put("status", status);
            requestBody.put("priority", priority);
            requestBody.put("deadline", deadline);
            
            // Add assign_to field if user is verified
            if (verifiedUsername != null) {
                requestBody.put("assign_to", verifiedUsername);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String url = Constants.CREATE_TASK_URL;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();

                    // Convert status to match the format expected by Dashboard
                    String dashboardStatus = convertStatusToDashboardFormat(status);
                    
                    // Notify Dashboard with the correct status format
                    notifyDashboard(dashboardStatus);

                    // Start TaskDetailsActivity
                    Intent taskDetailsIntent = new Intent(this, TaskDetailsActivity.class);
                    taskDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(taskDetailsIntent);
                    finish();
                },
                error -> {
                    Log.e(TAG, "Error creating task", error);
                    String errorMsg = "Error creating task";
                    if (error.networkResponse != null) {
                        errorMsg += " (Code: " + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .getString("jwt_token", "");
                if (!token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    // Add this new method to convert status formats
    private String convertStatusToDashboardFormat(String status) {
        switch (status) {
            case "Yet to Start":
                return "NOT_STARTED";
            case "In Progress":
                return "IN_PROGRESS";
            case "Completed":
                return "COMPLETED";
            case "On Hold":
                return "ON_HOLD";
            default:
                return status;
        }
    }

    private void notifyDashboard(String status) {
        Log.d(TAG, "Notifying dashboard with status: " + status);
        Intent intent = new Intent(Dashboard.ACTION_UPDATE_DASHBOARD);
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void verifyUser() {
        String username = assignToEditText.getText().toString().trim();
        if (username.isEmpty()) {
            assignToEditText.setError("Please enter a username");
            return;
        }

        showLoading();
        Log.d(TAG, "Verifying username: " + username);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    Constants.VERIFY_USERNAME_URL,
                    jsonBody,
                    response -> {
                        hideLoading();
                        Log.d(TAG, "Verify response: " + response.toString());
                        try {
                            boolean exists = response.getBoolean("exists");
                            String message = response.optString("message", "");
                            
                            if (exists) {
                                verifiedUsername = username;
                                assignToEditText.setEnabled(false);
                                verifyUserButton.setText("Verified ✓");
                                verifyUserButton.setEnabled(false);
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            } else {
                                assignToEditText.setError(message);
                                verifiedUsername = null;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            showError("Error verifying user");
                        }
                    },
                    error -> {
                        hideLoading();
                        Log.e(TAG, "Error verifying user", error);
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

        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating request: " + e.getMessage());
            showError("Error creating request");
        }
    }

    private void showLoading() {
        // If you have a ProgressBar in your layout
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        // Disable the buttons while loading
        if (saveTaskButton != null) saveTaskButton.setEnabled(false);
        if (verifyUserButton != null) verifyUserButton.setEnabled(false);
    }

    private void hideLoading() {
        // Hide ProgressBar
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        // Re-enable the buttons
        if (saveTaskButton != null) saveTaskButton.setEnabled(true);
        if (verifyUserButton != null) verifyUserButton.setEnabled(true);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(com.android.volley.VolleyError error) {
        hideLoading();
        String message = "An error occurred";
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                JSONObject data = new JSONObject(responseBody);
                if (data.has("error")) {
                    message = data.getString("error");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response: " + e.getMessage());
            }
        }
        showError(message);
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = SharedPrefManager.getInstance(this).getToken();
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        headers.put("Content-Type", "application/json");
        return headers;
    }
}