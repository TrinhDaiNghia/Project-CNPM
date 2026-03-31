package com.example.demo.dtos.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.Date;

@Data
public class ReportFilterRequest {

    private Date fromDate;

    private Date toDate;

    @AssertTrue(message = "toDate must be after fromDate")
    public boolean isDateRangeValid() {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !toDate.before(fromDate);
    }
}

