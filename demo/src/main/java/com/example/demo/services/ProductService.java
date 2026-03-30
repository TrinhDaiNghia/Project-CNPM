package com.example.demo.services;

import com.example.demo.dtos.request.ProductCreateRequest;
import com.example.demo.dtos.request.ProductSearchRequest;
import com.example.demo.dtos.request.ProductUpdateRequest;
import com.example.demo.dtos.response.ProductImageResponse;
import com.example.demo.dtos.response.ProductResponse;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;
import com.example.demo.entities.ProductImage;
import com.example.demo.entities.enums.ProductStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.ProductImageRepository;
import com.example.demo.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final AccessControlService accessControlService;
    private final CloudinaryService cloudinaryService;

    public ProductResponse createProduct(ProductCreateRequest request) {
        accessControlService.requirePrivilegedRole();
        validateProductRequest(request.getPrice(), request.getStockQuantity(), request.getName(), request.getBrand());

        if (productRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new IllegalStateException("Product already exists in this category");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Product product = new Product();
        applyProductRequest(product, request, category);
        return toProductResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        accessControlService.requirePrivilegedRole();
        validateProductRequest(request.getPrice(), request.getStockQuantity(), request.getName(), request.getBrand());

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
        accessControlService.requirePrivilegedRole();
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (productRepository.existsRelatedTransactions(id)) {
            throw new IllegalStateException("Cannot delete product because it is referenced in order transactions");
        }

        productRepository.delete(existing);
    }

    public ProductImageResponse uploadProductImage(String productId,
                                                   MultipartFile file,
                                                   String altText,
                                                   Boolean isThumbnail) {
        accessControlService.requirePrivilegedRole();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        CloudinaryService.CloudinaryUploadResult uploaded = cloudinaryService.uploadProductImage(file, productId);

        if (Boolean.TRUE.equals(isThumbnail)) {
            productImageRepository.clearThumbnailByProductId(productId);
        }

        ProductImage image = ProductImage.builder()
                .imageUrl(uploaded.imageUrl())
                .publicId(uploaded.publicId())
                .altText(normalizeOptionalText(altText))
                .isThumbnail(Boolean.TRUE.equals(isThumbnail))
                .product(product)
                .build();

        ProductImage saved = productImageRepository.save(image);
        return mapProductImageResponse(saved);
    }

        public ProductResponse uploadProductImages(String productId,
                               List<MultipartFile> files,
                               Integer thumbnailIndex) {
        accessControlService.requirePrivilegedRole();

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (thumbnailIndex != null && thumbnailIndex >= 0 && thumbnailIndex < files.size()) {
            productImageRepository.clearThumbnailByProductId(productId);
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            CloudinaryService.CloudinaryUploadResult uploaded = cloudinaryService.uploadProductImage(file, productId);

            ProductImage image = ProductImage.builder()
                .imageUrl(uploaded.imageUrl())
                .publicId(uploaded.publicId())
                .altText(null)
                .isThumbnail(thumbnailIndex != null && thumbnailIndex == i)
                .product(product)
                .build();

            productImageRepository.save(image);
        }

        Product updated = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return toProductResponse(updated);
        }

        public void deleteProductImage(String productId, String imageId) {
        accessControlService.requirePrivilegedRole();

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product image not found: " + imageId));

        cloudinaryService.deleteProductImage(image.getPublicId());
        productImageRepository.delete(image);
        }

        @Transactional(readOnly = true)
        public List<ProductImageResponse> getProductImages(String productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        return productImageRepository.findByProductId(productId)
            .stream()
            .map(this::mapProductImageResponse)
            .toList();
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
    public Page<Product> searchProducts(ProductSearchRequest request, Pageable pageable) {
        return productRepository.searchProducts(
                normalizeSearchText(request.getName()),
                normalizeSearchText(request.getBrand()),
                normalizeSearchText(request.getColor()),
                normalizeSearchText(request.getFaceSize()),
                normalizeSearchText(request.getSpec()),
                request.getStatus(),
                pageable);
    }
    @Transactional(readOnly = true)
    public List<ProductResponse> findAvailableProducts() {
        return productRepository.findAvailableProducts().stream()
                .map(this::toProductResponse)
                .toList();
    }

    public Product updateStock(String id, int quantity) {
        accessControlService.requirePrivilegedRole();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        int nextQuantity = product.getStockQuantity() + quantity;
        if (nextQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        product.setStockQuantity(nextQuantity);
        return productRepository.save(product);
    }

    private void validateProductRequest(Long price, Integer stockQuantity, String name, String brand) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Brand is required");
        }
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative");
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ProductImageResponse mapProductImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .isThumbnail(image.getIsThumbnail())
                .build();
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

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
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

    private ProductResponse toProductResponse(Product product) {
        List<String> imageUrls = product.getImages().stream()
                .sorted(Comparator.comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getIsThumbnail())))
                .map(ProductImage::getImageUrl)
                .toList();

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
                .imageUrls(imageUrls)
                .averageRating(null)
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
