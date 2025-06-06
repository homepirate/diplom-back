package com.example.diplom.controllers;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.notif.ChatNotificationService;
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
    private ChatNotificationService chatNotificationService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatService.sendMessage(chatMessage);
        String conversationId = getConversationId(chatMessage.getSenderId(), chatMessage.getReceiverId());
        messagingTemplate.convertAndSend("/topic/chat." + conversationId, chatMessage);
        messagingTemplate.convertAndSend("/topic/user." + chatMessage.getReceiverId(), chatMessage);
        messagingTemplate.convertAndSend("/topic/user." + chatMessage.getSenderId(), chatMessage);
        chatNotificationService.sendChatNotification(chatMessage);
    }

    private String getConversationId(String id1, String id2) {
        return id1.compareTo(id2) <= 0 ? id1 + "-" + id2 : id2 + "-" + id1;
    }
}
