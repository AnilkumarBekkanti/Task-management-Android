package com.example.loginscreen;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboard extends AppCompatActivity {
    private static final String TAG = "UserDashboard";
    private TextView tvTotalTasks, tvInProgress, tvCompleted, tvYetToStart, tvOnHold;
    private RecyclerView taskRecyclerView;
    private RequestQueue requestQueue;
    private TaskAdapter taskAdapter;
    private Button btnCreateTask;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);
        
        // Setup toolbar
        setupToolbar();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        fetchTaskStats();
        fetchUserTasks();
    }

    private void initializeViews() {
        requestQueue = Volley.newRequestQueue(this);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvInProgress = findViewById(R.id.tvInProgress);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvYetToStart = findViewById(R.id.tvYetToStart);
        tvOnHold = findViewById(R.id.tvOnHold);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
    }

    private void setupRecyclerView() {
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, new ArrayList<>(), false);
        taskRecyclerView.setAdapter(taskAdapter);
    }

    private void setupClickListeners() {
        btnCreateTask.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboard.this, CreateTaskActivity.class);
            startActivity(intent);
        });
    }

    private void fetchTaskStats() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                Constants.TASK_STATS_URL,
                null,
                response -> {
                    try {
                        tvTotalTasks.setText("Total Tasks: " + response.getInt("total_tasks"));
                        tvYetToStart.setText("Yet to Start: " + response.getInt("yet_to_start"));
                        tvInProgress.setText("In Progress: " + response.getInt("in_progress"));
                        tvCompleted.setText("Completed: " + response.getInt("completed"));
                        tvOnHold.setText("On Hold: " + response.getInt("on_hold"));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing stats response", e);
                    }
                },
                error -> handleError(error)
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

    private void fetchUserTasks() {
        try {
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    Constants.USER_TASKS_URL,
                    null,
                    response -> {
                        try {
                            if (!response.has("tasks")) {
                                Log.e(TAG, "Response missing tasks array");
                                return;
                            }
                            
                            JSONArray tasksArray = response.getJSONArray("tasks");
                            List<Task> tasks = new ArrayList<>();
                            
                            for (int i = 0; i < tasksArray.length(); i++) {
                                JSONObject taskObj = tasksArray.getJSONObject(i);
                                try {
                                    Task task = new Task(
                                            taskObj.getInt("id"),
                                            taskObj.getString("task_name"),
                                            taskObj.optString("description", ""),
                                            taskObj.optString("status", "Yet to Start"),
                                            taskObj.optString("priority", "Low"),
                                            taskObj.optString("deadline", "")
                                    );
                                    tasks.add(task);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing task: " + e.getMessage());
                                }
                            }
                            
                            if (taskAdapter != null) {
                                taskAdapter.updateTasks(tasks);
                                taskRecyclerView.setVisibility(View.VISIBLE);
                            } else {
                                Log.e(TAG, "TaskAdapter is null");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing tasks response", e);
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error fetching tasks", error);
                        handleError(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return getAuthHeaders();
                }
            };
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchUserTasks", e);
        }
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

    private void handleError(com.android.volley.VolleyError error) {
        String message = "An error occurred";
        if (error.networkResponse != null) {
            if (error.networkResponse.statusCode == 401) {
                // Token expired or invalid, redirect to login
                SharedPrefManager.getInstance(this).logout();
                startActivity(new Intent(this, Login.class));
                finish();
                return;
            }
            message = "Error: " + error.networkResponse.statusCode;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            // Handle home click
            Intent homeIntent = new Intent(this, Home.class);
            startActivity(homeIntent);
            return true;
        } else if (id == R.id.action_view_tasks) {
            // Handle view tasks click
            Intent tasksIntent = new Intent(this, TaskDetailsActivity.class);
            startActivity(tasksIntent);
            return true;
        } else if (id == R.id.action_profile) {
            // Handle profile click
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPrefManager.getInstance(this).logout();
                    Intent intent = new Intent(this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard");
        }
    }
}
