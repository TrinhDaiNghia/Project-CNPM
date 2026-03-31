package com.example.demo.controllers;

import com.example.demo.dtos.request.ImportReceiptRequest;
import com.example.demo.dtos.response.ImportReceiptResponse;
import com.example.demo.services.ImportReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/import-receipts")
@RequiredArgsConstructor
public class ImportReceiptController {

    private final ImportReceiptService importReceiptService;

    @GetMapping
    public ResponseEntity<List<ImportReceiptResponse>> getAll(
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(importReceiptService.findAll(supplierId, fromDate, toDate, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportReceiptResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(importReceiptService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ImportReceiptResponse> create(@Valid @RequestBody ImportReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importReceiptService.createImportReceipt(request));
    }
}
