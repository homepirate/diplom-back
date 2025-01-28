package com.example.diplom.controllers.RR;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record AddAttachmentRequest(

        @NotNull(message = "Visit ID cannot be null")
        UUID visitId,

        @NotNull(message = "File cannot be null")
        MultipartFile file,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description

) {}
