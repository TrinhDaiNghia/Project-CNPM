package com.example.demo.controllers;

import com.example.demo.dtos.request.WarrantyRequest;
import com.example.demo.dtos.request.WarrantyStatusUpdateRequest;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.entities.enums.WarrantyStatus;
import com.example.demo.services.WarrantyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warranties")
@RequiredArgsConstructor
public class WarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping
    public ResponseEntity<List<WarrantyResponse>> getAll(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) WarrantyStatus status) {
        return ResponseEntity.ok(warrantyService.findAll(customerId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarrantyResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(warrantyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<WarrantyResponse> create(@Valid @RequestBody WarrantyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warrantyService.createWarranty(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarrantyResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody WarrantyStatusUpdateRequest request) {
        return ResponseEntity.ok(warrantyService.updateStatus(id, request));
    }
}
