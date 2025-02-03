package com.example.diplom.notif;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender emailSender;

    @Autowired
    public NotificationService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendVisitCreatedNotification(String email, String visitDate) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("gfgfga@rambler.ru"); // TODO надо брать адресс из aplication.ptoperties
        mailMessage.setTo(email);
        mailMessage.setSubject("Ваш визит создан");
        mailMessage.setText("Здравствуйте! Ваш визит назначен на: " + visitDate);

        emailSender.send(mailMessage);
        System.out.println("Сообщение отправлено на " + email);
    }
}
