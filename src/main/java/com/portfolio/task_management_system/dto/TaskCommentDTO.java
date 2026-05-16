package com.portfolio.task_management_system.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskCommentDTO {
    private Long id;
    private Long taskId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createdAt;
}
