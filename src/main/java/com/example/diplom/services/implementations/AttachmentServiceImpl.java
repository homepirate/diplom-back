package com.example.diplom.services.implementations;

import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.dtos.AttachmentDto;
import io.minio.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final VisitRepository visitRepository;
    private final AttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;
    private final MinioClient minioClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Autowired
    public AttachmentServiceImpl(VisitRepository visitRepository,
                                 AttachmentRepository attachmentRepository,
                                 ModelMapper modelMapper,
                                 MinioClient minioClient, RedisTemplate<String, Object> redisTemplate) {
        this.visitRepository = visitRepository;
        this.attachmentRepository = attachmentRepository;
        this.modelMapper = modelMapper;
        this.minioClient = minioClient;
        this.redisTemplate = redisTemplate;
    }


    private void evictPatientCache(UUID patientId) {
        String cacheName = "patientCache";
        String pattern = cacheName + "::" + patientId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public AttachmentDto addAttachment(UUID patientId, AddAttachmentRequest request) throws IOException {
        Visit visit = visitRepository.findByIdAndPatientId(request.visitId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found for the given patient"));

        String fileName;

        evictPatientCache(patientId);
        try {
            fileName = storeFile(request.file());
        } catch (Exception e) {
            throw new RuntimeException("Error storing file", e);
        }

        Attachment attachment = new Attachment();
        attachment.setVisit(visit);
        attachment.setFilePath(fileName);
        attachment.setDescription(request.description());

        Attachment savedAttachment = attachmentRepository.save(attachment);

        Set<Attachment> visitAttachments = visit.getAttachments();
        visitAttachments.add(savedAttachment);
        visit.setAttachments(visitAttachments);
        visitRepository.save(visit);

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

        ObjectWriteResponse response = minioClient.putObject(
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
                        .expiry(2, TimeUnit.HOURS)
                        .build()
        );
    }

    @Override
    public void deleteAttachmentByUrl(UUID patientId, String url) throws Exception {
        URI uri = new URI(url);
        String path = uri.getPath();
        String expectedPrefix = "/" + bucketName + "/";
        if (!path.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Неверный формат URL");
        }
        String fileKey = path.substring(expectedPrefix.length());

        Attachment attachment = attachmentRepository.findByFilePath(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (!attachment.getVisit().getPatient().getId().equals(patientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Patient not allowed to delete this attachment");
        }

        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .build()
        );

        attachment.getVisit().getAttachments().remove(attachment);
        attachmentRepository.delete(attachment);
    }

    @Transactional
    @Override
    public void deleteAttachmentById(UUID attachmentId) throws Exception {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        String fileKey = attachment.getFilePath();
        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .build()
        );

        if (attachment.getVisit() != null && attachment.getVisit().getAttachments() != null) {
            attachment.getVisit().getAttachments().remove(attachment);
        }
        attachmentRepository.delete(attachment);
    }

    @Override
    public void deleteAllAttachmentsByPatientId(UUID patientId) throws Exception {
        List<Visit> visits = visitRepository.findByPatientId(patientId);
        for (Visit visit : visits) {
            Set<Attachment> attachmentsCopy = Set.copyOf(visit.getAttachments());
            for (Attachment attachment : attachmentsCopy) {
                try {
                    deleteAttachmentById(attachment.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
