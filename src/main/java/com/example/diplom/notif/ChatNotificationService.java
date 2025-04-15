package com.example.diplom.notif;

import com.example.diplom.models.ChatMessage;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.repositories.UserRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("ChatNotificationService")
public class ChatNotificationService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    public void sendChatNotification(ChatMessage chatMessage) {
        String conversationId = getConversationId(chatMessage.getSenderId(), chatMessage.getReceiverId());
        String topic = "user_" + chatMessage.getReceiverId();

        String senderName = getSenderFullName(chatMessage.getSenderId());
        if (senderName == null) {
            senderName = "Unknown";
        }

        Message message = Message.builder()
                .setTopic(topic)
                .putData("notificationType", "chat")
                .putData("title", senderName)
                .putData("message", chatMessage.getContent())
                .putData("senderId", chatMessage.getSenderId())
                .putData("senderName", senderName)
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

    private String getSenderFullName(String senderId) {

        Optional<Doctor> doctor = doctorRepository.findById(UUID.fromString(senderId));
        if (doctor.isPresent()) {
            return doctor.get().getFullName();
        }

        Optional<Patient> patient = patientRepository.findById(UUID.fromString(senderId));
        if (patient.isPresent()) {
            return patient.get().getFullName();
        }
        return null;
    }

    private String getConversationId(String id1, String id2) {
        return id1.compareTo(id2) <= 0 ? id1 + "-" + id2 : id2 + "-" + id1;
    }
}