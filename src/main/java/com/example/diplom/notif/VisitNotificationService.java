package com.example.diplom.notif;

import com.example.diplom.controllers.DoctorController;
import com.example.diplom.models.Visit;
import com.example.diplom.repositories.VisitRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(VisitNotificationService.class);


    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private void sendFcmNotificationTopic(String topic, String title, String body) {
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
            logger.info("FCM ответ: " + response);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке FCM уведомления: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendNotifications() {
        logger.info("Запуск отправки уведомлений: " + LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime doctorNotificationTime = now.plusHours(1);
        List<Visit> doctorVisits = visitRepository.findVisitsByVisitDateBetween(
                doctorNotificationTime.minusSeconds(30),
                doctorNotificationTime.plusSeconds(30)
        );
        logger.info("Found " + doctorVisits.size() + " doctor visits.");
        for (Visit visit : doctorVisits) {
            String formattedDate = visit.getVisitDate().format(formatter);
            String messageText = "Визит: " +
                    visit.getPatient().getFullName() + " на " + formattedDate;
            String topic = "doctor_" + visit.getDoctor().getId();
            logger.info("Sending FCM message to topic: " + topic);
            sendFcmNotificationTopic(topic, "Визит через час", messageText);
            messagingTemplate.convertAndSend("/topic/doctor/" + visit.getDoctor().getId(), messageText);
        }

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
