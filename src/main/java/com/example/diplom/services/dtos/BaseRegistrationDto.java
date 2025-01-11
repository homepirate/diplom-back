package com.example.diplom.services.dtos;

public class BaseRegistrationDto {
    private String password;
    private String role;
    private String email;
    private String phone;
    private String fullName;

    public BaseRegistrationDto(String password, String role, String email, String phone, String fullName) {
        this.password = password;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
    }

    public BaseRegistrationDto() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "BaseRegistrationDto{" +
                "password='" + password + '\'' +
                ", role='" + role + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}
