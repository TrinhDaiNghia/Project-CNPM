package com.example.demo.controllers;

import com.example.demo.dtos.request.ProductDiscussionAskRequest;
import com.example.demo.dtos.response.PageResponse;
import com.example.demo.dtos.response.ProductDiscussionAskResponse;
import com.example.demo.dtos.response.ProductDiscussionMessageResponse;
import com.example.demo.services.ProductDiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/discussions")
@RequiredArgsConstructor
public class ProductDiscussionController {

    private final ProductDiscussionService productDiscussionService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductDiscussionMessageResponse>> list(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(productDiscussionService.listByProduct(productId, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ProductDiscussionAskResponse> ask(
            @PathVariable String productId,
            @Valid @RequestBody ProductDiscussionAskRequest request
    ) {
        return ResponseEntity.ok(productDiscussionService.ask(productId, request.getContent()));
    }
}
