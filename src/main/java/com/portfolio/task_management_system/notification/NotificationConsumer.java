package com.portfolio.task_management_system.notification;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.event.TaskUpdatedEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationConsumer {

    private final PendingNotificationRepository pendingNotificationRepository;

    public NotificationConsumer(PendingNotificationRepository pendingNotificationRepository) {
        this.pendingNotificationRepository = pendingNotificationRepository;
    }

    @KafkaListener(topics = "${app.kafka.topics.task-events:task-events}")
    public void consume(TaskUpdatedEvent event) {
        log.info("Received event {}", event);
        PendingNotification notification = PendingNotification.builder()
                .userId(event.getUserId())
                .taskId(event.getTaskId())
                .taskTitle(event.getTaskTitle())
                .updateType(event.getUpdateType())
                .message("%s: %s".formatted(event.getUpdateType(), event.getTaskTitle()))
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build();

        pendingNotificationRepository.save(notification);
        log.info("Saving notification");
        log.info("Stored pending notification type={} taskId={} userId={}",
                event.getUpdateType(),
                event.getTaskId(),
                event.getUserId());
    }
}
