package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.StaffSearchRequest;
import com.example.demo.dtos.response.StaffResponse;
import com.example.demo.entities.Staff;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final AccessControlService accessControlService;

    public void deleteStaff(String id) {
        accessControlService.requireOwnerRole();

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));

        if (staffRepository.existsRelatedRecords(id)) {
            throw new IllegalStateException("Cannot delete staff because related records exist");
        }

        staffRepository.delete(staff);
    }

    @Transactional(readOnly = true)
    public Optional<StaffResponse> findById(String id) {
        accessControlService.requireOwnerRole();
        return staffRepository.findById(id).map(DtoMapper::toStaffResponse);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> searchStaff(StaffSearchRequest request, Pageable pageable) {
        accessControlService.requireOwnerRole();
        return staffRepository.searchStaff(
                        normalizeSearchText(request.getKeyword()),
                        pageable)
                .map(DtoMapper::toStaffResponse);
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}

