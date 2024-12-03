package com.example.loginscreen;

public class ProfileData {
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String bio;
    private String profileImageUrl;

    public ProfileData(String username, String email, String fullName, String phone, String bio, String profileImageUrl) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // Builder pattern for easier object creation
    public static class Builder {
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String bio;
        private String profileImageUrl;

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setBio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public ProfileData build() {
            return new ProfileData(username, email, fullName, phone, bio, profileImageUrl);
        }
    }
}