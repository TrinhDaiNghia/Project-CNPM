package com.example.demo.controllers;

import com.example.demo.dtos.request.VoucherCreateRequest;
import com.example.demo.dtos.request.VoucherSearchRequest;
import com.example.demo.dtos.request.VoucherStatusUpdateRequest;
import com.example.demo.dtos.request.VoucherUpdateRequest;
import com.example.demo.entities.Voucher;
import com.example.demo.services.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<Page<Voucher>> getAll(
            @Valid @ModelAttribute VoucherSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(voucherService.searchVouchers(request, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Voucher>> search(
            @Valid @ModelAttribute VoucherSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(voucherService.searchVouchers(request, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Voucher>> getActive() {
        return ResponseEntity.ok(voucherService.findActiveVouchers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Voucher> getById(@PathVariable String id) {
        return voucherService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Voucher> getByCode(@PathVariable String code) {
        return voucherService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Voucher> create(@Valid @RequestBody VoucherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Voucher> update(@PathVariable String id, @Valid @RequestBody VoucherUpdateRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Voucher> updateStatus(@PathVariable String id, @Valid @RequestBody VoucherStatusUpdateRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucherStatus(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Voucher> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(voucherService.disableVoucher(id));
    }

    @PostMapping("/apply/{code}")
    public ResponseEntity<Voucher> apply(@PathVariable String code) {
        return ResponseEntity.ok(voucherService.applyVoucher(code));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }
}
