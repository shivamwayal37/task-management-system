package com.portfolio.task_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portfolio.task_management_system.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    User findByName(String name);
}
