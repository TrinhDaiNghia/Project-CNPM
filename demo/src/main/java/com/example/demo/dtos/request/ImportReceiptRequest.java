package com.example.demo.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ImportReceiptRequest {

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    @NotEmpty(message = "Import items are required")
    @Valid
    private List<ImportReceiptItemRequest> items;
}
