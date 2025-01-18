package com.example.diplom.services.dtos;

import com.example.diplom.models.Attachment;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.models.VisitService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VisitDto {

    private UUID id;

    private PatientDto patient;
    private DoctorDto doctor;
    private LocalDateTime visitDate;
    private String notes;
    private Set<VisitServiceDto> visitServices = new HashSet<>();
    private AttachmentDto attachment;


    public VisitDto(UUID id, PatientDto patient, DoctorDto doctor, LocalDateTime visitDate, String notes, Set<VisitServiceDto> visitServices, AttachmentDto attachment) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.visitDate = visitDate;
        this.notes = notes;
        this.visitServices = visitServices;
        this.attachment = attachment;
    }

    public VisitDto() {
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }

    public DoctorDto getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorDto doctor) {
        this.doctor = doctor;
    }

    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<VisitServiceDto> getVisitServices() {
        return visitServices;
    }

    public void setVisitServices(Set<VisitServiceDto> visitServices) {
        this.visitServices = visitServices;
    }

    public AttachmentDto getAttachment() {
        return attachment;
    }

    public void setAttachment(AttachmentDto attachment) {
        this.attachment = attachment;
    }
}
