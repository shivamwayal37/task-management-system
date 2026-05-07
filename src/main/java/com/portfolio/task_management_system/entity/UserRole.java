package com.portfolio.task_management_system.entity;

public enum UserRole {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
