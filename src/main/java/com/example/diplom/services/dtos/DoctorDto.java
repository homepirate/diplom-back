package com.example.diplom.services.dtos;

import com.example.diplom.models.Specialization;

public class DoctorDto {


    private String fullName;
    private Specialization specialization;
    private String uniqueCode;


    public DoctorDto(String fullName, Specialization specialization, String uniqueCode) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.uniqueCode = uniqueCode;
    }

    public DoctorDto() {
    }


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
