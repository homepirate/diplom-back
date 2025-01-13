package com.example.diplom.services.dtos;
import com.example.diplom.models.Specialization;

public class DoctorRegistrationDto extends BaseRegistrationDto {

    private Specialization specialization;
    private String uniqueCode;

    public DoctorRegistrationDto(String password, String role, String email, String phone, String fullName, Specialization specialization, String uniqueCode) {
        super(password, role, email, phone, fullName);
        this.specialization = specialization;
        this.uniqueCode = uniqueCode;
    }

    public DoctorRegistrationDto() {
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }
}

