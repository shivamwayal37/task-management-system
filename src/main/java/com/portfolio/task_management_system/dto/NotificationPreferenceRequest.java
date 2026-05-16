package com.portfolio.task_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationPreferenceRequest {
    @NotBlank(message = "Notification mode is required")
    private String mode;
}
