package com.example.loginscreen;

public class Constants {
    // Base URL for emulator
    public static final String BASE_URL = "http://10.0.2.2:8000/";

    // Authentication Endpoints
    public static final String LOGIN_URL = BASE_URL + "api/users/login/";
    public static final String REGISTER_URL = BASE_URL + "api/users/register/";
    public static final String UPDATE_PASSWORD_URL = BASE_URL + "api/users/update-password/";
    public static final String FORGOT_PASSWORD_URL = BASE_URL + "api/auth/forgot-password/";
    public static final String REFRESH_TOKEN_URL = BASE_URL + "api/auth/token/refresh/";

    // Task Endpoints
    public static final String TASKS_URL = BASE_URL + "api/tasks/";
    public static final String CREATE_TASK_URL = BASE_URL + "api/tasks/";
    public static final String UPDATE_TASK_URL = BASE_URL + "api/tasks/%d/";  // Use with String.format()
    public static final String DELETE_TASK_URL = BASE_URL + "api/tasks/%d/";  // Use with String.format()
    public static final String TASK_STATS_URL = BASE_URL + "api/tasks/stats/";
    public static final String USER_TASKS_URL = BASE_URL + "api/tasks/user/";
    public static final String FILTERED_TASKS_URL = BASE_URL + "api/tasks/filtered/";

    // Profile Endpoints
    public static final String PROFILE_URL = BASE_URL + "api/users/profile/";
    public static final String UPDATE_PROFILE_URL = BASE_URL + "api/users/profile/update/";
    public static final String UPDATE_PROFILE_IMAGE_URL = BASE_URL + "api/users/profile/update-image/"; // Updated URL
    public static final String REMOVE_PROFILE_IMAGE_URL = BASE_URL + "api/users/profile/remove-image/";
    public static final String GET_PROFILE_IMAGE_URL = BASE_URL + "api/users/profile/image/";

    // Shared Preferences Keys
    public static final String PREF_NAME = "app_prefs";
    public static final String TOKEN_KEY = "jwt_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String USER_ID_KEY = "user_id";
    public static final String USERNAME_KEY = "username";
    public static final String EMAIL_KEY = "email";
    public static final String PROFILE_IMAGE_KEY = "profile_image";
    public static final String PROFILE_IMAGE_URL_KEY = "profile_image_url";
    public static final String LAST_SYNC_KEY = "last_sync";
    public static final String THEME_KEY = "app_theme";
    public static final String NOTIFICATION_KEY = "notifications_enabled";
    public static final String LOGIN_TIMESTAMP = "login_timestamp";

    // Request Codes
    public static final int PICK_IMAGE_REQUEST = 1;
    public static final int PERMISSION_REQUEST_CODE = 2;
    public static final int CAMERA_REQUEST = 3;
    public static final int SETTINGS_REQUEST = 4;

    // Image Constants
    public static final int MAX_IMAGE_DIMENSION = 800;
    public static final int IMAGE_COMPRESSION_QUALITY = 70;
    public static final String IMAGE_MIME_TYPE = "image/jpeg";
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String DEFAULT_PROFILE_IMAGE = "default_profile";

    // Network Constants
    public static final int REQUEST_TIMEOUT = 30000; // 30 seconds
    public static final int MAX_RETRIES = 0;
    public static final int BACKOFF_MULTIPLIER = 1;
    public static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    public static final int READ_TIMEOUT = 30000; // 30 seconds

    // Task Status Constants
    public static final String STATUS_YET_TO_START = "Yet to Start";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_ON_HOLD = "On Hold";

    // Task Priority Constants
    public static final String PRIORITY_LOW = "Low";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_HIGH = "High";

    // Validation Constants
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 30;
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$";
    public static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    // Cache Constants
    public static final long CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24 hours
    public static final int MAX_CACHE_SIZE = 10 * 1024 * 1024; // 10MB

    // Error Messages
    public static final String ERROR_NETWORK = "Network error. Please check your connection.";
    public static final String ERROR_SERVER = "Server error. Please try again later.";
    public static final String ERROR_TIMEOUT = "Request timed out. Please try again.";
    public static final String ERROR_AUTH = "Authentication failed. Please login again.";
    public static final String ERROR_VALIDATION = "Please check your input and try again.";
    public static final String ERROR_IMAGE_SIZE = "Image size too large. Maximum size is 5MB.";
    public static final String ERROR_IMAGE_FORMAT = "Invalid image format.";
    public static final String ERROR_PROFILE_UPDATE = "Failed to update profile.";
    public static final String ERROR_PASSWORD_MATCH = "New passwords do not match.";
    public static final String ERROR_CURRENT_PASSWORD = "Current password is incorrect.";

    // Success Messages
    public static final String SUCCESS_PROFILE_UPDATE = "Profile updated successfully";
    public static final String SUCCESS_PASSWORD_UPDATE = "Password updated successfully";
    public static final String SUCCESS_IMAGE_UPLOAD = "Profile picture updated successfully";
    public static final String SUCCESS_IMAGE_REMOVE = "Profile picture removed successfully";
    public static final String SUCCESS_REGISTRATION = "Registration successful";
    public static final String SUCCESS_LOGIN = "Login successful";
    public static final String SUCCESS_LOGOUT = "Logout successful";
    public static final String SUCCESS_PASSWORD_RESET = "Password reset email sent";

    // Permission Messages
    public static final String PERMISSION_CAMERA = "Camera permission is required to take photos";
    public static final String PERMISSION_STORAGE = "Storage permission is required to select photos";
    public static final String PERMISSION_DENIED = "Permission denied. Please enable in settings.";

    // Dialog Messages
    public static final String DIALOG_DELETE_TITLE = "Delete Task";
    public static final String DIALOG_DELETE_MESSAGE = "Are you sure you want to delete this task?";
    public static final String DIALOG_LOGOUT_TITLE = "Logout";
    public static final String DIALOG_LOGOUT_MESSAGE = "Are you sure you want to logout?";
    public static final String DIALOG_YES = "Yes";
    public static final String DIALOG_NO = "No";
    public static final String DIALOG_CANCEL = "Cancel";
    public static final String DIALOG_OK = "OK";

    // Admin Endpoints
    public static final String ADMIN_TASKS_URL = BASE_URL + "api/admin/tasks/all/";
    public static final String ADMIN_STATS_URL = BASE_URL + "api/admin/tasks/stats/";
    public static final String ADMIN_TASK_DETAIL_URL = BASE_URL + "api/admin/tasks/%d/";

    // Verify Username Endpoint
    public static final String VERIFY_USERNAME_URL = BASE_URL + "api/users/verify-username/";
}