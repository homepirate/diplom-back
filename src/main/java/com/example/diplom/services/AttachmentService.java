package com.example.diplom.services;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.services.dtos.AttachmentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface AttachmentService {

    AttachmentDto addAttachment(UUID patientId, AddAttachmentRequest request) throws IOException;
    String storeFile(MultipartFile file) throws IOException;
}
