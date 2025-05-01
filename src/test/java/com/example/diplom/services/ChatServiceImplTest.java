package com.example.diplom.services;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.repositories.ChatMessageRepository;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import com.example.diplom.services.implementations.ChatServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceImplTest {

    private ChatMessageRepository repo;
    private ChatServiceImpl service;
    private TextEncryptor textEncryptor;

    private  DoctorRepository doctorRepository;
    private PatientRepository patientRepository;

    private final String PASSWORD = "testPass";
    private final String SALT     = "1234";

    @BeforeEach
    void setUp() {
        repo = mock(ChatMessageRepository.class);
        service = new ChatServiceImpl(repo, PASSWORD, SALT, doctorRepository, patientRepository);
        textEncryptor = Encryptors.text(PASSWORD, SALT);
    }

    @Test
    void sendMessage_persistsEncryptedAndReturnsWithTimestamp() {
        // Подготовка
        ChatMessage in = new ChatMessage();
        in.setSenderId("userA");
        in.setReceiverId("userB");
        in.setContent("Hello, world!");

        // Мокаем save так, чтобы возвращался тот же entity
        when(repo.save(any(ChatMessageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Выполняем
        ChatMessage out = service.sendMessage(in);

        // Ловим сохранённый entity
        ArgumentCaptor<ChatMessageEntity> captor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(repo, times(1)).save(captor.capture());
        ChatMessageEntity saved = captor.getValue();

        // Проверяем, что sender/receiver правильно проставлены
        assertEquals("userA", saved.getSenderId());
        assertEquals("userB", saved.getReceiverId());

        // Декодируем содержимое и сравниваем
        String decrypted = textEncryptor.decrypt(saved.getContent());
        assertEquals("Hello, world!", decrypted);

        // Timestamp установился в entity и прокинулся в DTO
        assertNotNull(saved.getTimestamp());
        assertEquals(saved.getTimestamp(), out.getTimestamp());
    }

    @Test
    void getChatHistory_emptyRepository_returnsEmptyList() {
        when(repo.findBySenderIdAndReceiverId("u1", "u2"))
                .thenReturn(Collections.emptyList());

        List<ChatMessage> history = service.getChatHistory("u1", "u2");
        assertTrue(history.isEmpty());
    }

    @Test
    void getChatHistory_decryptsAllMessagesInOrder() {
        // Подготовка одного entity
        ChatMessageEntity e = new ChatMessageEntity();
        e.setSenderId("u1");
        e.setReceiverId("u2");
        e.setTimestamp("2025-04-24T10:15:30");
        e.setContent(textEncryptor.encrypt("Secret"));

        when(repo.findBySenderIdAndReceiverId("u1", "u2"))
                .thenReturn(List.of(e));

        // Выполняем
        List<ChatMessage> history = service.getChatHistory("u1", "u2");

        assertEquals(1, history.size());
        ChatMessage m = history.get(0);
        assertEquals("u1", m.getSenderId());
        assertEquals("u2", m.getReceiverId());
        assertEquals("Secret", m.getContent());
        assertEquals("2025-04-24T10:15:30", m.getTimestamp());
        assertEquals(ChatMessage.MessageType.CHAT, m.getType());
    }

    @Test
    void deleteAllMessagesForUser_invokesRepository() {
        service.deleteAllMessagesForUser("someUser");
        verify(repo, times(1))
                .deleteBySenderIdOrReceiverId("someUser", "someUser");
    }
}
