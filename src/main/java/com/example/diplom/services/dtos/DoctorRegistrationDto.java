package com.example.diplom.services.dtos;

public class DoctorRegistrationDto extends BaseRegistrationDto{

    private String specialization;
    private String uniqueCode;

    public DoctorRegistrationDto(String password, String role, String email, String phone, String fullName, String specialization, String uniqueCode) {
        super(password, role, email, phone, fullName);
        this.specialization = specialization;
        this.uniqueCode = uniqueCode;
    }

    public DoctorRegistrationDto() {
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }
}
