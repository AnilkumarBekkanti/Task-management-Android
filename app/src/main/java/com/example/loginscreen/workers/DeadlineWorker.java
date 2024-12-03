package com.example.loginscreen.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.loginscreen.Constants;
import com.example.loginscreen.SharedPrefManager;
import com.example.loginscreen.helpers.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class DeadlineWorker extends Worker {
    private static final String TAG = "DeadlineWorker";

    public DeadlineWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        checkDeadlines(context);
        return Result.success();
    }

    private void checkDeadlines(Context context) {
        String url = Constants.BASE_URL + "api/tasks/deadlines/";
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        CountDownLatch latch = new CountDownLatch(1);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    JSONArray tasks = response.getJSONArray("tasks");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String today = sdf.format(new Date());

                    for (int i = 0; i < tasks.length(); i++) {
                        JSONObject task = tasks.getJSONObject(i);
                        String deadline = task.getString("deadline");
                        String taskName = task.getString("task_name");
                        String status = task.getString("status");

                        // Check if task is due today and not completed
                        if (deadline.equals(today) && !status.equals("COMPLETED")) {
                            NotificationHelper.showDeadlineNotification(
                                context,
                                taskName,
                                "today"
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking deadlines", e);
                }
                latch.countDown();
            },
            error -> {
                Log.e(TAG, "Error fetching deadlines", error);
                latch.countDown();
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(context).getToken();
                if (!token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
        }
    }
}