package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.controllers.interfaces.PatientAPI;
import com.example.diplom.exceptions.AlreadyLinkedException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.exceptions.StatusResponse;
import com.example.diplom.models.Patient;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.AttachmentDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
public class PatientController implements PatientAPI {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final AttachmentService attachmentService;
    private final PatientService patientService;

    @Autowired
    public PatientController(AttachmentService attachmentService, PatientService patientService) {
        this.attachmentService = attachmentService;
        this.patientService = patientService;
    }

    @Override
    public PatientResponse getAllPatients(int page) {
        logger.debug("getAllPatients called with page: {}", page);
        return null;
    }

    @Override
    public ResponseEntity<?> AddAttachment(@Valid @ModelAttribute AddAttachmentRequest addAttachmentRequest) {
        UUID patientId = getPatientId();
        logger.info("Начало обработки запроса на добавление вложения для пациента с id: {}", patientId);
        try {
            AttachmentDto attachment = attachmentService.addAttachment(patientId, addAttachmentRequest);
            logger.info("Вложение успешно добавлено для пациента с id: {}", patientId);
            return ResponseEntity.ok(new StatusResponse("Add", "Attachment added"));
        } catch (ResourceNotFoundException e) {
            logger.warn("Не найден ресурс для пациента с id: {}. Ошибка: {}", patientId, e.getMessage());
            return ResponseEntity.status(404).body(new StatusResponse("Error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new StatusResponse("Error", "File processing error"));
        }
    }

    @Override
    public ResponseEntity<?> deleteAttachment(@RequestParam("url") String url) {
        UUID patientId = getPatientId();
        logger.info("Запрос на удаление вложения по URL: {} для пациента с id: {}", url, patientId);
        try {
            attachmentService.deleteAttachmentByUrl(patientId, url);
            return ResponseEntity.ok(new StatusResponse("Delete", "Attachment deleted successfully"));
        } catch (ResourceNotFoundException e) {
            logger.warn("Вложение не найдено: {}", e.getMessage());
            return ResponseEntity.status(404).body(new StatusResponse("Error", e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("Доступ запрещён: {}", e.getMessage());
            return ResponseEntity.status(403).body(new StatusResponse("Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при удалении вложения", e);
            return ResponseEntity.status(500).body(new StatusResponse("Error", "Internal server error"));
        }
    }

    @Override
    public ResponseEntity<List<DoctorResponse>> getPatientDoctors() {
        UUID patientId = getPatientId();
        logger.info("Получение докторов для пациента с id: {}", patientId);
        List<DoctorResponse> doctors = patientService.getPatientDoctors(patientId);
        logger.debug("Найдено {} докторов для пациента  с id: {}", doctors.size(), patientId);
        return ResponseEntity.ok(doctors);
    }

    @Override
    public ResponseEntity<List<PatientVisitDetailsResponse>> getVisitsByPatient(UUID id) {
        logger.info("Получение списка визитов пациента");
        UUID patientId = (id != null) ? id : getPatientId();
        List<PatientVisitDetailsResponse> visits = patientService.getVisitsByPatient(patientId);
        logger.debug("Список визитов пациента: {}", visits);
        return ResponseEntity.ok(visits);
    }

    @Override
    public ResponseEntity<PatientProfileResponse> getPatientProfile(UUID id) {
        UUID patientId = (id != null) ? id : getPatientId();
        PatientProfileResponse response = patientService.profileById(patientId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> updatePatientProfile(@Valid @RequestBody UpdatePatientProfileRequest updateRequest) {
        UUID patientId = getPatientId();
        patientService.updatePatientProfile(patientId, updateRequest);
        return ResponseEntity.ok(new StatusResponse("Update", "Profile updated successfully"));
    }

    @Override
    public ResponseEntity<StatusResponse> deleteAllPatientData() {
        UUID patientId = getPatientId();
        patientService.deleteAllPatientData(patientId);
        return ResponseEntity.ok(new StatusResponse("delete", "Data deleted succesfully"));
    }
    @Override
    public ResponseEntity<StatusResponse> linkDoctor(@Valid @RequestBody LinkDoctorRequest request) {
        UUID patientId = getPatientId();
        logger.info("Linking patient {} with doctor using code {}", patientId, request.doctorCode());
        try {
            patientService.linkPatientWithDoctor(patientId, request.doctorCode());
            return ResponseEntity.ok(new StatusResponse("Link", "Patient linked with doctor successfully"));
        } catch (ResourceNotFoundException e) {
            logger.warn("Doctor not found with code {}: {}", request.doctorCode(), e.getMessage());
            return ResponseEntity.status(404).body(new StatusResponse("Error", e.getMessage()));
        } catch (AlreadyLinkedException e) {
            logger.warn("Patient already linked: {}", e.getMessage());
            return ResponseEntity.status(409).body(new StatusResponse("Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error linking patient with doctor", e);
            return ResponseEntity.status(500).body(new StatusResponse("Error", "Internal server error"));
        }
    }

    UUID getPatientId() {
        logger.debug("Извлечение идентификатора пациента из JWT");
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID patientId = UUID.fromString(jwt.getClaim("id"));
        logger.debug("Получен идентификатор пациента: {}", patientId);
        return patientId;
    }




}
