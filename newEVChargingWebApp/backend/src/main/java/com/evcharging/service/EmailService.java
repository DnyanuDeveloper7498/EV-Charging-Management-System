package com.evcharging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text e-mails. SMTP is optional: when SPRING_MAIL_HOST is not set there is no JavaMailSender bean and
 * the mail is only logged (the body only when MAIL_DEV_LOG=true, because it may contain a password-reset link).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean devLog;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.from:no-reply@smartev.local}") String from,
                        @Value("${app.mail.dev-log:false}") boolean devLog) {
        this.mailSender = mailSender;
        this.from = from;
        this.devLog = devLog;
    }

    /** Never throws: a failing mail server must not break booking or password-reset requests. */
    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            if (devLog) {
                log.info("[MAIL not configured] To: {} | Subject: {}\n{}", to, subject, body);
            } else {
                log.info("[MAIL not configured] Not sent to {} (subject: {}). Set SPRING_MAIL_HOST to enable e-mail.",
                        to, subject);
            }
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            log.error("Could not send e-mail to {}: {}", to, e.getMessage());
        }
    }
}
