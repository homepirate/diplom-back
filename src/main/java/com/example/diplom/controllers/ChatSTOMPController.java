package com.example.diplom.controllers;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.repositories.ChatMessageRepository;
import com.example.diplom.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatSTOMPController {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatService.sendMessage(chatMessage);
        return chatMessage;
    }
}

