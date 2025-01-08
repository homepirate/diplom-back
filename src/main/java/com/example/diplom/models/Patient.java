package com.example.diplom.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "patients")
public class Patient extends User {


    private String fullName;
    private LocalDate birthDate;
    private Set<DoctorPatient> doctorPatients = new HashSet<>();
    private Set<Visit> visits = new HashSet<>();


    public Patient() {
    }


    @Column(name = "full_name", nullable = false, length = 100)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Column(name = "birth_date")
    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<DoctorPatient> getDoctorPatients() {
        return doctorPatients;
    }

    public void setDoctorPatients(Set<DoctorPatient> doctorPatients) {
        this.doctorPatients = doctorPatients;
    }

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Visit> getVisits() {
        return visits;
    }

    public void setVisits(Set<Visit> visits) {
        this.visits = visits;
    }

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", name='" + fullName + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }
}