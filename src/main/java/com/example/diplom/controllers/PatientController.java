package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.controllers.RR.PatientResponse;
import com.example.diplom.controllers.RR.PatientVisitDetailsResponse;
import com.example.diplom.controllers.interfaces.PatientAPI;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.exceptions.StatusResponse;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.AttachmentDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    public ResponseEntity<List<PatientVisitDetailsResponse>> getVisitsByPatient() {
        logger.info("Получение списка визитов пациента");
        UUID patientId = getPatientId();
        List<PatientVisitDetailsResponse> visits = patientService.getVisitsByPatient(patientId);
        logger.debug("Список визитов пациента: {}", visits);
        return ResponseEntity.ok(visits);
    }

    UUID getPatientId() {
        logger.debug("Извлечение идентификатора пациента из JWT");
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID patientId = UUID.fromString(jwt.getClaim("id"));
        logger.debug("Получен идентификатор пациента: {}", patientId);
        return patientId;
    }
}
