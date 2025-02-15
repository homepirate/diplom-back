package com.example.diplom.services.implementations;

import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.dtos.AttachmentDto;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Attachment;
import com.example.diplom.models.Visit;
import com.example.diplom.repositories.AttachmentRepository;
import com.example.diplom.repositories.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final VisitRepository visitRepository;
    private final AttachmentRepository attachmentRepository;
    private final Path fileStorageLocation;
    private final ModelMapper modelMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Autowired
    public AttachmentServiceImpl(VisitRepository visitRepository,
                                 AttachmentRepository attachmentRepository, ModelMapper modelMapper, MinioClient minioClient) throws IOException {
        this.visitRepository = visitRepository;
        this.attachmentRepository = attachmentRepository;
        this.modelMapper = modelMapper;
        this.minioClient = minioClient;
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    @Override
    public AttachmentDto addAttachment(UUID patientId, AddAttachmentRequest request) throws IOException {
        // Verify that the visit exists and belongs to the patient
        Visit visit = visitRepository.findByIdAndPatientId(request.visitId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found for the given patient"));

        String fileName;
        try {
            fileName = storeFile(request.file());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create and save the attachment
        Attachment attachment = new Attachment();
        attachment.setVisit(visit);
        attachment.setFilePath(fileName);
        attachment.setDescription(request.description());
        Attachment savedAttachment = attachmentRepository.save(attachment);

        return new AttachmentDto(savedAttachment.getId(), savedAttachment.getVisit().getId(), savedAttachment.getFilePath(), savedAttachment.getDescription());
    }

    @Override
    public String storeFile(MultipartFile file) throws Exception {
        // Normalize the file name
        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String fileName = UUID.randomUUID() + "_" + originalFileName;

        // Upload file to MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return fileName;
    }

    @Override
    public String getPresignedUrlForAttachment(UUID attachmentId) throws Exception {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(attachment.getFilePath())
                        .expiry(2, TimeUnit.HOURS) // URL valid for 2 hours
                        .build()
        );
    }
}
