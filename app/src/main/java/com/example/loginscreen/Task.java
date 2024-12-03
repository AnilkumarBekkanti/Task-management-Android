package com.example.loginscreen;

import java.io.Serializable;

public class Task implements Serializable {
    private int id;
    private String task_name;
    private String description;
    private String priority;
    private String status;
    private String deadline;
    private String userName;

    // Constructor for regular tasks
    public Task(int id, String task_name, String description, String priority, String status, String deadline) {
        this.id = id;
        this.task_name = task_name;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.deadline = deadline;
    }

    // Constructor for admin tasks (includes userName)
    public Task(int id, String task_name, String description, String priority, String status, String deadline, String userName) {
        this.id = id;
        this.task_name = task_name;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.deadline = deadline;
        this.userName = userName;
    }

    public Task() {
        // Empty constructor
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTaskName() { return task_name; }
    public void setTaskName(String taskName) { this.task_name = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}