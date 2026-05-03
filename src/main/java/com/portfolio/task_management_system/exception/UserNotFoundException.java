package com.portfolio.task_management_system.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }

    public UserNotFoundException(String name) {
        super("User not found with name: " + name);
    }
}
