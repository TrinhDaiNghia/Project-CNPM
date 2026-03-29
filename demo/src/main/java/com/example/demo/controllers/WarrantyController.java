package com.example.demo.controllers;

import com.example.demo.dtos.request.WarrantyProcessRequest;
import com.example.demo.dtos.request.WarrantyRequest;
import com.example.demo.dtos.request.WarrantySearchRequest;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.services.WarrantyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warranties")
@RequiredArgsConstructor
public class WarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping
    public ResponseEntity<Page<WarrantyResponse>> getAll(
            @Valid @ModelAttribute WarrantySearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(warrantyService.searchWarrantyRequests(request, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<WarrantyResponse>> search(
            @Valid @ModelAttribute WarrantySearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(warrantyService.searchWarrantyRequests(request, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarrantyResponse> getById(@PathVariable String id) {
        return warrantyService.getWarrantyRequestDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WarrantyResponse> create(@Valid @RequestBody WarrantyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warrantyService.createWarrantyRequest(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarrantyResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody WarrantyProcessRequest request) {
        return ResponseEntity.ok(warrantyService.processWarrantyRequest(id, request));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<WarrantyResponse> approve(
            @PathVariable String id,
            @RequestParam(required = false) String technicianNote) {
        return ResponseEntity.ok(warrantyService.approveWarrantyRequest(id, technicianNote));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<WarrantyResponse> reject(
            @PathVariable String id,
            @RequestParam String rejectReason) {
        return ResponseEntity.ok(warrantyService.rejectWarrantyRequest(id, rejectReason));
    }
}

