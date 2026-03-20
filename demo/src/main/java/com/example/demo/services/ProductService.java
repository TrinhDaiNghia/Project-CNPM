package com.example.demo.services;

import com.example.demo.dtos.request.ProductRequest;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Product createProduct(ProductRequest request) {
        if (productRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new IllegalStateException("Product already exists in this category");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Product product = new Product();
        applyProductRequest(product, request, category);
        return productRepository.save(product);
    }

    public Product updateProduct(String id, ProductRequest request) {
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
        return productRepository.save(existing);
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    public List<Product> compareProducts(String productAId, String productBId) {
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

        return List.of(productA, productB);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategoryId(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> findAvailableProducts() {
        return productRepository.findAvailableProducts();
    }

    @Transactional(readOnly = true)
    public List<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    public Product updateStock(String id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        return productRepository.save(product);
    }

    private void applyProductRequest(Product product, ProductRequest request, Category category) {
        product.setBrand(request.getBrand());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setPartNumber(request.getPartNumber());
        product.setPowerSource(request.getPowerSource());
        product.setLicense(request.getLicense());
        product.setWarranty(request.getWarranty());

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }
}
