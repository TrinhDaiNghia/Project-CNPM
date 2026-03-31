package com.example.demo.services;

import com.example.demo.entities.Category;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(String id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(String id) {
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
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
}

