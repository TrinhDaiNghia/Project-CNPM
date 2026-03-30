package com.example.demo.controllers;

import com.example.demo.dtos.request.ProductCreateRequest;
import com.example.demo.dtos.request.ProductSearchRequest;
import com.example.demo.dtos.request.ProductUpdateRequest;
import com.example.demo.dtos.response.ProductImageResponse;
import com.example.demo.entities.Product;
import com.example.demo.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<Page<Product>> search(
            @Valid @ModelAttribute ProductSearchRequest request,
            @RequestParam(required = false) String specs,
            @PageableDefault(size = 10) Pageable pageable) {
        if ((request.getSpec() == null || request.getSpec().isBlank()) && specs != null && !specs.isBlank()) {
            request.setSpec(specs);
        }
        return ResponseEntity.ok(productService.searchProducts(request, pageable));
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

    @PostMapping({"", "/create"})
    public ResponseEntity<Product> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> uploadImages(
            @PathVariable String id,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) Integer thumbnailIndex) {
        return ResponseEntity.ok(productService.uploadProductImages(id, files, thumbnailIndex));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ProductImageResponse>> getImages(@PathVariable("id") String productId) {
        return ResponseEntity.ok(productService.getProductImages(productId));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable("id") String productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) Boolean isThumbnail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.uploadProductImage(productId, file, altText, isThumbnail));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable("id") String productId,
                                            @PathVariable String imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/compare")
    public ResponseEntity<List<Product>> compare(@RequestParam String productAId, @RequestParam String productBId) {
        return ResponseEntity.ok(productService.compareProducts(productAId, productBId));
    }
}
