package com.example.diplom.services;


import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Attachment;
import com.example.diplom.models.Patient;
import com.example.diplom.models.Visit;
import com.example.diplom.repositories.AttachmentRepository;
import com.example.diplom.repositories.VisitRepository;
import com.example.diplom.services.dtos.AttachmentDto;
import com.example.diplom.services.implementations.AttachmentServiceImpl;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private AttachmentServiceImpl service;

    private final String bucket = "bucket";

    @BeforeEach
    void setUp() {
        // Устанавливаем значение приватного поля bucketName
        ReflectionTestUtils.setField(service, "bucketName", bucket);
    }

    @Test
    void addAttachment_whenVisitNotFound_thenThrow() {
        UUID patientId = UUID.randomUUID();
        AddAttachmentRequest req = new AddAttachmentRequest(UUID.randomUUID(), null, "desc");
        when(visitRepository.findByIdAndPatientId(req.visitId(), patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addAttachment(patientId, req));
    }

    @Test
    void addAttachment_success() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Visit visit = new Visit();
        visit.setId(visitId);
        visit.setAttachments(new HashSet<>());

        when(visitRepository.findByIdAndPatientId(visitId, patientId))
                .thenReturn(Optional.of(visit));

        // Готовим запрос
        MultipartFile file = mock(MultipartFile.class);
        AddAttachmentRequest req = new AddAttachmentRequest(visitId, file, "desc");

        // Шпион на сервис, чтобы подменить storeFile
        AttachmentServiceImpl spyService = Mockito.spy(service);
        String fileName = "uuid_file.txt";
        doReturn(fileName).when(spyService).storeFile(file);

        // Сохраняемый attachment
        Attachment saved = new Attachment();
        saved.setId(UUID.randomUUID());
        saved.setVisit(visit);
        saved.setFilePath(fileName);
        saved.setDescription("desc");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(saved);

        // Выполняем
        AttachmentDto dto = spyService.addAttachment(patientId, req);

        // Проверки
        assertEquals(saved.getId(), dto.getId());
        assertEquals(visitId, dto.getVisitId());
        assertEquals(fileName, dto.getFilePath());
        assertEquals("desc", dto.getDescription());

        // Должен быть добавлен в набор визита
        assertTrue(visit.getAttachments().contains(saved));
        verify(visitRepository).save(visit);
    }

    @Test
    void storeFile_whenOriginalFilenameNull_thenThrow() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> service.storeFile(file));
    }

    @Test
    void storeFile_success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        byte[] data = "data".getBytes();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(data));
        when(file.getSize()).thenReturn((long) data.length);
        when(file.getContentType()).thenReturn("text/plain");

        ObjectWriteResponse resp = mock(ObjectWriteResponse.class);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(resp);

        String result = service.storeFile(file);

        assertNotNull(result);
        assertTrue(result.endsWith("_file.txt"));
        // Проверяем, что префикс — валидный UUID
        String prefix = result.substring(0, result.length() - "_file.txt".length());
        assertDoesNotThrow(() -> UUID.fromString(prefix));
    }

    @Test
    void getPresignedUrlForAttachment_whenNotFound_thenThrow() {
        UUID attId = UUID.randomUUID();
        when(attachmentRepository.findById(attId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPresignedUrlForAttachment(attId));
    }

    @Test
    void getPresignedUrlForAttachment_success() throws Exception {
        UUID attId = UUID.randomUUID();
        Attachment attachment = new Attachment();
        attachment.setId(attId);
        attachment.setFilePath("path");

        when(attachmentRepository.findById(attId)).thenReturn(Optional.of(attachment));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("url");

        String url = service.getPresignedUrlForAttachment(attId);

        assertEquals("url", url);
    }

    @Test
    void deleteAttachmentByUrl_whenUrlInvalid_thenThrow() {
        UUID patientId = UUID.randomUUID();
        String badUrl = "http://example.com/invalid";

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteAttachmentByUrl(patientId, badUrl));
    }

    @Test
    void deleteAttachmentByUrl_whenAttachmentNotFound_thenThrow() {
        UUID patientId = UUID.randomUUID();
        String url = "http://host/" + bucket + "/file.txt";

        when(attachmentRepository.findByFilePath("file.txt"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteAttachmentByUrl(patientId, url));
    }

    @Test
    void deleteAttachmentByUrl_whenPatientNotAllowed_thenThrow() {
        UUID patientId = UUID.randomUUID();
        String url = "http://host/" + bucket + "/file.txt";

        Attachment attachment = new Attachment();
        Visit visit = new Visit();
        Patient other = new Patient();
        other.setId(UUID.randomUUID());
        visit.setPatient(other);
        attachment.setVisit(visit);

        when(attachmentRepository.findByFilePath("file.txt"))
                .thenReturn(Optional.of(attachment));

        assertThrows(AccessDeniedException.class,
                () -> service.deleteAttachmentByUrl(patientId, url));
    }

    @Test
    void deleteAttachmentByUrl_success() throws Exception {
        UUID patientId = UUID.randomUUID();
        String url = "http://host/" + bucket + "/file.txt";

        Patient patient = new Patient();
        patient.setId(patientId);
        Visit visit = new Visit();
        visit.setPatient(patient);
        Set<Attachment> set = new HashSet<>();
        Attachment attachment = new Attachment();
        attachment.setVisit(visit);
        attachment.setFilePath("file.txt");
        set.add(attachment);
        visit.setAttachments(set);

        when(attachmentRepository.findByFilePath("file.txt"))
                .thenReturn(Optional.of(attachment));

        service.deleteAttachmentByUrl(patientId, url);

        verify(minioClient).removeObject(any());
        verify(attachmentRepository).delete(attachment);
        assertFalse(visit.getAttachments().contains(attachment));
    }

    @Test
    void deleteAttachmentById_whenNotFound_thenThrow() {
        UUID attId = UUID.randomUUID();
        when(attachmentRepository.findById(attId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteAttachmentById(attId));
    }

    @Test
    void deleteAttachmentById_success() throws Exception {
        UUID attId = UUID.randomUUID();
        Attachment attachment = new Attachment();
        Visit visit = new Visit();
        Set<Attachment> attachments = new HashSet<>();
        attachments.add(attachment);
        visit.setAttachments(attachments);
        attachment.setVisit(visit);
        attachment.setFilePath("fileKey");

        when(attachmentRepository.findById(attId))
                .thenReturn(Optional.of(attachment));

        service.deleteAttachmentById(attId);

        verify(minioClient).removeObject(any());
        verify(attachmentRepository).delete(attachment);
        assertFalse(visit.getAttachments().contains(attachment));
    }

    @Test
    void deleteAllAttachmentsByPatientId_success() throws Exception {
        UUID patientId = UUID.randomUUID();

        Visit emptyVisit = new Visit();
        emptyVisit.setAttachments(new HashSet<>());

        Attachment a2 = new Attachment();
        a2.setId(UUID.randomUUID());
        Visit visitWithAttachment = new Visit();
        Set<Attachment> set2 = new HashSet<>();
        set2.add(a2);
        visitWithAttachment.setAttachments(set2);

        when(visitRepository.findByPatientId(patientId))
                .thenReturn(List.of(emptyVisit, visitWithAttachment));

        // Шпион, чтобы «поймать» вызов deleteAttachmentById
        AttachmentServiceImpl spyService = spy(service);
        doNothing().when(spyService).deleteAttachmentById(a2.getId());

        spyService.deleteAllAttachmentsByPatientId(patientId);

        verify(spyService).deleteAttachmentById(a2.getId());
    }
}
