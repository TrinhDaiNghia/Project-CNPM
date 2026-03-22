package com.example.demo.controllers;

import com.example.demo.dtos.request.ProductRequest;
import com.example.demo.dtos.response.ProductResponse;
import com.example.demo.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllAvailable() {
        return ResponseEntity.ok(productService.findAvailableProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.searchByName(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable String id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(productService.findByCategoryId(categoryId));
    }

    @PostMapping("/create")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/compare")
    public ResponseEntity<List<ProductResponse>> compare(@RequestParam String productAId, @RequestParam String productBId) {
        return ResponseEntity.ok(productService.compareProducts(productAId, productBId));
    }
}
