package com.example.demo.services;

import com.example.demo.entities.Category;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public Category createCategory(Category category) {
        String normalizedName = normalizeName(category.getName());
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        }

        category.setName(normalizedName);
        category.setDescription(normalizeDescription(category.getDescription()));
        return categoryRepository.save(category);
    }

    public Category updateCategory(String id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        String normalizedName = normalizeName(category.getName());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        }

        existing.setName(normalizedName);
        existing.setDescription(normalizeDescription(category.getDescription()));
        return categoryRepository.save(existing);
    }

    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        if (productRepository.existsByAnyCategoryId(id)) {
            throw new IllegalStateException("Không thể xóa danh mục đang có sản phẩm.");
        }
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> findById(String id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Page<Category> searchCategories(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        return categoryRepository.searchCategories(normalizedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục là bắt buộc.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự.");
        }
        return trimmed;
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 500 ký tự.");
        }
        return trimmed;
    }

    private String normalizeSearchKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

