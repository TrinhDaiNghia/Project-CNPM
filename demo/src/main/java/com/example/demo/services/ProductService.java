package com.example.demo.services;

import com.example.demo.dtos.request.ProductRequest;
import com.example.demo.dtos.response.ProductResponse;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;
import com.example.demo.entities.enums.ProductStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new IllegalStateException("Product already exists in this category");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Product product = new Product();
        applyProductRequest(product, request, category);
        return toProductResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        boolean changedIdentity = !existing.getName().equals(request.getName())
                || !existing.getCategory().getId().equals(request.getCategoryId());
        if (changedIdentity && productRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new IllegalStateException("Product already exists in this category");
        }

        applyProductRequest(existing, request, category);
        return toProductResponse(productRepository.save(existing));
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    public List<ProductResponse> compareProducts(String productAId, String productBId) {
        if (productAId == null || productAId.isBlank() || productBId == null || productBId.isBlank()) {
            throw new IllegalArgumentException("Both product IDs are required");
        }

        if (productAId.equals(productBId)) {
            throw new IllegalArgumentException("Cannot compare the same product: " + productAId);
        }

        Map<String, Product> productsById = productRepository.findAllById(List.of(productAId, productBId))
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left));

        Product productA = Optional.ofNullable(productsById.get(productAId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productAId));
        Product productB = Optional.ofNullable(productsById.get(productBId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productBId));

        return List.of(toProductResponse(productA), toProductResponse(productB));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findById(String id) {
        return productRepository.findById(id).map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategoryId(String categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAvailableProducts() {
        return productRepository.findAvailableProducts().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status).stream()
                .map(this::toProductResponse)
                .toList();
    }

    public ProductResponse updateStock(String id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        return toProductResponse(productRepository.save(product));
    }

    private void applyProductRequest(Product product, ProductRequest request, Category category) {
        product.setBrand(request.getBrand());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setMovementType(request.getMovementType());
        product.setGlassMaterial(request.getGlassMaterial());
        product.setWaterResistance(request.getWaterResistance());
        product.setFaceSize(request.getFaceSize());
        product.setWireMaterial(request.getWireMaterial());
        product.setWireColor(request.getWireColor());
        product.setCaseColor(request.getCaseColor());
        product.setFaceColor(request.getFaceColor());

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }

    private ProductResponse toProductResponse(Product product) {
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
                .status(product.getStatus())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(Collections.emptyList())
                .averageRating(null)
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
