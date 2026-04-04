package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long total;
    private int totalPages;
}
