package com.example.demo.services;

import com.example.demo.entities.Customer;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    public void requireOwnerRole() {
        User currentUser = getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new AccessDeniedException("Only OWNER is allowed for this action");
        }
    }

    public void requirePrivilegedRole() {
        User currentUser = getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.OWNER && currentUser.getRole() != UserRole.STAFF) {
            throw new AccessDeniedException("Only STAFF or OWNER is allowed for this action");
        }
    }

    public void requireUserSelfOrPrivileged(String targetUserId) {
        User currentUser = getCurrentUserOrThrow();
        if (isPrivileged(currentUser) || currentUser.getId().equals(targetUserId)) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this user");
    }

    public void requireCustomerAccess(String customerId) {
        User currentUser = getCurrentUserOrThrow();
        if (isPrivileged(currentUser)) {
            return;
        }

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("You are not allowed to access this customer");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        if (!customer.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to access this customer");
        }
    }

    public boolean isOwner(User user) {
        return user.getRole() == UserRole.OWNER;
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == UserRole.OWNER || user.getRole() == UserRole.STAFF;
    }
}




