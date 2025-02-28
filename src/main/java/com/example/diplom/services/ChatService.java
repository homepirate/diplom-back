package com.example.diplom.services;

import com.example.diplom.models.ChatMessage;
import java.util.List;

public interface ChatService {
    List<ChatMessage> getChatHistory(String user1, String user2);
}
