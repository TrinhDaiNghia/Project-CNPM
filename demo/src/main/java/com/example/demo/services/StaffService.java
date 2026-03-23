package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.StaffCreateRequest;
import com.example.demo.dtos.request.StaffSearchRequest;
import com.example.demo.dtos.request.StaffUpdateRequest;
import com.example.demo.dtos.response.StaffResponse;
import com.example.demo.entities.Staff;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.StaffRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private static final String STAFF_ID_REGEX = "^[A-Za-z0-9_-]{3,30}$";

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    public StaffResponse createStaff(StaffCreateRequest request) {
        accessControlService.requireOwnerRole();
        validateCreateRequest(request);

        Staff staff = new Staff();
        staff.setUsername(request.getUsername().trim());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setFullName(request.getFullName().trim());
        staff.setEmail(request.getEmail().trim().toLowerCase());
        staff.setPhone(normalizeBlankToNull(request.getPhone()));
        staff.setAddress(normalizeBlankToNull(request.getAddress()));
        staff.setGender(request.getGender());
        staff.setRole(request.getRole() == null ? UserRole.STAFF : request.getRole());
        staff.setStaffId(request.getStaffId().trim());

        return DtoMapper.toStaffResponse(staffRepository.save(staff));
    }

    public StaffResponse updateStaff(String id, StaffUpdateRequest request) {
        accessControlService.requirePrivilegedRole();

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));

        validateUpdateRequest(id, request);

        staff.setFullName(request.getFullName().trim());
        staff.setEmail(request.getEmail().trim().toLowerCase());
        staff.setPhone(normalizeBlankToNull(request.getPhone()));
        staff.setAddress(normalizeBlankToNull(request.getAddress()));
        staff.setGender(request.getGender());
        staff.setStaffId(request.getStaffId().trim());

        return DtoMapper.toStaffResponse(staffRepository.save(staff));
    }

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
        accessControlService.requirePrivilegedRole();
        return staffRepository.findById(id).map(DtoMapper::toStaffResponse);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> searchStaff(StaffSearchRequest request, Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        return staffRepository.searchStaff(
                        normalizeSearchText(request.getFullName()),
                        normalizeSearchText(request.getEmail()),
                        normalizeSearchText(request.getPhone()),
                        pageable)
                .map(DtoMapper::toStaffResponse);
    }

    private void validateCreateRequest(StaffCreateRequest request) {
        if (request.getRole() == UserRole.CUSTOMER) {
            throw new IllegalStateException("Staff role cannot be CUSTOMER");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalStateException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalStateException("Email already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && staffRepository.existsByPhone(request.getPhone().trim())) {
            throw new IllegalStateException("Phone already exists");
        }
        validateStaffId(request.getStaffId());
        if (staffRepository.existsByStaffId(request.getStaffId().trim())) {
            throw new IllegalStateException("Staff code already exists");
        }
    }

    private void validateUpdateRequest(String id, StaffUpdateRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (staffRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new IllegalStateException("Email already exists");
        }
        String normalizedPhone = normalizeBlankToNull(request.getPhone());
        if (normalizedPhone != null && staffRepository.existsByPhoneAndIdNot(normalizedPhone, id)) {
            throw new IllegalStateException("Phone already exists");
        }
        validateStaffId(request.getStaffId());
        if (staffRepository.existsByStaffIdAndIdNot(request.getStaffId().trim(), id)) {
            throw new IllegalStateException("Staff code already exists");
        }
    }

    private void validateStaffId(String staffId) {
        if (staffId == null || staffId.isBlank() || !staffId.trim().matches(STAFF_ID_REGEX)) {
            throw new IllegalArgumentException("Invalid staff code format");
        }
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

