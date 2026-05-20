package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request object for creating or updating a task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Schema(description = "Task title", example = "Implement Kafka notifications")
    private String title;

    @Schema(description = "Detailed task description", example = "Send async notification when task status changes")
    private String description;

    @NotBlank(message = "Status is required")
    @Schema(description = "Task status", example = "TODO", allowableValues = {"TODO", "IN_PROGRESS", "COMPLETED"})
    private String status;
}
