package com.example.demo.repositories;

import com.example.demo.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    Optional<Category> findByName(String name);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    @Query("SELECT c FROM Category c WHERE (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);
}
