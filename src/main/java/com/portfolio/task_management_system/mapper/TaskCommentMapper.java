package com.portfolio.task_management_system.mapper;

import com.portfolio.task_management_system.dto.TaskCommentDTO;
import com.portfolio.task_management_system.entity.TaskComment;

public class TaskCommentMapper {

    private TaskCommentMapper() {
    }

    public static TaskCommentDTO toDTO(TaskComment comment) {
        return new TaskCommentDTO(
                comment.getId(),
                comment.getTask().getId(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
