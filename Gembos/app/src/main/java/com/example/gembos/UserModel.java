package com.example.gembos;

public class UserModel {
    private String phoneNumber;
    private String name;
    private String surname;
    private String password;

    public UserModel(String phoneNumber, String name, String surname, String password) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.surname = surname;
        this.password = password;
    }

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
}
