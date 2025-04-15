package com.example.diplom.notif;

import com.example.diplom.models.ChatMessage;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service("ChatNotificationService")
public class ChatNotificationService {

    /**
     * Sends a data-only FCM message for chat notifications.
     * The message includes:
     * - notificationType: "chat"
     * - title: a custom title (e.g. "Новое сообщение")
     * - message: the chat message text
     * - senderId: who sent the message
     * - conversationId: unique conversation identifier
     */




    public void sendChatNotification(ChatMessage chatMessage) {
        // Build a unique conversation id from the two user IDs.
        String conversationId = getConversationId(chatMessage.getSenderId(), chatMessage.getReceiverId());
        // The recipient will subscribe to the "user_{id}" topic.
        String topic = "user_" + chatMessage.getReceiverId();

        Message message = Message.builder()
                .setTopic(topic)
                .putData("notificationType", "chat")
                .putData("title", "Новое сообщение")
                .putData("message", chatMessage.getContent())
                .putData("senderId", chatMessage.getSenderId())
             //   .putData("senderName", ) // Add this extra!
                .putData("conversationId", conversationId)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("FCM chat notification ответ: " + response);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке FCM чат уведомления: " + e.getMessage());
        }
    }

    private String getConversationId(String id1, String id2) {
        return id1.compareTo(id2) <= 0 ? id1 + "-" + id2 : id2 + "-" + id1;
    }
}
