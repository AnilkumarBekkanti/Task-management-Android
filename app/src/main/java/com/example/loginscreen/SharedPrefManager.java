package com.example.loginscreen;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPrefManager {
    private static final String PREF_NAME = "app_prefs";
    private static final String TOKEN_KEY = "jwt_token";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String USER_ID_KEY = "user_id";
    private static final String LOGIN_TIMESTAMP = "login_timestamp";
    private static final String PROFILE_IMAGE_URL_KEY = "profile_image_url";
    private static final String IS_SUPERUSER_KEY = "is_superuser";

    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // Token methods
    public SharedPrefManager setToken(String token) {
        editor.putString(TOKEN_KEY, token).apply();
        return this;
    }

    public String getToken() {
        Log.d(TAG, "Getting token from SharedPreferences");
        String token = sharedPreferences.getString(TOKEN_KEY, null);
        Log.d(TAG, "Token exists: " + (token != null && !token.isEmpty()));
        return token;
    }

    // Username methods
    public SharedPrefManager setUsername(String username) {
        editor.putString(USERNAME_KEY, username).apply();
        return this;
    }

    public String getUsername() {
        return sharedPreferences.getString(USERNAME_KEY, "");
    }

    // Email methods
    public SharedPrefManager setEmail(String email) {
        editor.putString(EMAIL_KEY, email).apply();
        return this;
    }

    public String getEmail() {
        return sharedPreferences.getString(EMAIL_KEY, "");
    }

    // User ID methods
    public SharedPrefManager setUserId(String userId) {
        editor.putString(USER_ID_KEY, userId).apply();
        return this;
    }

    public String getUserId() {
        return sharedPreferences.getString(USER_ID_KEY, "");
    }

    // Logout method
    public void logout() {
        // Store the image
        String imageUrl = getProfileImageUrl();

        // Clear all data
        editor.clear();
        editor.apply();

        // Restore the image URL if it exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            setProfileImageUrl(imageUrl);
        }

        Log.d(TAG, "User logged out, preferences cleared except profile image URL");
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        Log.d(TAG, "Checking if user is logged in");
        boolean hasToken = sharedPreferences.contains(TOKEN_KEY);
        Log.d(TAG, "Has token: " + hasToken);
        return hasToken;
    }

    public boolean isTokenExpired() {
        long loginTime = sharedPreferences.getLong(LOGIN_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000; // 24 hours

        return (currentTime - loginTime) > dayInMillis;
    }

    public void saveLoginTimestamp(long timestamp) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LOGIN_TIMESTAMP, timestamp);
        editor.apply();
        Log.d(TAG, "Login timestamp saved: " + timestamp);
    }

    public long getLoginTimestamp() {
        return sharedPreferences.getLong(LOGIN_TIMESTAMP, 0);
    }

    // Profile image URL methods
    public SharedPrefManager setProfileImageUrl(String imageUrl) {
        editor.putString(PROFILE_IMAGE_URL_KEY, imageUrl);
        editor.apply();
        return this;
    }

    public String getProfileImageUrl() {
        return sharedPreferences.getString(PROFILE_IMAGE_URL_KEY, "");
    }

    // Superuser methods
    public SharedPrefManager setIsSuperuser(boolean isSuperuser) {
        editor.putBoolean(IS_SUPERUSER_KEY, isSuperuser).apply();
        return this;
    }

    public boolean getIsSuperuser() {
        return sharedPreferences.getBoolean(IS_SUPERUSER_KEY, false);
    }
}