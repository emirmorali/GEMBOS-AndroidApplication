package com.example.gembos.DTOs;

public class UserDTO {
    private String phoneNumber;
    private String name;
    private String surname;
    private String password;

    public UserDTO(String phoneNumber, String name, String surname, String password) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.surname = surname;
        this.password = password;
    }
}
