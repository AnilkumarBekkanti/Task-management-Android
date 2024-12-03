package com.example.loginscreen;

public class TaskStatistics {
    private int totalTasks;
    private int completedTasks;
    private int inProgressTasks;
    private int yetToStartTasks;

    public TaskStatistics(int totalTasks, int completedTasks, int inProgressTasks, int yetToStartTasks) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.yetToStartTasks = yetToStartTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getInProgressTasks() {
        return inProgressTasks;
    }

    public int getYetToStartTasks() {
        return yetToStartTasks;
    }
}