package com.example.loginscreen;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminTasksActivity extends AppCompatActivity {
    private static final String TAG = "AdminTasksActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private List<Task> taskList;
    private TaskAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private List<Task> originalTaskList;
    private com.google.android.material.chip.Chip chipLow, chipMedium, chipHigh;
    private com.google.android.material.chip.Chip chipYetToStart, chipInProgress, chipCompleted;
    private com.google.android.material.chip.Chip chipToday, chipWeek, chipMonth, chipYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tasks);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearchView();
        setupFilters();
        loadAllTasks();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Users' Tasks");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(this, taskList, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadAllTasks);
    }

    private void setupSearchView() {
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTasks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText);
                return true;
            }
        });
    }

    private void setupFilters() {
        // Initialize chips
        chipLow = findViewById(R.id.chipLow);
        chipMedium = findViewById(R.id.chipMedium);
        chipHigh = findViewById(R.id.chipHigh);
        chipYetToStart = findViewById(R.id.chipYetToStart);
        chipInProgress = findViewById(R.id.chipInProgress);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipToday = findViewById(R.id.chipToday);
        chipWeek = findViewById(R.id.chipWeek);
        chipMonth = findViewById(R.id.chipMonth);
        chipYear = findViewById(R.id.chipYear);

        View.OnClickListener chipClickListener = v -> {
            // Uncheck other chips in the same category
            if (v == chipLow || v == chipMedium || v == chipHigh) {
                handlePriorityFilter((com.google.android.material.chip.Chip) v);
            } else if (v == chipYetToStart || v == chipInProgress || v == chipCompleted) {
                handleStatusFilter((com.google.android.material.chip.Chip) v);
            } else if (v == chipToday || v == chipWeek || v == chipMonth || v == chipYear) {
                handleDeadlineFilter((com.google.android.material.chip.Chip) v);
            }
            applyFilters();
        };

        // Assign click listeners
        chipLow.setOnClickListener(chipClickListener);
        chipMedium.setOnClickListener(chipClickListener);
        chipHigh.setOnClickListener(chipClickListener);
        chipYetToStart.setOnClickListener(chipClickListener);
        chipInProgress.setOnClickListener(chipClickListener);
        chipCompleted.setOnClickListener(chipClickListener);
        chipToday.setOnClickListener(chipClickListener);
        chipWeek.setOnClickListener(chipClickListener);
        chipMonth.setOnClickListener(chipClickListener);
        chipYear.setOnClickListener(chipClickListener);
    }

    private void handlePriorityFilter(com.google.android.material.chip.Chip selectedChip) {
        chipLow.setChecked(selectedChip == chipLow);
        chipMedium.setChecked(selectedChip == chipMedium);
        chipHigh.setChecked(selectedChip == chipHigh);
    }

    private void handleStatusFilter(com.google.android.material.chip.Chip selectedChip) {
        chipYetToStart.setChecked(selectedChip == chipYetToStart);
        chipInProgress.setChecked(selectedChip == chipInProgress);
        chipCompleted.setChecked(selectedChip == chipCompleted);
    }

    private void handleDeadlineFilter(com.google.android.material.chip.Chip selectedChip) {
        chipToday.setChecked(selectedChip == chipToday);
        chipWeek.setChecked(selectedChip == chipWeek);
        chipMonth.setChecked(selectedChip == chipMonth);
        chipYear.setChecked(selectedChip == chipYear);
    }

    private void applyFilters() {
        if (originalTaskList == null) return;

        List<Task> filteredList = new ArrayList<>(originalTaskList);

        // Apply priority filter
        if (chipLow.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getPriority().equals("Low"))
                    .collect(Collectors.toList());
        } else if (chipMedium.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getPriority().equals("Medium"))
                    .collect(Collectors.toList());
        } else if (chipHigh.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getPriority().equals("High"))
                    .collect(Collectors.toList());
        }

        // Apply status filter
        if (chipYetToStart.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getStatus().equals("Yet to Start"))
                    .collect(Collectors.toList());
        } else if (chipInProgress.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getStatus().equals("In Progress"))
                    .collect(Collectors.toList());
        } else if (chipCompleted.isChecked()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getStatus().equals("Completed"))
                    .collect(Collectors.toList());
        }

        // Apply deadline filter
        if (chipToday.isChecked()) {
            filteredList = filterByDeadlinePeriod(filteredList, 0); // Today
        } else if (chipWeek.isChecked()) {
            filteredList = filterByDeadlinePeriod(filteredList, 7); // Week
        } else if (chipMonth.isChecked()) {
            filteredList = filterByDeadlinePeriod(filteredList, 30); // Month
        } else if (chipYear.isChecked()) {
            filteredList = filterByDeadlinePeriod(filteredList, 365); // Year
        }

        taskList.clear();
        taskList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    private List<Task> filterByDeadlinePeriod(List<Task> tasks, int days) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar endDate = (Calendar) today.clone();
        endDate.add(Calendar.DAY_OF_YEAR, days);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        return tasks.stream()
                .filter(task -> {
                    if (task.getDeadline() == null || task.getDeadline().isEmpty()) {
                        return false;
                    }
                    try {
                        Date taskDate = sdf.parse(task.getDeadline());
                        if (taskDate != null) {
                            return !taskDate.before(today.getTime()) && 
                                   (days == 0 ? !taskDate.after(today.getTime()) : !taskDate.after(endDate.getTime()));
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date: " + e.getMessage());
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private void filterTasks(String query) {
        if (originalTaskList == null) {
            originalTaskList = new ArrayList<>(taskList);
        }

        if (query.isEmpty()) {
            taskList.clear();
            taskList.addAll(originalTaskList);
        } else {
            List<Task> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Task task : originalTaskList) {
                if (task.getTaskName().toLowerCase().contains(lowerCaseQuery) ||
                    task.getDescription().toLowerCase().contains(lowerCaseQuery) ||
                    task.getUserName().toLowerCase().contains(lowerCaseQuery) ||
                    task.getStatus().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(task);
                }
            }
            taskList.clear();
            taskList.addAll(filteredList);
        }
        adapter.notifyDataSetChanged();
    }

    private void loadAllTasks() {
        showLoading();
        String url = Constants.ADMIN_TASKS_URL;
        Log.d(TAG, "Loading admin tasks from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    hideLoading();
                    try {
                        Log.d(TAG, "Received response: " + response.toString());
                        JSONArray tasksArray = response.getJSONArray("tasks");
                        taskList.clear();
                        Log.d(TAG, "Processing " + tasksArray.length() + " tasks");

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObj = tasksArray.getJSONObject(i);
                            Log.d(TAG, "Processing task: " + taskObj.toString());

                            String creatorUsername = taskObj.optString("creator_username", "Unknown");

                            Task task = new Task(
                                    taskObj.getInt("id"),
                                    taskObj.getString("task_name"),
                                    taskObj.optString("description", ""),
                                    taskObj.optString("priority", "Medium"),
                                    taskObj.optString("status", "Yet to Start"),
                                    taskObj.optString("deadline", ""),
                                    creatorUsername
                            );
                            taskList.add(task);
                        }

                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);

                        if (taskList.isEmpty()) {
                            showError("No tasks found");
                        }
                        originalTaskList = new ArrayList<>(taskList);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing tasks: " + e.getMessage(), e);
                        showError("Error parsing tasks data");
                    }
                },
                error -> {
                    hideLoading();
                    handleError(error);
                    swipeRefreshLayout.setRefreshing(false);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(AdminTasksActivity.this).getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    Log.d(TAG, "Token added to request: " + token.substring(0, Math.min(token.length(), 10)) + "...");
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllTasks();
    }
}