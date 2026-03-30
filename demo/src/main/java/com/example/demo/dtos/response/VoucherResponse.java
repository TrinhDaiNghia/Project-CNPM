package com.example.demo.dtos.response;

import com.example.demo.entities.enums.VoucherStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class VoucherResponse {

    private String id;
    private String code;
    private Integer discountPercent;
    private Boolean isUsed;
    private Date validFrom;
    private Date validTo;
    private Date usedAt;
    private Integer quantity;
    private VoucherStatus status;
    private Boolean active;
}

