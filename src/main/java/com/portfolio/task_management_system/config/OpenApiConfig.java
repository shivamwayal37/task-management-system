package com.portfolio.task_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskManagementOpenAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Task Management System API")
                        .description("""
                                Enterprise-style task management backend built using Spring Boot.

                                Features:
                                - JWT Authentication
                                - Role-Based Access Control
                                - Task Assignment and Status Workflows
                                - Task Comments
                                - Audit Logging
                                - Soft Delete and Restore
                                - Redis Caching
                                - Kafka Notification Digest Pipeline
                                - Pagination and Search
                                """)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Shivam Wayal")
                                .email("wayalshivam7@gmail.com"))
                        .license(new License().name("MIT License")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/shivamwayal37/task-management-system"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
