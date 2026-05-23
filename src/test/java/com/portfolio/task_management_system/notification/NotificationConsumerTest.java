package com.portfolio.task_management_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portfolio.task_management_system.event.TaskUpdatedEvent;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private PendingNotificationRepository pendingNotificationRepository;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void consumeMapsAndSavesPendingNotification() {
        TaskUpdatedEvent event = TaskUpdatedEvent.builder()
                .version(1)
                .taskId(55L)
                .userId(101L)
                .taskTitle("Implement digest pipeline")
                .updateType("STATUS_CHANGE")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        notificationConsumer.consume(event);

        ArgumentCaptor<PendingNotification> notificationCaptor = ArgumentCaptor.forClass(PendingNotification.class);
        verify(pendingNotificationRepository).save(notificationCaptor.capture());
        PendingNotification saved = notificationCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(101L);
        assertThat(saved.getTaskId()).isEqualTo(55L);
        assertThat(saved.getTaskTitle()).isEqualTo("Implement digest pipeline");
        assertThat(saved.getUpdateType()).isEqualTo("STATUS_CHANGE");
        assertThat(saved.getMessage()).isEqualTo("STATUS_CHANGE: Implement digest pipeline");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
