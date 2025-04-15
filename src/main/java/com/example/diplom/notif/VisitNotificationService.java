package com.example.diplom.notif;

import com.example.diplom.models.Visit;
import com.example.diplom.repositories.VisitRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service("VisitNotificationService")
public class VisitNotificationService {

    @Autowired
    private VisitRepository visitRepository;

    // WebSocket messaging for real‑time UI updates:
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Formatter for the visit date (customize as needed)
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Helper method to send a data‑only FCM message to a topic
    private void sendFcmNotificationTopic(String topic, String title, String body) {
        // Create a message with only data payload (no notification payload)
        Message message = Message.builder()
                .setTopic(topic)
                .putData("title", title)
                .putData("message", body)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("FCM ответ: " + response);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке FCM уведомления: " + e.getMessage());
        }
    }

    // Scheduled method runs every minute to check for upcoming visits
    @Scheduled(fixedRate = 60000)
    public void sendNotifications() {
        System.out.println("Запуск отправки уведомлений: " + LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        // Notifications for Doctors (1 hour before visit)
        LocalDateTime doctorNotificationTime = now.plusHours(1);
        List<Visit> doctorVisits = visitRepository.findVisitsByVisitDateBetween(
                doctorNotificationTime.minusSeconds(30),
                doctorNotificationTime.plusSeconds(30)
        );
        System.out.println("Found " + doctorVisits.size() + " doctor visits.");
        for (Visit visit : doctorVisits) {
            String formattedDate = visit.getVisitDate().format(formatter);
            // Create message text that includes the patient's name
            String messageText = "Визит: " +
                    visit.getPatient().getFullName() + " на " + formattedDate;
            String topic = "doctor_" + visit.getDoctor().getId();
            System.out.println("Sending FCM message to topic: " + topic);
            sendFcmNotificationTopic(topic, "Визит через час", messageText);
            messagingTemplate.convertAndSend("/topic/doctor/" + visit.getDoctor().getId(), messageText);
        }

        // Notifications for Patients (1 day before visit)
        LocalDateTime patientNotificationTimeDay = now.plusDays(1);
        List<Visit> patientVisitsDay = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeDay.minusSeconds(30),
                patientNotificationTimeDay.plusSeconds(30)
        );
        for (Visit visit : patientVisitsDay) {
            String formattedDate = visit.getVisitDate().format(formatter);
            String messageText = "Визит: " +
                    visit.getDoctor().getFullName() + " на " + formattedDate;
            String topic = "patient_" + visit.getPatient().getId();
            sendFcmNotificationTopic(topic, "Визит через день", messageText);
            messagingTemplate.convertAndSend("/topic/patient/" + visit.getPatient().getId(), messageText);
        }

        // Notifications for Patients (1 hour before visit)
        LocalDateTime patientNotificationTimeHour = now.plusHours(1);
        List<Visit> patientVisitsHour = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeHour.minusSeconds(30),
                patientNotificationTimeHour.plusSeconds(30)
        );
        for (Visit visit : patientVisitsHour) {
            String formattedDate = visit.getVisitDate().format(formatter);
            String messageText = "Визит: " +
                    visit.getDoctor().getFullName() + " на " + formattedDate;
            String topic = "patient_" + visit.getPatient().getId();
            sendFcmNotificationTopic(topic, "Визит через час", messageText);
            messagingTemplate.convertAndSend("/topic/patient/" + visit.getPatient().getId(), messageText);
        }
    }
}
