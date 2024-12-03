package com.example.loginscreen;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import android.util.Log;

public class VolleySingleton {
    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private static Context context;
    private static final String TAG = "VolleySingleton";

    private VolleySingleton(Context context) {
        VolleySingleton.context = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            try {
                requestQueue = Volley.newRequestQueue(context.getApplicationContext());
                Log.d(TAG, "RequestQueue initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing RequestQueue: " + e.getMessage());
            }
        }
        return requestQueue;
    }
}
