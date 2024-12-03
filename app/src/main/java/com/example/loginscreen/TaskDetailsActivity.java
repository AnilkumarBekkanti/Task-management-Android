package com.example.loginscreen;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.example.loginscreen.helpers.NotificationHelper;
import android.Manifest;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TaskDetailsActivity extends AppCompatActivity {
    private static final String TAG = "TaskDetailsActivity";
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TextView totalTasksTextView;
    private TextView emptyStateTextView;
    private RequestQueue requestQueue;
    private FloatingActionButton fabAddTask;
    private ProgressBar progressBar;
    private BroadcastReceiver updateReceiver;
    private EditText searchEditText;
    private List<Task> filteredList;
    private ChipGroup priorityChipGroup;
    private ChipGroup statusChipGroup;
    private ChipGroup deadlineChipGroup;
    private String selectedPriority = "";
    private String selectedStatus = "";
    private String selectedDeadline = "";
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        isAdmin = SharedPrefManager.getInstance(this).getIsSuperuser();

        requestQueue = Volley.newRequestQueue(this);
        taskList = new ArrayList<>();
        filteredList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupBroadcastReceiver();
        setupSearchFunctionality();
        fetchUserTasks();
        checkDeadlinesAndNotify();
        requestNotificationPermission();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        totalTasksTextView = findViewById(R.id.totalTasksTextView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        fabAddTask = findViewById(R.id.fabAddTask);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        priorityChipGroup = findViewById(R.id.priorityChipGroup);
        statusChipGroup = findViewById(R.id.statusChipGroup);
        deadlineChipGroup = findViewById(R.id.deadlineChipGroup);

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(TaskDetailsActivity.this, Dashboard.class);
            startActivity(intent);
        });

        setupChipGroups();
    }

    private void setupChipGroups() {
        priorityChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedPriority = "";
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                selectedPriority = chip.getText().toString();
            }
            applyFilters();
        });

        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedStatus = "";
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                selectedStatus = chip.getText().toString();
            }
            applyFilters();
        });

        deadlineChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedDeadline = "";
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                selectedDeadline = chip.getText().toString();
            }
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, filteredList, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupBroadcastReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Dashboard.ACTION_UPDATE_DASHBOARD.equals(intent.getAction())) {
                    Log.d(TAG, "Received update broadcast in TaskDetails");
                    fetchUserTasks();
                }
            }
        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(updateReceiver, new IntentFilter(Dashboard.ACTION_UPDATE_DASHBOARD));
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTasks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchUserTasks() {
        String url = Constants.BASE_URL + "api/tasks/user/";
        Log.d(TAG, "Fetching tasks from URL: " + url);

        showLoading();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    hideLoading();
                    Log.d(TAG, "Response received: " + response.toString());
                    try {
                        JSONArray tasksArray = response.getJSONArray("tasks");
                        taskList.clear();
                        filteredList.clear();

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskJson = tasksArray.getJSONObject(i);
                            Task task = new Task(
                                    taskJson.getInt("id"),
                                    taskJson.getString("task_name"),
                                    taskJson.getString("description"),
                                    taskJson.getString("priority"),
                                    taskJson.getString("status"),
                                    taskJson.getString("deadline")
                            );
                            taskList.add(task);
                        }

                        filteredList.addAll(taskList);
                        taskAdapter.notifyDataSetChanged();
                        updateEmptyState();

                        if (response.has("counts")) {
                            JSONObject counts = response.getJSONObject("counts");
                            int total = counts.getInt("total");
                            totalTasksTextView.setText("Total Tasks: " + total);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON", e);
                        showError("Error parsing tasks: " + e.getMessage());
                    }
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
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.setText("No tasks found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.GONE);
        }
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(updateReceiver);
        }
    }

    private void updateTaskStatus(String newStatus) {
        // Your existing update code...

        // After successful update
        String dashboardStatus = convertStatusToDashboardFormat(newStatus);
        notifyDashboard(dashboardStatus);
    }

    private String convertStatusToDashboardFormat(String status) {
        switch (status) {
            case "Yet to Start":
                return "NOT_STARTED";
            case "In Progress":
                return "IN_PROGRESS";
            case "Completed":
                return "COMPLETED";
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

    private void filterTasks(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(taskList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Task task : taskList) {
                if (task.getTaskName().toLowerCase().contains(lowerCaseQuery) ||
                        task.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(task);
                }
            }
        }
        taskAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void applyFilters() {
        filteredList.clear();
        String searchQuery = searchEditText.getText().toString().toLowerCase();

        for (Task task : taskList) {
            boolean matchesPriority = selectedPriority.isEmpty() ||
                    task.getPriority().equalsIgnoreCase(selectedPriority);
            boolean matchesStatus = selectedStatus.isEmpty() ||
                    task.getStatus().equalsIgnoreCase(selectedStatus);
            boolean matchesDeadline = selectedDeadline.isEmpty() ||
                    matchesDeadlineFilter(task.getDeadline(), selectedDeadline);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    task.getTaskName().toLowerCase().contains(searchQuery) ||
                    task.getDescription().toLowerCase().contains(searchQuery);

            if (matchesPriority && matchesStatus && matchesDeadline && matchesSearch) {
                filteredList.add(task);
            }
        }

        taskAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesDeadlineFilter(String taskDeadline, String filter) {
        // Implement deadline filtering logic based on your date format
        // Example implementation:
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date deadline = sdf.parse(taskDeadline);
            Date now = new Date();
            Calendar cal = Calendar.getInstance();

            switch (filter) {
                case "Today":
                    return isSameDay(deadline, now);
                case "This Week":
                    cal.setTime(now);
                    cal.add(Calendar.DAY_OF_YEAR, 7);
                    return deadline.before(cal.getTime());
                case "This Month":
                    cal.setTime(now);
                    cal.add(Calendar.MONTH, 1);
                    return deadline.before(cal.getTime());
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
        }
        return false;
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String convertToApiFormat(String status) {
        // Ensure exact string matching with the API's expected values
        switch (status.trim()) {
            case "Yet to Start":
            case "Not Started":
            case "On Hold":
            case "yet to start":  // Add lowercase variant
            case "YET TO START":  // Add uppercase variant
                return "Yet to Start";  // Exact format the API expects

            case "In Progress":
            case "in progress":
            case "IN PROGRESS":
                return "In Progress";  // Exact format the API expects

            case "Completed":
            case "completed":
            case "COMPLETED":
                return "Completed";    // Exact format the API expects

            default:
                Log.d(TAG, "Converting unknown status to 'Yet to Start': " + status);
                return "Yet to Start";
        }
    }

    public void updateTaskStatus(Task task, String newStatus) {
        // First, ensure we have a valid future deadline
        String updatedDeadline = ensureFutureDeadline(task.getDeadline());

        // Convert status to proper API format
        String apiStatus = convertToApiFormat(newStatus);

        // Log the values for debugging
        Log.d(TAG, "Updating task - ID: " + task.getId());
        Log.d(TAG, "Updating task - Original Status: " + newStatus);
        Log.d(TAG, "Updating task - Converted Status: " + apiStatus);
        Log.d(TAG, "Updating task - Original Deadline: " + task.getDeadline());
        Log.d(TAG, "Updating task - Updated Deadline: " + updatedDeadline);

        String url = Constants.BASE_URL + "api/tasks/" + task.getId();
        showLoading();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("task_name", task.getTaskName());
            jsonBody.put("description", task.getDescription());
            jsonBody.put("priority", task.getPriority());
            jsonBody.put("status", apiStatus);
            jsonBody.put("deadline", updatedDeadline);  // Use the updated deadline

            Log.d(TAG, "Request body: " + jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonBody,
                    response -> {
                        hideLoading();
                        // Update local task object with new values
                        task.setStatus(apiStatus);
                        task.setDeadline(updatedDeadline);  // Update the local deadline
                        taskAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        hideLoading();
                        handleUpdateError(error);
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

        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating JSON body", e);
            showError("Error updating task: " + e.getMessage());
        }
    }

    // Add this helper method to handle update errors
    private void handleUpdateError(com.android.volley.VolleyError error) {
        String errorMsg = "Error updating task";
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                Log.e(TAG, "Error response: " + responseBody);

                // Parse the error response to show a more user-friendly message
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("deadline")) {
                    errorMsg = "Please select a future date for the deadline";
                } else if (errorJson.has("status")) {
                    errorMsg = "Invalid status selected";
                } else {
                    errorMsg += ": " + responseBody;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading error response", e);
            }
        }
        showError(errorMsg);
    }

    // Update the ensure future deadline method
    private String ensureFutureDeadline(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            // Get current date without time component
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            // Get tomorrow's date
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
            tomorrow.set(Calendar.MINUTE, 0);
            tomorrow.set(Calendar.SECOND, 0);
            tomorrow.set(Calendar.MILLISECOND, 0);

            // Parse the deadline
            Date deadlineDate = sdf.parse(deadline);

            // If deadline is today or in the past, set it to tomorrow
            if (deadlineDate.before(tomorrow.getTime())) {
                String newDeadline = sdf.format(tomorrow.getTime());
                Log.d(TAG, "Updating past deadline from " + deadline + " to " + newDeadline);
                return newDeadline;
            }

            return deadline;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + deadline, e);
            // Return tomorrow's date as fallback
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            String newDeadline = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(tomorrow.getTime());
            Log.d(TAG, "Using fallback deadline: " + newDeadline);
            return newDeadline;
        }
    }

    // Add this method to your TaskDetailsActivity class
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = SharedPrefManager.getInstance(this).getToken();
        if (!token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private void handleError(com.android.volley.VolleyError error) {
        String errorMessage = "An error occurred";

        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                Log.e(TAG, "Error response: " + responseBody);

                // Try to parse error message from response
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("detail")) {
                    errorMessage = errorJson.getString("detail");
                } else if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    errorMessage = errorJson.getString("error");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response", e);
                errorMessage = "Error: " + error.networkResponse.statusCode;
            }
        } else if (error.getMessage() != null) {
            errorMessage = error.getMessage();
        }

        Log.e(TAG, "Network error: " + errorMessage);
        showError(errorMessage);
    }

    private void checkDeadlinesAndNotify() {
        String url = Constants.BASE_URL + "api/tasks/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray tasks = response.getJSONArray("tasks");
                        for (int i = 0; i < tasks.length(); i++) {
                            JSONObject task = tasks.getJSONObject(i);
                            String deadline = task.getString("deadline");
                            String taskName = task.getString("task_name");
                            String status = task.getString("status");

                            // Check if task is due today and not completed
                            if (isTaskDueToday(deadline) && !status.equals("COMPLETED")) {
                                NotificationHelper.showTaskDeadlineNotification(
                                        this,
                                        taskName,
                                        "today"
                                );
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing tasks", e);
                    }
                },
                error -> Log.e(TAG, "Error fetching tasks", error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private boolean isTaskDueToday(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());
            return deadline.equals(today);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date", e);
            return false;
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    // Add this constant at the class level
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
}