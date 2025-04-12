package com.example.diplom.notif;

import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationMailService {

    private final JavaMailSender emailSender;

    @Autowired
    public NotificationMailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    public void sendVisitCreatedNotification(String email, String visitDate) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom("gfgfga@rambler.ru"); // TODO: Get from properties
            mailMessage.setTo(email);
            mailMessage.setSubject("Ваш визит создан");
            mailMessage.setText("Здравствуйте! Ваш визит назначен на: " + visitDate);

            emailSender.send(mailMessage);
            System.out.println("Сообщение отправлено на " + email);
        } catch (Exception e) {
            System.err.println("Ошибка отправки email: " + e.getMessage());
        }
    }
}
