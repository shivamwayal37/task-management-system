package com.portfolio.task_management_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationDigestSchedulerTest {

    @Mock
    private PendingNotificationRepository pendingNotificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationDigestScheduler notificationDigestScheduler;

    @Test
    void processDigestSkipsWhenNoPendingNotifications() {
        when(pendingNotificationRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        notificationDigestScheduler.processDigest();

        verify(pendingNotificationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    void processDigestSendsEmailAndMarksNotificationsProcessed() {
        PendingNotification first = pendingNotification(1L, 7L, "STATUS_CHANGE: Task A");
        PendingNotification second = pendingNotification(1L, 8L, "COMMENT_ADDED: Task B");

        User user = new User();
        user.setId(1L);
        user.setName("Shivam");
        user.setEmail("shivam@example.com");

        when(pendingNotificationRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationDigestScheduler.processDigest();

        verify(emailService).sendDigestEmail(
                "shivam@example.com",
                "Shivam",
                List.of("STATUS_CHANGE: Task A", "COMMENT_ADDED: Task B"));

        ArgumentCaptor<List<PendingNotification>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(pendingNotificationRepository).saveAll(listCaptor.capture());
        List<PendingNotification> saved = listCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(PendingNotification::isProcessed);
        assertThat(saved).allMatch(item -> item.getProcessedAt() != null);
    }

    @Test
    void processDigestSkipsMissingUserButStillMarksProcessed() {
        PendingNotification pending = pendingNotification(99L, 12L, "STATUS_CHANGE: Missing User Task");
        when(pendingNotificationRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(pending));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        notificationDigestScheduler.processDigest();

        verify(emailService, never()).sendDigestEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList());
        verify(pendingNotificationRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
        assertThat(pending.isProcessed()).isTrue();
        assertThat(pending.getProcessedAt()).isNotNull();
    }

    private PendingNotification pendingNotification(Long userId, Long taskId, String message) {
        return PendingNotification.builder()
                .id(taskId)
                .userId(userId)
                .taskId(taskId)
                .taskTitle("Task " + taskId)
                .updateType("STATUS_CHANGE")
                .message(message)
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build();
    }
}
