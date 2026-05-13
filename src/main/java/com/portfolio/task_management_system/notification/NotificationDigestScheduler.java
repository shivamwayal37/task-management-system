package com.portfolio.task_management_system.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationDigestScheduler {

    private final PendingNotificationRepository pendingNotificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationDigestScheduler(PendingNotificationRepository pendingNotificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.pendingNotificationRepository = pendingNotificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRateString = "${app.notifications.digest-rate:5s}")
    @Transactional
    public void processDigest() {
        List<PendingNotification> pendingNotifications =
                pendingNotificationRepository.findByProcessedFalseOrderByCreatedAtAsc();

        if (pendingNotifications.isEmpty()) {
            log.warn("pending notification is empty!");
            return;
        }

        Map<Long, List<PendingNotification>> notificationsByUser = pendingNotifications.stream()
                .collect(Collectors.groupingBy(PendingNotification::getUserId));

        notificationsByUser.forEach(this::sendDigest);

        LocalDateTime processedAt = LocalDateTime.now();
        pendingNotifications.forEach(notification -> {
            notification.setProcessed(true);
            notification.setProcessedAt(processedAt);
        });
        pendingNotificationRepository.saveAll(pendingNotifications);
    }

    private void sendDigest(Long userId, List<PendingNotification> notifications) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Skipping digest for missing user {}", userId);
            return;
        }

        Map<Long, Long> updatesByTask = notifications.stream()
                .collect(Collectors.groupingBy(PendingNotification::getTaskId, Collectors.counting()));

        log.info("Sending notification digest to {} with {} updates across {} tasks",
                user.getEmail(),
                notifications.size(),
                updatesByTask.size());

        List<String> messages = notifications.stream()
                .map(PendingNotification::getMessage)
                .toList();

        emailService.sendDigestEmail(user.getEmail(), user.getName(), messages);
    }
}
