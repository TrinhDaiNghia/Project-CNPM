package com.example.demo.services;

import com.example.demo.dtos.request.SupplierRequest;
import com.example.demo.entities.ImportReceipt;
import com.example.demo.entities.Supplier;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.ImportReceiptRepository;
import com.example.demo.repositories.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final AccessControlService accessControlService;

    public Supplier createSupplier(SupplierRequest request) {
        accessControlService.requireOwnerRole();

        if (supplierRepository.existsByName(request.getName().trim())) {
            throw new IllegalStateException("Supplier name already exists");
        }

        Supplier supplier = new Supplier();
        applySupplierRequest(supplier, request);
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(String id, SupplierRequest request) {
        accessControlService.requireOwnerRole();

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        boolean changedName = !existing.getName().equalsIgnoreCase(request.getName().trim());
        if (changedName && supplierRepository.existsByName(request.getName().trim())) {
            throw new IllegalStateException("Supplier name already exists");
        }

        applySupplierRequest(existing, request);
        return supplierRepository.save(existing);
    }

    public void deleteSupplier(String id) {
        accessControlService.requireOwnerRole();

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        List<ImportReceipt> linkedReceipts = importReceiptRepository.findBySupplierId(id);
        if (!linkedReceipts.isEmpty()) {
            throw new IllegalStateException("Cannot delete supplier because it is linked to import receipts");
        }

        supplierRepository.delete(supplier);
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> findById(String id) {
        accessControlService.requireOwnerRole();
        return supplierRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        accessControlService.requireOwnerRole();
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Supplier> search(String keyword) {
        accessControlService.requireOwnerRole();
        if (!StringUtils.hasText(keyword)) {
            return findAll();
        }

        String normalized = keyword.trim().toLowerCase();
        return supplierRepository.findAll().stream()
                .filter(supplier -> containsIgnoreCase(supplier.getName(), normalized)
                        || containsIgnoreCase(supplier.getAddress(), normalized)
                        || containsIgnoreCase(supplier.getContractInfo(), normalized))
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void applySupplierRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName().trim());
        supplier.setAddress(trimToNull(request.getAddress()));
        supplier.setContractInfo(trimToNull(request.getContractInfo()));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
