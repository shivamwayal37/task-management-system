package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Request object for adding a task comment")
@Data
public class CreateCommentRequest {
    @NotBlank(message = "Comment content is required")
    @Schema(description = "Comment text", example = "I started working on this task.")
    private String content;
}
