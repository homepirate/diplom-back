package com.example.diplom.models;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctors")
public class Doctor extends User {

    private String fullName;
    private String specialization;
    private String uniqueCode;
    private Set<DoctorPatient> doctorPatients = new HashSet<>();
    private Set<Service> services = new HashSet<>();

    public Doctor() {
    }

    @Column(name = "full_name", nullable = false, length = 100)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Column(name = "specialization", length = 100)
    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    @Column(name = "unique_code", length = 7, nullable = false, unique = true)
    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<DoctorPatient> getDoctorPatients() {
        return doctorPatients;
    }

    public void setDoctorPatients(Set<DoctorPatient> doctorPatients) {
        this.doctorPatients = doctorPatients;
    }

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Service> getServices() {
        return services;
    }

    public void setServices(Set<Service> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", specialization='" + specialization + '\'' +
                ", uniqueCode='" + uniqueCode + '\'' +
                '}';
    }
}
