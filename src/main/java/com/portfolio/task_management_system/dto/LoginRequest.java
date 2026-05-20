package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Login request for JWT authentication")
@Data
public class LoginRequest {
    @NotBlank(message = "Name is required")
    @Schema(description = "Username", example = "shivam")
    private String name;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "Pass123!")
    private String password;
}
