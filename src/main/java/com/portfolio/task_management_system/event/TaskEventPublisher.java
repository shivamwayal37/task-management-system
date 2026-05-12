package com.portfolio.task_management_system.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskEventPublisher {

    private final String taskEventsTopic;
    private final KafkaTemplate<String, TaskUpdatedEvent> kafkaTemplate;

    // Use constructor injection for both the template and the value
    public TaskEventPublisher(
            KafkaTemplate<String, TaskUpdatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.task-events:task-events}") String taskEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskEventsTopic = taskEventsTopic;
    }

    public void publishAfterCommit(TaskUpdatedEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
            return;
        }
        publish(event);
    }

    private void publish(TaskUpdatedEvent event) {
        try {
            log.info("Sending task event {}", event);
            kafkaTemplate.send(taskEventsTopic, String.valueOf(event.getTaskId()), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish task event type={} taskId={} userId={}",
                                    event.getUpdateType(),
                                    event.getTaskId(),
                                    event.getUserId(),
                                    exception);
                            return;
                        }

                        log.info("Published task event type={} taskId={} userId={}",
                                event.getUpdateType(),
                                event.getTaskId(),
                                event.getUserId());
                    });
        } catch (RuntimeException exception) {
            log.error("Failed to queue task event type={} taskId={} userId={}",
                    event.getUpdateType(),
                    event.getTaskId(),
                    event.getUserId(),
                    exception);
        }
    }
}
