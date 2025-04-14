package com.example.diplom.notif;



import com.example.diplom.models.Visit;
import com.example.diplom.repositories.VisitRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service("FCMVisitNotificationService")
public class FCMVisitNotificationService {

    @Autowired
    private VisitRepository visitRepository;

    // Логика отправки для веб-клиентов по WebSocket остаётся без изменений:
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Метод, который отправляет уведомление через FCM по теме
    private void sendFcmNotificationTopic(String topic, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setTopic(topic)  // Используем тему вместо токена
                .setNotification(notification)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("FCM ответ: " + response);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке FCM уведомления: " + e.getMessage());
        }
    }

    // Метод запускается каждую минуту
    @Scheduled(fixedRate = 60000)
    public void sendNotifications() {
        System.out.println("Запуск отправки уведомлений: " + LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        // Уведомления для врача: за 1 час до визита
        LocalDateTime doctorNotificationTime = now.plusHours(1);
        List<Visit> doctorVisits = visitRepository.findVisitsByVisitDateBetween(
                doctorNotificationTime.minusSeconds(30),
                doctorNotificationTime.plusSeconds(30)
        );
        System.out.println("Found " + doctorVisits.size() + " doctor visits.");
        for (Visit visit : doctorVisits) {
            String messageText = "Напоминание: через 1 час назначен визит на " + visit.getVisitDate();
            String topic = "doctor_" + visit.getDoctor().getId();
            System.out.println("Sending FCM message to topic: " + topic);
            sendFcmNotificationTopic(topic, "Напоминание о визите", messageText);

            // Additionally, send via WebSocket:
            messagingTemplate.convertAndSend("/topic/doctor/" + visit.getDoctor().getId(), messageText);
        }

        // Для пациента: уведомление за 1 день до визита
        LocalDateTime patientNotificationTimeDay = now.plusDays(1);
        List<Visit> patientVisitsDay = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeDay.minusSeconds(30),
                patientNotificationTimeDay.plusSeconds(30)
        );
        for (Visit visit : patientVisitsDay) {
            String messageText = "Напоминание: через 1 день назначен визит на " + visit.getVisitDate();
            String topic = "patient_" + visit.getPatient().getId();
            sendFcmNotificationTopic(topic, "Напоминание о визите", messageText);
            messagingTemplate.convertAndSend("/topic/patient/" + visit.getPatient().getId(), messageText);
        }

        // Для пациента: уведомление за 1 час до визита
        LocalDateTime patientNotificationTimeHour = now.plusHours(1);
        List<Visit> patientVisitsHour = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeHour.minusSeconds(30),
                patientNotificationTimeHour.plusSeconds(30)
        );
        for (Visit visit : patientVisitsHour) {
            String messageText = "Напоминание: через 1 час назначен визит на " + visit.getVisitDate();
            String topic = "patient_" + visit.getPatient().getId();
            sendFcmNotificationTopic(topic, "Напоминание о визите", messageText);
            messagingTemplate.convertAndSend("/topic/patient/" + visit.getPatient().getId(), messageText);
        }
    }
}
