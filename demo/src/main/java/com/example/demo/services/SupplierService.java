package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.SupplierRequest;
import com.example.demo.dtos.request.SupplierSearchRequest;
import com.example.demo.dtos.response.SupplierResponse;
import com.example.demo.entities.Supplier;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AccessControlService accessControlService;

    public SupplierResponse createSupplier(SupplierRequest request) {
        accessControlService.requirePrivilegedRole();
        validateSupplierUnique(request, null);

        Supplier supplier = new Supplier();
        applySupplierRequest(supplier, request);
        return DtoMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse updateSupplier(String id, SupplierRequest request) {
        accessControlService.requirePrivilegedRole();

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        validateSupplierUnique(request, id);
        applySupplierRequest(supplier, request);

        return DtoMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    public void deleteSupplier(String id) {
        accessControlService.requirePrivilegedRole();

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        if (supplierRepository.existsRelatedRecords(id)) {
            throw new IllegalStateException("Cannot delete supplier because related import records exist");
        }

        supplierRepository.delete(supplier);
    }

    @Transactional(readOnly = true)
    public Optional<SupplierResponse> findById(String id) {
        accessControlService.requirePrivilegedRole();
        return supplierRepository.findById(id).map(DtoMapper::toSupplierResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(SupplierSearchRequest request, Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        return supplierRepository.searchSuppliers(
                        normalizeSearchText(request.getKeyword()),
                        normalizeSearchText(request.getName()),
                        normalizeSearchText(request.getContractInfo()),
                        normalizeSearchText(request.getAddress()),
                        pageable)
                .map(DtoMapper::toSupplierResponse);
    }

    private void validateSupplierUnique(SupplierRequest request, String supplierId) {
        String normalizedName = request.getName().trim();

        if (supplierId == null) {
            if (supplierRepository.existsByName(normalizedName)) {
                throw new IllegalStateException("Supplier name already exists");
            }
            return;
        }

        if (supplierRepository.existsByNameAndIdNot(normalizedName, supplierId)) {
            throw new IllegalStateException("Supplier name already exists");
        }
    }

    private void applySupplierRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName().trim());
        supplier.setContractInfo(normalizeBlankToNull(request.getContractInfo()));
        supplier.setAddress(normalizeBlankToNull(request.getAddress()));
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

