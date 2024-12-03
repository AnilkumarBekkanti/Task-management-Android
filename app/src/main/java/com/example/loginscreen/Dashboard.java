package com.example.loginscreen;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard extends AppCompatActivity {
    private static final String TAG = "Dashboard";
    public static final String ACTION_UPDATE_DASHBOARD = "com.example.loginscreen.UPDATE_DASHBOARD";
    private TextView tvTotalTasks;
    private TextView tvInProgress;
    private TextView tvCompleted;
    private TextView tvOnHold;
    private TextView tvYetToStart;
    private Button btnCreateTask;
    private RecyclerView taskRecyclerView;
    private RequestQueue requestQueue;
    private BroadcastReceiver updateReceiver;
    private TaskAdapter taskAdapter;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Set the title to Dashboard
        setTitle("Dashboard");
        
        Log.d(TAG, "Dashboard onCreate started");

        try {
            // Initialize RequestQueue
            requestQueue = Volley.newRequestQueue(this);

            // Check if user is admin
            isAdmin = SharedPrefManager.getInstance(this).getIsSuperuser();

            // Initialize views
            initializeViews();
            setupClickListeners();
            setupBroadcastReceiver();
            fetchTaskStats();

            // Show admin button only for superusers
            Button adminButton = findViewById(R.id.adminButton);
            if (isAdmin) {
                adminButton.setVisibility(View.VISIBLE);
                adminButton.setOnClickListener(v -> {
                    fetchAndShowAllUsersTasks();
                });
            } else {
                adminButton.setVisibility(View.GONE);
            }

            Log.d(TAG, "Dashboard initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in Dashboard onCreate: ", e);
        }
    }

    private void initializeViews() {
        try {
            // Initialize TextViews for counts
            tvTotalTasks = findViewById(R.id.tvTotalTasks);
            tvInProgress = findViewById(R.id.tvInProgress);
            tvCompleted = findViewById(R.id.tvCompleted);
            tvOnHold = findViewById(R.id.tvOnHold);
            tvYetToStart = findViewById(R.id.tvYetToStart);

            // Initialize create task button
            btnCreateTask = findViewById(R.id.btnCreateTask);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;  // Rethrow to be caught by outer try-catch
        }
    }

    private void setupClickListeners() {
        btnCreateTask.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, CreateTaskActivity.class);
            startActivity(intent);
        });

        // Modify this click listener for total tasks
        tvTotalTasks.setOnClickListener(v -> {
            fetchAndShowAllTasks();  // Add this new method
        });
    }

    private void setupBroadcastReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received broadcast: " + intent.getAction());
                if (ACTION_UPDATE_DASHBOARD.equals(intent.getAction())) {
                    String updatedStatus = intent.getStringExtra("status");
                    if (updatedStatus != null) {
                        updateTaskCount(updatedStatus);
                    } else {
                        fetchTaskStats();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_UPDATE_DASHBOARD);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);
        Log.d(TAG, "Broadcast receiver registered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(updateReceiver);
        }
    }

    private void fetchTaskStats() {
        Log.d(TAG, "Starting fetchTaskStats");
        String url;

        if (isAdmin) {
            url = Constants.ADMIN_STATS_URL;
            Log.d(TAG, "Using admin stats URL: " + url);
        } else {
            url = Constants.TASK_STATS_URL;
            Log.d(TAG, "Using regular stats URL: " + url);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Received task counts response: " + response.toString());
                    try {
                        // Update total tasks
                        if (!response.has("total_tasks")) {
                            Log.e(TAG, "Response missing total_tasks field");
                            return;
                        }
                        
                        int totalTasks = response.getInt("total_tasks");
                        tvTotalTasks.setText("Total Tasks: " + totalTasks);

                        // Update status counts with labels
                        if (response.has("yet_to_start")) {
                            tvYetToStart.setText(response.getInt("yet_to_start") + "");
                        }
                        if (response.has("in_progress")) {
                            tvInProgress.setText(response.getInt("in_progress") + "");
                        }
                        if (response.has("completed")) {
                            tvCompleted.setText(response.getInt("completed") + "");
                        }
                        if (response.has("on_hold")) {
                            tvOnHold.setText(response.getInt("on_hold") + "");
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        Log.e(TAG, "Response that caused error: " + response.toString());
                        Toast.makeText(this, "Error loading task counts", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching counts", error);
                    if (error.networkResponse != null) {
                        String responseData = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        Log.e(TAG, "Error Response: " + responseData);
                        Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                    }
                    handleError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(Dashboard.this).getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    Log.d(TAG, "Added token to request headers");
                } else {
                    Log.e(TAG, "No token available for request");
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                Constants.REQUEST_TIMEOUT,
                Constants.MAX_RETRIES,
                Constants.BACKOFF_MULTIPLIER
        ));

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTaskStats();
        // Refresh tasks if they're being displayed
        if (taskRecyclerView != null && taskRecyclerView.getVisibility() == View.VISIBLE) {
            if (isAdmin) {
                fetchAndShowAllUsersTasks();
            } else {
                fetchAndShowAllTasks();
            }
        }
    }

    private void updateTaskCount(String status) {
        Log.d(TAG, "Updating count for status: " + status);
        String url = Constants.BASE_URL + "api/tasks/stats/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Received updated counts: " + response.toString());
                    try {
                        // Update total tasks
                        int totalTasks = response.getInt("total_tasks");
                        tvTotalTasks.setText("Total Tasks: " + totalTasks);

                        // Update individual status counts
                        switch(status) {
                            case "NOT_STARTED":
                                tvYetToStart.setText("Yet to Start: " + response.getInt("yet_to_start"));
                                break;
                            case "IN_PROGRESS":
                                tvInProgress.setText("In Progress: " + response.getInt("in_progress"));
                                break;
                            case "COMPLETED":
                                tvCompleted.setText("Completed: " + response.getInt("completed"));
                                break;
                            case "ON_HOLD":
                                tvOnHold.setText("On Hold: " + response.getInt("on_hold"));
                                break;
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error updating counts", error);
                    Toast.makeText(this, "Error updating task counts", Toast.LENGTH_SHORT).show();
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
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void fetchAndShowAllUsersTasks() {
        Log.d(TAG, "Starting fetchAndShowAllUsersTasks");

        if (!SharedPrefManager.getInstance(this).getIsSuperuser()) {
            Log.e(TAG, "Non-admin user attempting to access admin tasks");
            Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fix the URL - remove any trailing slashes
        String url = Constants.ADMIN_TASKS_URL;
        Log.d(TAG, "Requesting URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Received response: " + response.toString());
                    try {
                        if (!response.has("tasks")) {
                            Log.e(TAG, "Response missing 'tasks' array");
                            Toast.makeText(this, "Invalid response format", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray tasksArray = response.getJSONArray("tasks");
                        List<Task> tasks = new ArrayList<>();
                        Log.d(TAG, "Found " + tasksArray.length() + " tasks");

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObj = tasksArray.getJSONObject(i);
                            Log.d(TAG, "Processing task " + i + ": " + taskObj.toString());

                            Task task = new Task(
                                    taskObj.getInt("id"),
                                    taskObj.getString("task_name"),
                                    taskObj.optString("description", ""),
                                    taskObj.optString("priority", "Medium"),
                                    taskObj.optString("status", "Yet to Start"),
                                    taskObj.optString("deadline", ""),
                                    taskObj.optString("username", "Unknown")
                            );
                            tasks.add(task);
                        }

                        // Start AdminTasksActivity with the tasks
                        Intent intent = new Intent(Dashboard.this, AdminTasksActivity.class);
                        intent.putExtra("tasks", new ArrayList<>(tasks));
                        startActivity(intent);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        Log.e(TAG, "Response was: " + response.toString());
                        Toast.makeText(this, "Error parsing admin tasks", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Network error fetching admin tasks");
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Error body: " + responseBody);
                            JSONObject errorObj = new JSONObject(responseBody);
                            String errorMessage = errorObj.optString("detail", "Error loading admin tasks");
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error response", e);
                            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "No network response", error);
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(Dashboard.this).getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    Log.d(TAG, "Added token to request headers");
                } else {
                    Log.e(TAG, "No token available for request");
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                Constants.REQUEST_TIMEOUT,
                Constants.MAX_RETRIES,
                Constants.BACKOFF_MULTIPLIER
        ));

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }
        requestQueue.add(request);
    }

    private void fetchAndShowAllTasks() {
        String url = Constants.BASE_URL + "api/tasks/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray tasksArray = response.getJSONArray("tasks");
                        List<Task> tasks = new ArrayList<>();

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObj = tasksArray.getJSONObject(i);
                            Task task = new Task(
                                    taskObj.getInt("id"),
                                    taskObj.getString("task_name"),
                                    taskObj.getString("description"),
                                    taskObj.getString("status"),
                                    taskObj.getString("priority"),
                                    taskObj.getString("deadline")
                            );
                            tasks.add(task);
                        }

                        showTasksInRecyclerView(tasks, false);  // false for regular view

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing tasks: " + e.getMessage());
                        Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching tasks", error);
                    Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void showTasksInRecyclerView(List<Task> tasks, boolean isAdminView) {
        // Initialize RecyclerView if not already initialized
        if (taskRecyclerView == null) {
            taskRecyclerView = findViewById(R.id.taskRecyclerView);
            taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        // Create and set adapter
        taskAdapter = new TaskAdapter(this, tasks, isAdminView);
        taskRecyclerView.setAdapter(taskAdapter);

        // Make RecyclerView visible
        taskRecyclerView.setVisibility(View.VISIBLE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Use if-else instead of switch for menu items
        int id = item.getItemId();

        if (id == R.id.action_home) {
            // Navigate to Home activity
            Intent homeIntent = new Intent(Dashboard.this, Home.class);
            startActivity(homeIntent);
            finish();
            return true;
        }
        else if (id == R.id.action_view_tasks) {
            // Navigate to TaskDetails activity
            Intent tasksIntent = new Intent(Dashboard.this, TaskDetailsActivity.class);
            startActivity(tasksIntent);
            return true;
        }
        else if (id == R.id.action_profile) {
            try {
                SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(this);
                String token = sharedPrefManager.getToken();

                Log.d(TAG, "=== Profile Navigation Debug ===");
                Log.d(TAG, "Token exists: " + (token != null && !token.isEmpty()));
                Log.d(TAG, "Is logged in: " + sharedPrefManager.isLoggedIn());
                Log.d(TAG, "Username: " + sharedPrefManager.getUsername());
                Log.d(TAG, "Token value: " + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null"));

                // First check if logged in and token exists
                if (!sharedPrefManager.isLoggedIn() || token == null || token.isEmpty()) {
                    Log.e(TAG, "User not logged in or token missing");
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                    // Clear session and redirect to login
                    sharedPrefManager.logout();
                    Intent loginIntent = new Intent(Dashboard.this, Login.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                    return true;
                }

                // If we get here, we should be good to go
                Log.d(TAG, "Starting ProfileActivity...");
                Intent profileIntent = new Intent(Dashboard.this, ProfileActivity.class);
                profileIntent.putExtra("token", token); // Pass token explicitly
                startActivity(profileIntent);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "=== Profile Navigation Error ===");
                Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
                Log.e(TAG, "Error message: " + e.getMessage());
                Log.e(TAG, "Stack trace: ", e);
                Toast.makeText(this, "Error accessing profile", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else if (id == R.id.action_logout) {
            // Show logout confirmation dialog
            showLogoutConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        Log.d(TAG, "User confirmed logout");
                        // Clear the session
                        SharedPrefManager.getInstance(this).logout();
                        Log.d(TAG, "Session cleared");

                        // Navigate to Login activity
                        Intent intent = new Intent(Dashboard.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        Log.d(TAG, "Redirected to login");
                    } catch (Exception e) {
                        Log.e(TAG, "Error during logout: " + e.getMessage());
                        Toast.makeText(this, "Error during logout", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void handleError(com.android.volley.VolleyError error) {
        String message = "An error occurred";
        if (error.networkResponse != null) {
            try {
                String responseData = new String(error.networkResponse.data, "UTF-8");
                Log.e(TAG, "Error Response: " + responseData);

                switch (error.networkResponse.statusCode) {
                    case 401:
                        message = "Unauthorized access. Please login again.";
                        // Handle unauthorized access - maybe redirect to login
                        SharedPrefManager.getInstance(this).logout();
                        Intent loginIntent = new Intent(Dashboard.this, Login.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(loginIntent);
                        finish();
                        break;
                    case 403:
                        message = "Permission denied. Admin access required.";
                        break;
                    case 404:
                        message = "Tasks not found. Please try again later.";
                        break;
                    case 500:
                        message = "Server error. Please try again later.";
                        break;
                    default:
                        message = "Error: " + error.networkResponse.statusCode;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response", e);
            }
        } else {
            message = "Network error. Please check your connection.";
        }
        Log.e(TAG, "Error loading tasks: " + message);
        showError(message);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}