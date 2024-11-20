package com.example.loginscreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.os.AsyncTask;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Login extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_login);

        // Initialize EditText views for username and password
        usernameEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);

        // Handle Sign Up navigation
        TextView signUpTextView = findViewById(R.id.textViewSignUp);
        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SignUp activity
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
            }
        });

        // Handle login button click (assuming you have a login button)
        findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Login.this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                } else {
                    new LoginTask().execute(username, password);
                }
            }
        });
    }

    // AsyncTask for making the HTTP request in the background
    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            try {

                URL url   = new URL("http://13.234.41.119/devenv/ss_apis/login.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                // Create the POST data
                String postData = "email=" + username + "&password=" + password;

                // Send the POST request
                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Get the response code
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response from the input stream
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    int data = reader.read();
                    StringBuilder response = new StringBuilder();
                    while (data != -1) {
                        char current = (char) data;
                        response.append(current);
                        data = reader.read();
                    }
                    return response.toString();
                } else {
                    return "Error: Unable to connect to the server.";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result.contains("success")) {
                Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Login.this, Dashboard.class); // Changed from SignUp to Dashboard
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(Login.this, "Login failed: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
