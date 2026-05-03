package com.portfolio.task_management_system.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.dto.CreateUserRequest;
import com.portfolio.task_management_system.dto.UserDTO;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.UserMapper;
import com.portfolio.task_management_system.repository.UserRepository;

@Service
public class UserService{
    
    @Autowired
    private UserRepository userRepository;

    public UserDTO createUser(CreateUserRequest request){
        User user = UserMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    public UserDTO getUserById(Long id) {
        User user = getUserEntityById(id);
        return UserMapper.toDTO(user);
    }

    public UserDTO getUser(String name){
        User user = userRepository.findByName(name);
        if (user == null) {
            throw new UserNotFoundException(name);
        }

        return UserMapper.toDTO(user);
    }

    public UserDTO updateUser(Long id, CreateUserRequest request) {
        User user = getUserEntityById(id);
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());

        return UserMapper.toDTO(userRepository.save(user));
    }

    public void deleteUser(Long id){
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
