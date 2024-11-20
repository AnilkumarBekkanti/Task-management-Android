package com.example.loginscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;

public class SignUp extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_sign_up);

        // Initialize the EditText fields for user input
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        emailEditText = findViewById(R.id.email);

        // Handle login navigation
        TextView loginTextView = findViewById(R.id.loginText);
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the Login screen
                Intent intent = new Intent(SignUp.this, Login.class);
                startActivity(intent);
            }
        });

        // Handle sign-up button click
        findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get input from the user
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();

                // Check if all fields are filled
                if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                    Toast.makeText(SignUp.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Call AsyncTask to register the user
                    new SignUpTask().execute(username, password, email);
                }
            }
        });
    }

    // AsyncTask for making the HTTP request in the background
    private class SignUpTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            String email = params[2];

            try {
                // URL of the sign-up API endpoint
                URL url = new URL("http://13.234.41.119/devenv/ss_apis/signup.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                // Create the POST data
                String postData = "username=" + username + "&password=" + password + "&email=" + email;

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
                    return response.toString(); // Return the server's response
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
            // Handle the server response here
            super.onPostExecute(result);

            if (result.contains("success")) {
                // Registration successful, navigate to login screen
                Toast.makeText(SignUp.this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignUp.this, Login.class); // Navigate to Login page
                startActivity(intent);
                finish(); // Close the sign-up screen
            } else {
                // Registration failed
                Toast.makeText(SignUp.this, "Registration failed: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
