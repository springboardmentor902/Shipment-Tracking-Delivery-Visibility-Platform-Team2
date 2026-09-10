package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.service.NotificationEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sends the current notification through the configured Spring mail client. */
@Service
@RequiredArgsConstructor
public class JavaMailNotificationEmailSender implements NotificationEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@shiptrack.local}")
    private String fromAddress;

    @Override
    public void send(Notification notification) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromAddress);
        email.setTo(notification.getUser().getEmail());
        email.setSubject(notification.getTitle());
        email.setText(notification.getMessage());
        mailSender.send(email);
    }
}
