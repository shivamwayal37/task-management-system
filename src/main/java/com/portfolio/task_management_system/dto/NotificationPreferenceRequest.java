package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Request object for updating notification preference")
@Data
public class NotificationPreferenceRequest {
    @NotBlank(message = "Notification mode is required")
    @Schema(description = "Notification delivery mode", example = "HOURLY_DIGEST",
            allowableValues = {"REAL_TIME", "HOURLY_DIGEST", "DAILY_DIGEST"})
    private String mode;
}
