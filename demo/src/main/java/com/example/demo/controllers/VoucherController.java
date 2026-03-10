package com.example.demo.controllers;

import com.example.demo.entities.Voucher;
import com.example.demo.services.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

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
    public ResponseEntity<Voucher> create(@Valid @RequestBody Voucher voucher) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(voucher));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Voucher> update(@PathVariable String id, @Valid @RequestBody Voucher voucher) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, voucher));
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
