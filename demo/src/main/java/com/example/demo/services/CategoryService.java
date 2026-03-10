package com.example.demo.services;

import com.example.demo.entities.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    Category createCategory(Category category);

    Category updateCategory(String id, Category category);

    void deleteCategory(String id);

    Optional<Category> findById(String id);

    Optional<Category> findByName(String name);

    List<Category> findAll();
}
