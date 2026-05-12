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

    public NotificationDigestScheduler(PendingNotificationRepository pendingNotificationRepository,
            UserRepository userRepository) {
        this.pendingNotificationRepository = pendingNotificationRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRateString = "${app.notifications.digest-rate:1h}")
    @Transactional
    public void processDigest() {
        List<PendingNotification> pendingNotifications =
                pendingNotificationRepository.findByProcessedFalseOrderByCreatedAtAsc();

        if (pendingNotifications.isEmpty()) {
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
    }

    private void sendDigest(Long userId, List<PendingNotification> notifications) {
        User user = userRepository.findById(userId).orElse(null);
        String recipient = user == null ? "unknown-user-%d".formatted(userId) : user.getEmail();

        Map<Long, Long> updatesByTask = notifications.stream()
                .collect(Collectors.groupingBy(PendingNotification::getTaskId, Collectors.counting()));

        log.info("Sending notification digest to {} with {} updates across {} tasks",
                recipient,
                notifications.size(),
                updatesByTask.size());

        updatesByTask.forEach((taskId, count) ->
                log.info("Digest item recipient={} taskId={} updates={}", recipient, taskId, count));
    }
}
