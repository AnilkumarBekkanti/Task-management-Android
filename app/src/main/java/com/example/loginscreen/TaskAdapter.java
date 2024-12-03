package com.example.loginscreen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.content.res.ColorStateList;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private static final String TAG = "TaskAdapter";
    private Context context;
    private List<Task> taskList;
    private boolean isAdminView;

    public TaskAdapter(Context context, List<Task> taskList, boolean isAdminView) {
        this.context = context;
        this.taskList = taskList;
        this.isAdminView = isAdminView;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(isAdminView ? R.layout.item_admin_task : R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        Log.d(TAG, "Binding task at position " + position + ": " + task.getTaskName());
        holder.bind(task);

        // Edit button click
        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditTaskActivity.class);
            intent.putExtra("task_id", task.getId());
            intent.putExtra("task_name", task.getTaskName());
            intent.putExtra("description", task.getDescription());
            intent.putExtra("status", task.getStatus());
            intent.putExtra("priority", task.getPriority());
            intent.putExtra("deadline", task.getDeadline());
            if (isAdminView) {
                intent.putExtra("username", task.getUserName());
            }
            context.startActivity(intent);
        });

        // Delete button click
        holder.deleteButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog(task);
        });

        // Set background colors based on status and priority
        setStatusBackground(holder.statusTextView, task.getStatus());
        setPriorityBackground(holder.priorityTextView, task.getPriority());
    }

    private void showDeleteConfirmationDialog(Task task) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTask(task);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(Task task) {
        String url = Constants.BASE_URL + "api/tasks/" + task.getId() + "/";
        if (isAdminView) {
            url = Constants.BASE_URL + "api/admin/tasks/" + task.getId() + "/";
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    taskList.remove(task);
                    notifyDataSetChanged();
                    Toast.makeText(context, "Task deleted successfully", Toast.LENGTH_SHORT).show();

                    // Broadcast to update dashboard
                    Intent updateIntent = new Intent(Dashboard.ACTION_UPDATE_DASHBOARD);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
                },
                error -> {
                    Toast.makeText(context, "Error deleting task", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting task: " + error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(context).getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    private void setStatusBackground(TextView textView, String status) {
        int colorResId;
        switch (status) {
            case "Yet to Start":
                colorResId = R.color.status_yet_to_start;
                break;
            case "In Progress":
                colorResId = R.color.status_in_progress;
                break;
            case "Completed":
                colorResId = R.color.status_completed;
                break;
            case "On Hold":
                colorResId = R.color.status_on_hold;
                break;
            default:
                colorResId = R.color.status_default;
        }
        textView.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, colorResId)));
        textView.setText(status);
    }

    private void setPriorityBackground(TextView textView, String priority) {
        int colorResId;
        switch (priority) {
            case "High":
                colorResId = R.color.priority_high;
                break;
            case "Medium":
                colorResId = R.color.priority_medium;
                break;
            case "Low":
                colorResId = R.color.priority_low;
                break;
            default:
                colorResId = R.color.priority_default;
        }
        textView.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, colorResId)));
        textView.setText(priority);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount called, returning: " + (taskList != null ? taskList.size() : 0));
        return taskList != null ? taskList.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView statusTextView;
        TextView priorityTextView;
        TextView taskNameTextView;
        TextView descriptionTextView;
        TextView deadlineTextView;
        TextView usernameTextView; // For admin view
        Button editButton;
        Button deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskNameTextView = itemView.findViewById(R.id.taskNameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            priorityTextView = itemView.findViewById(R.id.priorityTextView);
            deadlineTextView = itemView.findViewById(R.id.deadlineTextView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView); // May be null for regular view
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Task task) {
            taskNameTextView.setText(task.getTaskName());
            descriptionTextView.setText(task.getDescription());
            statusTextView.setText(task.getStatus());
            priorityTextView.setText(task.getPriority());
            deadlineTextView.setText(task.getDeadline());

            // Show username if in admin view and username TextView exists
            if (usernameTextView != null && task.getUserName() != null) {
                usernameTextView.setVisibility(View.VISIBLE);
                usernameTextView.setText("Created by: " + task.getUserName());
            }
        }
    }

    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }
}