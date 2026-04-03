package com.example.demo.services;

import com.example.demo.entities.Customer;
import com.example.demo.entities.User;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final CustomerRepository customerRepository;
    private final OwnerRepository ownerRepository;

    public void syncProfileForRole(User user) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            throw new IllegalArgumentException("User, id, and role are required to sync profiles");
        }

        switch (user.getRole()) {
            case CUSTOMER -> {
                ensureCustomerProfile(user);
                removeOwnerProfileIfExists(user.getId());
            }
            case OWNER -> {
                ensureOwnerProfile(user);
                removeCustomerProfileIfExists(user.getId());
            }
            case STAFF -> {
                removeCustomerProfileIfExists(user.getId());
                removeOwnerProfileIfExists(user.getId());
            }
        }
    }

    private void ensureCustomerProfile(User user) {
        if (customerRepository.existsById(user.getId())) {
            return;
        }

        Customer customer = new Customer();
        customer.setId(user.getId());
        customerRepository.save(customer);
    }

    private void ensureOwnerProfile(User user) {
        if (ownerRepository.existsById(user.getId())) {
            return;
        }
        ownerRepository.insertOwnerProfile(user.getId());
    }

    private void removeCustomerProfileIfExists(String userId) {
        if (!customerRepository.existsById(userId)) {
            return;
        }

        try {
            customerRepository.deleteById(userId);
            customerRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Cannot remove customer profile because it is referenced by business data", ex);
        }
    }

    private void removeOwnerProfileIfExists(String userId) {
        if (!ownerRepository.existsById(userId)) {
            return;
        }

        try {
            ownerRepository.deleteById(userId);
            ownerRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Cannot remove owner profile because it is referenced by business data", ex);
        }
    }
}
