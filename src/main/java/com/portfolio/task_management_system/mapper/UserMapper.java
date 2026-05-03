package com.portfolio.task_management_system.mapper;

import com.portfolio.task_management_system.dto.CreateUserRequest;
import com.portfolio.task_management_system.dto.UserDTO;
import com.portfolio.task_management_system.entity.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    public static User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        return user;
    }
}
