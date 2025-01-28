package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.controllers.RR.PatientResponse;
import com.example.diplom.controllers.interfaces.PatientAPI;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.exceptions.StatusResponse;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.dtos.AttachmentDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
public class PatientController implements PatientAPI {

    private final AttachmentService attachmentService;

    @Autowired
    public PatientController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    public PatientResponse getAllPatients(int page) {
        return null;
    }

    @Override
    public ResponseEntity<?> AddAttachment(@Valid @ModelAttribute AddAttachmentRequest addAttachmentRequest) {
        UUID patientId = getPatientId();
        try {
            AttachmentDto attachment = attachmentService.addAttachment(patientId, addAttachmentRequest);

            return ResponseEntity.ok(new StatusResponse("Add", "Attachment added"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new StatusResponse("Error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new StatusResponse("Error","File processing error"));
        }
    }

    UUID getPatientId(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID patientId = UUID.fromString(jwt.getClaim("id"));
        return patientId;
    }
}
