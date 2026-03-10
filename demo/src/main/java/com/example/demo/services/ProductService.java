package com.example.demo.services;

import com.example.demo.entities.Product;
import com.example.demo.entities.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Product createProduct(Product product);

    Product updateProduct(String id, Product product);

    void deleteProduct(String id);

    Optional<Product> findById(String id);

    List<Product> findByCategoryId(String categoryId);

    Page<Product> searchByName(String name, Pageable pageable);

    List<Product> findAvailableProducts();

    List<Product> findByStatus(ProductStatus status);

    Product updateStock(String id, int quantity);
}
