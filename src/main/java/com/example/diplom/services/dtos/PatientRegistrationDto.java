package com.example.diplom.services.dtos;

import java.time.LocalDate;

public class PatientRegistrationDto extends BaseRegistrationDto{
    private LocalDate birthDate;

    public PatientRegistrationDto(String password, String role, String email, String phone, String fullName, LocalDate birthDate) {
        super(password, role, email, phone, fullName);
        this.birthDate = birthDate;
    }

    public PatientRegistrationDto(String password, String role, String email, String phone, String fullName) {
        super(password, role, email, phone, fullName);
    }

    public PatientRegistrationDto() {
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "PatientRegistrationDto{" +
                "birthDate=" + birthDate +
                "} " + super.toString();
    }
}
