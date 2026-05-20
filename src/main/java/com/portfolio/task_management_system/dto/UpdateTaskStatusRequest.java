package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Request object for task status transitions")
@Data
public class UpdateTaskStatusRequest {
    @NotBlank(message = "Status is required")
    @Schema(description = "Next task status", example = "IN_PROGRESS",
            allowableValues = {"TODO", "IN_PROGRESS", "COMPLETED"})
    private String status;
}
