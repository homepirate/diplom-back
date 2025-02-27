package com.example.diplom.controllers;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.repositories.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setSenderId(chatMessage.getSenderId());
        messageEntity.setReceiverId(chatMessage.getReceiverId());
        messageEntity.setContent(chatMessage.getContent());
        messageEntity.setTimestamp(LocalDateTime.now().toString());
        chatMessageRepository.save(messageEntity);
        return chatMessage;
    }
}

