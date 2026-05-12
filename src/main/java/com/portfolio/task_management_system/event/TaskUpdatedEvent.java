package com.portfolio.task_management_system.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdatedEvent {
    private int version;
    private Long taskId;
    private Long userId;
    private String taskTitle;
    private String updateType;
    private LocalDateTime timestamp;
}
