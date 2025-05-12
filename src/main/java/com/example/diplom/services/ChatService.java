package com.example.diplom.services;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.services.dtos.ChatPreview;

import java.util.List;

public interface ChatService {
    List<ChatMessage> getChatHistory(String user1, String user2);

    ChatMessage sendMessage(ChatMessage chatMessage);

    void deleteAllMessagesForUser(String userId);

    List<ChatPreview> getChatPreviews(String userId);
}
