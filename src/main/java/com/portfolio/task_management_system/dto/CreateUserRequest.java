package com.portfolio.task_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for creating or updating a user")
@Data
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Schema(description = "Unique username", example = "shivam")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User email address", example = "shivam@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "Pass123!")
    private String password;

    @Schema(description = "User role. Admin-only updates can change this value.", example = "USER",
            allowableValues = {"USER", "ADMIN"})
    private String role;
}
