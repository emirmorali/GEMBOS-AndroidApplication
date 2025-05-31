package com.example.gembos;

public class ProfileModel {
    private String phoneNumber;
    private String name;
    private String surname;
    private String password;
    private String about;
    private String profileImage;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public ProfileModel(String phoneNumber, String name, String surname, String password, String about, String profileImage) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.surname = surname;
        this.password = password;
        this.about = about;
        this.profileImage = profileImage;
    }
}
