package com.example.DACN.repository;

import com.example.DACN.entity.Category;
import jakarta.persistence.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNullAndHasDeletedFalse();

    List<Category> findByParentCategoryIdAndHasDeletedFalse(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.hasDeleted = false ORDER BY c.name")
    List<Category> findAllRootCategories();

    boolean existsBySlug(String slug);
}
