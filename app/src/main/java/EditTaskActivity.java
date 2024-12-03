package com.example.loginscreen;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class EditTaskActivity extends AppCompatActivity {
    private static final String TAG = "EditTaskActivity";

    private EditText taskNameEditText;
    private EditText descriptionEditText;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private EditText deadlineEditText;
    private Button updateTaskButton;
    private RequestQueue requestQueue;
    private int taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task); // Reusing the create task layout

        requestQueue = Volley.newRequestQueue(this);
        initializeViews();
        setupSpinners();
        setupDatePicker();
        loadTaskData();
        setupUpdateButton();
    }

    private void initializeViews() {
        taskNameEditText = findViewById(R.id.taskNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        statusSpinner = findViewById(R.id.statusSpinner);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        deadlineEditText = findViewById(R.id.deadlineEditText);
        updateTaskButton = findViewById(R.id.saveTaskButton);
        updateTaskButton.setText("Update Task"); // Change button text
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

    private void loadTaskData() {
        Intent intent = getIntent();
        taskId = intent.getIntExtra("task_id", -1);
        if (taskId == -1) {
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskNameEditText.setText(intent.getStringExtra("task_name"));
        descriptionEditText.setText(intent.getStringExtra("description"));
        deadlineEditText.setText(intent.getStringExtra("deadline"));

        // Set spinner selections
        String status = intent.getStringExtra("status");
        String priority = intent.getStringExtra("priority");

        ArrayAdapter statusAdapter = (ArrayAdapter) statusSpinner.getAdapter();
        int statusPosition = statusAdapter.getPosition(status);
        statusSpinner.setSelection(statusPosition);

        ArrayAdapter priorityAdapter = (ArrayAdapter) prioritySpinner.getAdapter();
        int priorityPosition = priorityAdapter.getPosition(priority);
        prioritySpinner.setSelection(priorityPosition);
    }

    private void setupUpdateButton() {
        updateTaskButton.setOnClickListener(v -> updateTask());
    }

    private void updateTask() {
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
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "Updating task with ID: " + taskId);
        Log.d(TAG, "Request body: " + requestBody.toString());

        String url = Constants.BASE_URL + "api/tasks/" + taskId + "/";
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,  // Make sure your API supports PUT
                url,
                requestBody,
                response -> {
                    Log.d(TAG, "Task update response: " + response.toString());
                    Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();

                    // Send broadcast to update both Dashboard and TaskDetails
                    Intent updateIntent = new Intent(Dashboard.ACTION_UPDATE_DASHBOARD);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);

                    // Return to TaskDetails and refresh
                    Intent intent = new Intent(this, TaskDetailsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    Log.e(TAG, "Error updating task", error);
                    String errorMsg = "Error updating task";
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Error response: " + responseBody);
                            errorMsg += "\n" + responseBody;
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }
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
                    headers.put("Content-Type", "application/json");  // Add content type header
                }
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending update request to: " + url);
        requestQueue.add(request);
    }
}