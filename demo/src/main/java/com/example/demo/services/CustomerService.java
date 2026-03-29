package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.CustomerCreateRequest;
import com.example.demo.dtos.request.CustomerSearchRequest;
import com.example.demo.dtos.request.CustomerUpdateRequest;
import com.example.demo.dtos.response.CustomerResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        accessControlService.requirePrivilegedRole();

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalStateException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalStateException("Email already exists");
        }
        String normalizedPhone = normalizeBlankToNull(request.getPhone());
        if (normalizedPhone != null && customerRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalStateException("Phone already exists");
        }

        Customer customer = new Customer();
        customer.setUsername(request.getUsername().trim());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setFullName(request.getFullName().trim());
        customer.setEmail(request.getEmail().trim().toLowerCase());
        customer.setPhone(normalizedPhone);
        customer.setAddress(request.getAddress().trim());
        customer.setGender(request.getGender());
        customer.setRole(UserRole.CUSTOMER);

        return DtoMapper.toCustomerResponse(customerRepository.save(customer));
    }

    public CustomerResponse updateCustomer(String id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        accessControlService.requireCustomerAccess(customer.getId());

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (customerRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new IllegalStateException("Email already exists");
        }

        String normalizedPhone = normalizeBlankToNull(request.getPhone());
        if (normalizedPhone != null && customerRepository.existsByPhoneAndIdNot(normalizedPhone, id)) {
            throw new IllegalStateException("Phone already exists");
        }

        customer.setFullName(request.getFullName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(normalizedPhone);
        customer.setAddress(request.getAddress().trim());
        customer.setGender(request.getGender());

        return DtoMapper.toCustomerResponse(customerRepository.save(customer));
    }

    public void deleteCustomer(String id) {
        accessControlService.requirePrivilegedRole();

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        if (customerRepository.existsRelatedOrders(id)) {
            throw new IllegalStateException("Cannot delete customer because related orders exist");
        }

        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findById(String id) {
        accessControlService.requireCustomerAccess(id);
        return customerRepository.findById(id).map(DtoMapper::toCustomerResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(CustomerSearchRequest request, Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        return customerRepository.searchCustomers(
                        normalizeSearchText(request.getFullName()),
                        normalizeSearchText(request.getEmail()),
                        normalizeSearchText(request.getPhone()),
                        normalizeSearchText(request.getAddress()),
                        pageable)
                .map(DtoMapper::toCustomerResponse);
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

