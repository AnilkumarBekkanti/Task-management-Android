package com.example.loginscreen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {
    private MutableLiveData<TaskStatistics> taskStatistics;
    private MutableLiveData<ProfileData> profileData;
    private MutableLiveData<Boolean> loadingState;

    public ProfileViewModel() {
        taskStatistics = new MutableLiveData<>();
        profileData = new MutableLiveData<>();
        loadingState = new MutableLiveData<>(false);
    }

    public LiveData<TaskStatistics> getTaskStatistics() {
        return taskStatistics;
    }

    public LiveData<ProfileData> getProfileData() {
        return profileData;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public void setTaskStatistics(TaskStatistics stats) {
        taskStatistics.setValue(stats);
    }

    public void setProfileData(ProfileData data) {
        profileData.setValue(data);
    }

    public void setLoading(boolean isLoading) {
        loadingState.setValue(isLoading);
    }
}