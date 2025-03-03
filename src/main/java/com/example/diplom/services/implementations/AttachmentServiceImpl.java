package com.example.diplom.services.implementations;

import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.dtos.AttachmentDto;
import io.minio.*;
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
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final VisitRepository visitRepository;
    private final AttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Autowired
    public AttachmentServiceImpl(VisitRepository visitRepository,
                                 AttachmentRepository attachmentRepository,
                                 ModelMapper modelMapper,
                                 MinioClient minioClient) {
        this.visitRepository = visitRepository;
        this.attachmentRepository = attachmentRepository;
        this.modelMapper = modelMapper;
        this.minioClient = minioClient;
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
            throw new RuntimeException("Error storing file", e);
        }

        // Create a new attachment
        Attachment attachment = new Attachment();
        attachment.setVisit(visit);
        attachment.setFilePath(fileName);
        attachment.setDescription(request.description());

        // Save the new attachment
        Attachment savedAttachment = attachmentRepository.save(attachment);

        // Ensure the visit correctly tracks multiple attachments
        Set<Attachment> visitAttachments = visit.getAttachments();
        visitAttachments.add(savedAttachment);
        visit.setAttachments(visitAttachments);
        visitRepository.save(visit); // Update visit with the new attachment

        return new AttachmentDto(savedAttachment.getId(),
                savedAttachment.getVisit().getId(),
                savedAttachment.getFilePath(),
                savedAttachment.getDescription());
    }

    public String storeFile(MultipartFile file) throws Exception {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("Invalid file name");
        }

        String fileName = UUID.randomUUID() + "_" + originalFileName;

        // Шифрование при загрузке
        ObjectWriteResponse response = minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .sse(new ServerSideEncryptionS3()) // Правильный метод для шифрования
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
    @Override
    public void deleteAttachmentByUrl(UUID patientId, String url) throws Exception {
        // Parse the URL to extract the file key
        URI uri = new URI(url);
        String path = uri.getPath(); // expected format: /bucketName/fileName
        String expectedPrefix = "/" + bucketName + "/";
        if (!path.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Неверный формат URL");
        }
        String fileKey = path.substring(expectedPrefix.length());

        // Find the attachment record by its file path (fileKey)
        Attachment attachment = attachmentRepository.findByFilePath(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Verify that the attachment belongs to the patient
        if (!attachment.getVisit().getPatient().getId().equals(patientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Patient not allowed to delete this attachment");
        }

        // Remove the file from MinIO
        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .build()
        );

        // Remove the attachment from the visit and delete it from the repository
        attachment.getVisit().getAttachments().remove(attachment);
        attachmentRepository.delete(attachment);
    }

}
