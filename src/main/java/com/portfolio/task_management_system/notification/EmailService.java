package com.portfolio.task_management_system.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String dashboardUrl;

    public EmailService(JavaMailSender mailSender,
            @Value("${app.notifications.dashboard-url:http://localhost:8080/dashboard}") String dashboardUrl) {
        this.mailSender = mailSender;
        this.dashboardUrl = dashboardUrl;
    }

    @Async
    public void sendDigestEmail(String to, String name, List<String> messages) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Task Management: Your Digest");
            message.setText(buildDigestBody(name, messages));

            mailSender.send(message);
            log.info("Digest email sent to {}", to);
        } catch (RuntimeException exception) {
            log.error("Failed to send digest email to {}", to, exception);
        }
    }

    private String buildDigestBody(String name, List<String> messages) {
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(name).append(",\n\n");
        body.append("You have ").append(messages.size()).append(" new task updates:\n\n");
        messages.forEach(message -> body.append("- ").append(message).append("\n"));
        body.append("\nView your tasks here: ").append(dashboardUrl);

        return body.toString();
    }
}
