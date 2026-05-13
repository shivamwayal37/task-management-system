package com.portfolio.task_management_system.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long> {
    List<PendingNotification> findByProcessedFalseOrderByCreatedAtAsc();
}