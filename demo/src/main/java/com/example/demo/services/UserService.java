package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(User user);

    User updateUser(String id, User user);

    void deleteUser(String id);

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAll();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
