package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.CustomerSearchRequest;
import com.example.demo.dtos.response.CustomerResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccessControlService accessControlService;

    public void deleteCustomer(String id) {
        accessControlService.requirePrivilegedRole();

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        if (customerRepository.existsRelatedOrders(id)) {
            throw new IllegalStateException("Cannot delete customer because related orders exist");
        }

        customerRepository.delete(customer);
    }

    public CustomerResponse promoteCustomerToStaff(String id) {
        accessControlService.requireOwnerRole();

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        customer.setRole(UserRole.STAFF);
        customer.setIsActive(true);
        return DtoMapper.toCustomerResponse(customerRepository.save(customer));
    }

    public CustomerResponse lockCustomer(String id) {
        return updateCustomerActiveStatus(id, false);
    }

    public CustomerResponse unlockCustomer(String id) {
        return updateCustomerActiveStatus(id, true);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findById(String id) {
        accessControlService.requireCustomerAccess(id);
        return customerRepository.findById(id)
                .filter(customer -> customer.getRole() == UserRole.CUSTOMER)
                .map(DtoMapper::toCustomerResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(CustomerSearchRequest request, Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        return customerRepository.searchCustomers(
                        UserRole.CUSTOMER,
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

    private CustomerResponse updateCustomerActiveStatus(String customerId, boolean isActive) {
        accessControlService.requirePrivilegedRole();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        customer.setIsActive(isActive);
        return DtoMapper.toCustomerResponse(customerRepository.save(customer));
    }
}

