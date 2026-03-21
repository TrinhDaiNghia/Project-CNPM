package com.example.demo.services;

import com.example.demo.dtos.request.ProductCreateRequest;
import com.example.demo.dtos.request.ProductUpdateRequest;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Product createProduct(ProductCreateRequest request) {
        if (productRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new IllegalStateException("Product already exists in this category");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Product product = new Product();
        applyProductRequest(product, request, category);
        return productRepository.save(product);
    }

    public Product updateProduct(String id, ProductUpdateRequest request) {
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
        productRepository.deleteById(id);
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

    private void applyProductRequest(Product product, ProductCreateRequest request, Category category) {
        applyProductFields(product, request.getBrand(), request.getName(), request.getDescription(), request.getPrice(),
                request.getStockQuantity(), request.getMovementType(), request.getGlassMaterial(), request.getWaterResistance(),
                request.getFaceSize(), request.getSize(), request.getWireMaterial(), request.getWireColor(), request.getCaseColor(),
                request.getFaceColor(), request.getColor(), request.getSpecs(), category);
    }

    private void applyProductRequest(Product product, ProductUpdateRequest request, Category category) {
        applyProductFields(product, request.getBrand(), request.getName(), request.getDescription(), request.getPrice(),
                request.getStockQuantity(), request.getMovementType(), request.getGlassMaterial(), request.getWaterResistance(),
                request.getFaceSize(), request.getSize(), request.getWireMaterial(), request.getWireColor(), request.getCaseColor(),
                request.getFaceColor(), request.getColor(), request.getSpecs(), category);
    }

    private void applyProductFields(Product product,
                                    String brand,
                                    String name,
                                    String description,
                                    Long price,
                                    Integer stockQuantity,
                                    String movementType,
                                    String glassMaterial,
                                    String waterResistance,
                                    String faceSize,
                                    String size,
                                    String wireMaterial,
                                    String wireColor,
                                    String caseColor,
                                    String faceColor,
                                    String color,
                                    String specs,
                                    Category category) {
        product.setBrand(brand);
        product.setName(name);
        product.setDescription(description != null ? description : specs);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setMovementType(movementType);
        product.setGlassMaterial(glassMaterial);
        product.setWaterResistance(waterResistance);
        product.setFaceSize(faceSize != null ? faceSize : size);
        product.setWireMaterial(wireMaterial);
        product.setWireColor(wireColor != null ? wireColor : color);
        product.setCaseColor(caseColor != null ? caseColor : color);
        product.setFaceColor(faceColor != null ? faceColor : color);

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }
}
