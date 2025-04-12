package com.example.diplom.services;

import com.example.diplom.models.Visit;
import com.example.diplom.repositories.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service("visitNotificationService")
public class NotificationService {

    @Autowired
    private VisitRepository visitRepository; // Репозиторий для работы с визитами

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Для отправки сообщений через WebSocket

    // Метод запускается каждую минуту
    @Scheduled(fixedRate = 60000)
    public void sendNotifications() {
        System.out.println("Запуск отправки уведомлений: " + LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // Для врача: уведомление за 1 час до визита
        LocalDateTime doctorNotificationTime = now.plusHours(1);
        List<Visit> doctorVisits = visitRepository.findVisitsByVisitDateBetween(
                doctorNotificationTime.minusSeconds(30),
                doctorNotificationTime.plusSeconds(30)
        );
        for (Visit visit : doctorVisits) {
            String destination = "/topic/doctor/" + visit.getDoctor().getId();
            String message = "Напоминание: через 1 час назначен визит на " + visit.getVisitDate();
            messagingTemplate.convertAndSend(destination, message);
        }

        // Для пациента: уведомление за 1 день до визита
        LocalDateTime patientNotificationTimeDay = now.plusDays(1);
        List<Visit> patientVisitsDay = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeDay.minusSeconds(30),
                patientNotificationTimeDay.plusSeconds(30)
        );
        for (Visit visit : patientVisitsDay) {
            String destination = "/topic/patient/" + visit.getPatient().getId();
            String message = "Напоминание: через 1 день назначен визит на " + visit.getVisitDate();
            messagingTemplate.convertAndSend(destination, message);
        }

        // Для пациента: уведомление за 1 час до визита
        LocalDateTime patientNotificationTimeHour = now.plusHours(1);
        List<Visit> patientVisitsHour = visitRepository.findVisitsByVisitDateBetween(
                patientNotificationTimeHour.minusSeconds(30),
                patientNotificationTimeHour.plusSeconds(30)
        );
        for (Visit visit : patientVisitsHour) {
            String destination = "/topic/patient/" + visit.getPatient().getId();
            String message = "Напоминание: через 1 час назначен визит на " + visit.getVisitDate();
            messagingTemplate.convertAndSend(destination, message);
        }
    }
}
