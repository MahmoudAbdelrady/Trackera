package com.mdevs.trackera.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TrackeraMailSender {
    private final JavaMailSender javaMailSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackeraMailSender.class);

    public TrackeraMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendEmail(String email, String subject, String body, boolean isHtml) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("Trackera - " + subject);
            mimeMessageHelper.setText(body, isHtml);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOGGER.error("Error while sending email to: {}", email, e);
            throw new RuntimeException(e);
        }
    }
}
