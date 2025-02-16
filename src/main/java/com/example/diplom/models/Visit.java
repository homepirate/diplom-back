package com.example.diplom.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "visits")
public class Visit extends Base {

    private Patient patient;
    private Doctor doctor;
    private LocalDateTime visitDate;
    private String notes;
    private Set<VisitService> visitServices = new HashSet<>();
    private Set<Attachment> attachments = new HashSet<>();
    private boolean isFinished;
    private BigDecimal totalCost;

    public Visit() {
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    @Column(name = "visit_date", nullable = false)
    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }

    @Column(name = "notes")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<VisitService> getVisitServices() {
        return visitServices;
    }

    public void setVisitServices(Set<VisitService> visitServices) {
        this.visitServices = visitServices;
    }

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = attachments;
    }

    @Column(name = "is_finished", nullable = false)
    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", visitDate=" + visitDate +
                ", notes='" + notes + '\'' +
                ", isFinished=" + isFinished +
                ", totalCost=" + totalCost +
                '}';
    }
}
