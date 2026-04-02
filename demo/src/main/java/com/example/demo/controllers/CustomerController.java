package com.example.demo.controllers;

import com.example.demo.dtos.request.CustomerCreateRequest;
import com.example.demo.dtos.request.CustomerSearchRequest;
import com.example.demo.dtos.request.CustomerUpdateRequest;
import com.example.demo.dtos.response.CustomerResponse;
import com.example.demo.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(new CustomerSearchRequest(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> search(
            @Valid @ModelAttribute CustomerSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(request, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable String id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable String id, @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<CustomerResponse> lock(@PathVariable String id) {
        return ResponseEntity.ok(customerService.lockCustomer(id));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<CustomerResponse> unlock(@PathVariable String id) {
        return ResponseEntity.ok(customerService.unlockCustomer(id));
    }
}

