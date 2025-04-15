package com.example.diplom.controllers;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.services.ChatNotificationService;
import com.example.diplom.services.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSTOMPController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatNotificationService chatNotificationService;  // new injection

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // Save the message in your backend (database, etc.)
        chatService.sendMessage(chatMessage);

        // Build a unique conversation id from the two user IDs
        String conversationId = getConversationId(chatMessage.getSenderId(), chatMessage.getReceiverId());

        // Send via WebSocket to conversation-specific and user-specific topics
        messagingTemplate.convertAndSend("/topic/chat." + conversationId, chatMessage);
        messagingTemplate.convertAndSend("/topic/user." + chatMessage.getReceiverId(), chatMessage);
        messagingTemplate.convertAndSend("/topic/user." + chatMessage.getSenderId(), chatMessage);

        // Also, send an FCM chat notification to the receiver
        chatNotificationService.sendChatNotification(chatMessage);
    }

    private String getConversationId(String id1, String id2) {
        return id1.compareTo(id2) <= 0 ? id1 + "-" + id2 : id2 + "-" + id1;
    }
}
