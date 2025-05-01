package com.example.diplom.services.implementations;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.repositories.ChatMessageRepository;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.services.ChatService;
import com.example.diplom.services.dtos.ChatPreview;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final TextEncryptor textEncryptor;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           @Value("${encryption.password}") String encryptionPassword,
                           @Value("${encryption.salt}") String encryptionSalt, DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.textEncryptor = Encryptors.text(encryptionPassword, encryptionSalt);
    }

    @Override
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setSenderId(chatMessage.getSenderId());
        messageEntity.setReceiverId(chatMessage.getReceiverId());
        String encryptedContent = textEncryptor.encrypt(chatMessage.getContent());
        messageEntity.setContent(encryptedContent);
        // Set the timestamp on the entity
        messageEntity.setTimestamp(LocalDateTime.now().toString());
        chatMessageRepository.save(messageEntity);

        // **Update the ChatMessage object with the timestamp from the entity**
        chatMessage.setTimestamp(messageEntity.getTimestamp());

        return chatMessage;
    }

    @Override
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        // ✅ Fetch messages using the custom sorted query (no need for two separate queries)
        List<ChatMessageEntity> allMessages = chatMessageRepository.findBySenderIdAndReceiverId(user1, user2);

        // Convert entity list to DTO list
        List<ChatMessage> history = new ArrayList<>();
        for (ChatMessageEntity entity : allMessages) {
            ChatMessage message = new ChatMessage();
            message.setSenderId(entity.getSenderId());
            message.setReceiverId(entity.getReceiverId());
            message.setContent(textEncryptor.decrypt(entity.getContent()));
            message.setType(ChatMessage.MessageType.CHAT);
            message.setTimestamp(entity.getTimestamp());
            history.add(message);
        }

        return history;
    }
    @Override
    public void deleteAllMessagesForUser(String userId) {
        // This method deletes all messages in which the user participates.
        chatMessageRepository.deleteBySenderIdOrReceiverId(userId, userId);
    }

    @Override
    public List<ChatPreview> getChatPreviews(String userId) {
        List<ChatMessageEntity> recent = chatMessageRepository.findRecentMessagesByUser(userId);
        Map<String, ChatPreview> previewsMap = new LinkedHashMap<>();

        for (ChatMessageEntity e : recent) {
            String partner = e.getSenderId().equals(userId)
                    ? e.getReceiverId()
                    : e.getSenderId();

            if (!previewsMap.containsKey(partner)) {
                // собираем ID разговора
                String convId = userId.compareTo(partner) <= 0
                        ? userId + "-" + partner
                        : partner + "-" + userId;

                // расшифровываем текст
                String decrypted = textEncryptor.decrypt(e.getContent());

                // достаём имя партнёра (сначала пробуем доктора, иначе пациента)
                String partnerName = doctorRepository.findById(UUID.fromString(partner))
                        .map(Doctor::getFullName)
                        .orElseGet(() ->
                                patientRepository.findById(UUID.fromString(partner))
                                        .map(Patient::getFullName)
                                        .orElse("Unknown"));

                // создаём превью
                ChatPreview preview = new ChatPreview(
                        convId,
                        partner,
                        partnerName,
                        decrypted,
                        e.getTimestamp()
                );
                previewsMap.put(partner, preview);
            }
        }

        return new ArrayList<>(previewsMap.values());
    }
}
