package com.example.diplom.models;

import com.example.diplom.models.PK.DoctorPatientPK;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "doctor_patient")
@IdClass(DoctorPatientPK.class)
public class DoctorPatient {


    private UUID doctorId;
    private UUID patientId;
    private Doctor doctor;
    private Patient patient;


    public DoctorPatient() {
    }

    public DoctorPatient(Doctor doctor, Patient patient) {
        this.doctor = doctor;
        this.patient = patient;
        this.doctorId = doctor.getId();
        this.patientId = patient.getId();
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
        if (doctor != null) {
            this.doctorId = doctor.getId();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    public Patient getPatient() {
        return patient;
    }

    @Id
    @Column(name = "doctor_id", nullable = false)
    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    @Id
    @Column(name = "patient_id", nullable = false)
    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        if (patient != null) {
            this.patientId = patient.getId();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorPatient)) return false;
        DoctorPatient that = (DoctorPatient) o;
        return Objects.equals(doctorId, that.doctorId) &&
                Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doctorId, patientId);
    }

    @Override
    public String toString() {
        return "DoctorPatient{" +
                "doctorId=" + doctorId +
                ", patientId=" + patientId +
                '}';
    }
}
