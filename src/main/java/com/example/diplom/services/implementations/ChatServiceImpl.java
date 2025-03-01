package com.example.diplom.services.implementations;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.repositories.ChatMessageRepository;
import com.example.diplom.services.ChatService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final TextEncryptor textEncryptor;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           @Value("${encryption.password}") String encryptionPassword,
                           @Value("${encryption.salt}") String encryptionSalt) {
        this.chatMessageRepository = chatMessageRepository;
        this.textEncryptor = Encryptors.text(encryptionPassword, encryptionSalt);
    }

    @Override
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setSenderId(chatMessage.getSenderId());
        messageEntity.setReceiverId(chatMessage.getReceiverId());
        String encryptedContent = textEncryptor.encrypt(chatMessage.getContent());
        messageEntity.setContent(encryptedContent);
        messageEntity.setTimestamp(LocalDateTime.now().toString());
        chatMessageRepository.save(messageEntity);
        return chatMessage;
    }

    @Override
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        // Fetch messages in both directions
        List<ChatMessageEntity> messagesFromUser1 = chatMessageRepository.findBySenderIdAndReceiverId(user1, user2);
        List<ChatMessageEntity> messagesFromUser2 = chatMessageRepository.findBySenderIdAndReceiverId(user2, user1);

        List<ChatMessageEntity> allMessages = new ArrayList<>();
        allMessages.addAll(messagesFromUser1);
        allMessages.addAll(messagesFromUser2);

        // Sort messages by timestamp (assumes ISO 8601 format)
        Collections.sort(allMessages, Comparator.comparing(ChatMessageEntity::getTimestamp));

        // Convert entity list to DTO list
        List<ChatMessage> history = new ArrayList<>();
        for (ChatMessageEntity entity : allMessages) {
            ChatMessage message = new ChatMessage();
            message.setSenderId(entity.getSenderId());
            message.setReceiverId(entity.getReceiverId());
            String decryptedContent = textEncryptor.decrypt(entity.getContent());
            message.setContent(decryptedContent);
            message.setType(ChatMessage.MessageType.CHAT);
            // If ChatMessage has a timestamp field, set it here:
            // message.setTimestamp(entity.getTimestamp());
            history.add(message);
        }
        return history;
    }
}
