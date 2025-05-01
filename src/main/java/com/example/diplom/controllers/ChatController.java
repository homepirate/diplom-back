package com.example.diplom.controllers;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.ChatMessageEntity;
import com.example.diplom.repositories.ChatMessageRepository;
import com.example.diplom.services.ChatService;
import com.example.diplom.services.dtos.ChatPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @RequestParam("user1") String user1,
            @RequestParam("user2") String user2) {
        List<ChatMessage> history = chatService.getChatHistory(user1, user2);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ChatPreview>> getChatList(
            @RequestParam("userId") String userId) {
        List<ChatPreview> previews = chatService.getChatPreviews(userId);
        return ResponseEntity.ok(previews);
    }
}
