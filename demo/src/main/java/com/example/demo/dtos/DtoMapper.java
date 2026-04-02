package com.example.demo.dtos;

import com.example.demo.dtos.response.CustomerResponse;
import com.example.demo.dtos.response.ProductResponse;
import com.example.demo.dtos.response.StaffResponse;
import com.example.demo.dtos.response.SupplierResponse;
import com.example.demo.dtos.response.VoucherResponse;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Product;
import com.example.demo.entities.ProductImage;
import com.example.demo.entities.Staff;
import com.example.demo.entities.Supplier;
import com.example.demo.entities.Voucher;
import com.example.demo.entities.Warranty;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static StaffResponse toStaffResponse(Staff staff) {
        if (staff == null) {
            return null;
        }
        return StaffResponse.builder()
                .id(staff.getId())
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phone(staff.getPhone())
                .address(staff.getAddress())
                .gender(staff.getGender())
                .role(staff.getRole())
                .createdAt(staff.getCreatedAt())
                .build();
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .username(customer.getUsername())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .gender(customer.getGender())
                .role(customer.getRole())
                .isActive(customer.getIsActive())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    public static SupplierResponse toSupplierResponse(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contractInfo(supplier.getContractInfo())
                .address(supplier.getAddress())
                .build();
    }

    public static ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(product.getId())
                .brand(product.getBrand())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .movementType(product.getMovementType())
                .glassMaterial(product.getGlassMaterial())
                .waterResistance(product.getWaterResistance())
                .faceSize(product.getFaceSize())
                .wireMaterial(product.getWireMaterial())
                .wireColor(product.getWireColor())
                .caseColor(product.getCaseColor())
                .faceColor(product.getFaceColor())
                .color(firstNonBlank(product.getWireColor(), product.getCaseColor(), product.getFaceColor()))
                .size(product.getFaceSize())
                .specs(buildSpecs(product))
                .status(product.getStatus())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(mapImageUrls(product.getImages()))
                .averageRating(calculateAverageRating(product))
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static WarrantyResponse toWarrantyResponse(Warranty warranty) {
        if (warranty == null) {
            return null;
        }
        return WarrantyResponse.builder()
                .id(warranty.getId())
                .customerPhone(warranty.getCustomerPhone())
                .customerName(warranty.getCustomerName())
                .issueDescription(warranty.getIssueDescription())
                .receivedDate(warranty.getReceivedDate())
                .expectedReturnDate(warranty.getExpectedReturnDate())
                .status(warranty.getStatus())
                .technicianNote(warranty.getTechnicianNote())
                .rejectReason(warranty.getRejectReason())
                .quantity(warranty.getQuantity())
                .productId(warranty.getProductId())
                .productName(warranty.getProduct() != null ? warranty.getProduct().getName() : null)
                .build();
    }

    public static VoucherResponse toVoucherResponse(Voucher voucher) {
        if (voucher == null) {
            return null;
        }
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountPercent(voucher.getDiscountPercent())
                .isUsed(voucher.getIsUsed())
                .validFrom(voucher.getValidFrom())
                .validTo(voucher.getValidTo())
                .usedAt(voucher.getUsedAt())
                .quantity(voucher.getQuantity())
                .status(voucher.getStatus())
                .active(voucher.getStatus() != null && voucher.getStatus().name().equals("ACTIVE"))
                .build();
    }

    private static List<String> mapImageUrls(List<ProductImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(ProductImage::getImageUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Double calculateAverageRating(Product product) {
        if (product.getReviews() == null || product.getReviews().isEmpty()) {
            return null;
        }
        return product.getReviews().stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0D);
    }

    private static String buildSpecs(Product product) {
        return List.of(product.getMovementType(), product.getGlassMaterial(), product.getWaterResistance(), product.getWireMaterial())
                .stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

