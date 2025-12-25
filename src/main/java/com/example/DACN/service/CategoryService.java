package com.example.DACN.service;

import com.example.DACN.dto.request.CreateCategoryRequest;
import com.example.DACN.dto.request.UpdateCategoryRequest;
import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.CategoryMapper;
import com.example.DACN.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryMapper categoryMapper;

    /**
     * Create a new category with optional icon upload
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, MultipartFile icon) throws IOException {
        log.info("Creating category with slug: {}", request.getSlug());

        // Check if slug already exists
        if (categoryRepository.existsBySlug(request.getSlug())) {
            log.warn("Category with slug {} already exists", request.getSlug());
            throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
        }

        // Create new category entity
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setHasDeleted(false);

        // Handle parent category if provided
        if (request.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (parentCategory.getHasDeleted()) {
                throw new ResourceNotFoundException("Parent category has been deleted");
            }

            category.setParent(parentCategory);
            log.info("Setting parent category ID: {}", request.getParentId());
        }

        // Upload icon to Cloudinary if provided
        if (icon != null && !icon.isEmpty()) {
            log.info("Uploading category icon to Cloudinary");
            String iconUrl = cloudinaryService.uploadImage(icon);
            category.setIconUrl(iconUrl);
            log.info("Icon uploaded successfully: {}", iconUrl);
        }

        // Save category
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getCategoryId());

        // Map to response
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    /**
     * Update an existing category with optional icon upload
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request, MultipartFile icon)
            throws IOException {
        log.info("Updating category with ID: {}", categoryId);

        // Find existing category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getHasDeleted()) {
            throw new ResourceNotFoundException("Category has been deleted");
        }

        // Update name if provided
        if (request.getName() != null) {
            category.setName(request.getName());
            log.info("Updated category name to: {}", request.getName());
        }

        // Update slug if provided and different
        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            // Check if new slug already exists
            if (categoryRepository.existsBySlug(request.getSlug())) {
                log.warn("Category with slug {} already exists", request.getSlug());
                throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
            }
            category.setSlug(request.getSlug());
            log.info("Updated category slug to: {}", request.getSlug());
        }

        // Update parent category if provided
        if (request.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (parentCategory.getHasDeleted()) {
                throw new ResourceNotFoundException("Parent category has been deleted");
            }

            // Prevent circular relationship
            if (parentCategory.getCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            category.setParent(parentCategory);
            log.info("Updated parent category ID to: {}", request.getParentId());
        }

        // Upload new icon to Cloudinary if provided
        if (icon != null && !icon.isEmpty()) {
            log.info("Uploading new category icon to Cloudinary");

            // Delete old icon if exists
            if (category.getIconUrl() != null) {
                cloudinaryService.deleteImage(category.getIconUrl());
                log.info("Deleted old icon");
            }

            String iconUrl = cloudinaryService.uploadImage(icon);
            category.setIconUrl(iconUrl);
            log.info("New icon uploaded successfully: {}", iconUrl);
        }

        // Save updated category
        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with ID: {}", updatedCategory.getCategoryId());

        // Map to response
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    /**
     * Soft delete a category by setting hasDeleted = true
     */
    @Transactional
    public CategoryResponse deleteCategory(Long categoryId) {
        log.info("Deleting category with ID: {}", categoryId);

        // Find existing category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getHasDeleted()) {
            throw new ResourceNotFoundException("Category has already been deleted");
        }

        // Check if category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            log.warn("Cannot delete category ID {} - has {} products", categoryId, category.getProducts().size());
            throw new IllegalStateException("Cannot delete category with existing products");
        }

        // Soft delete
        category.setHasDeleted(true);
        Category deletedCategory = categoryRepository.save(category);
        log.info("Category soft deleted successfully with ID: {}", deletedCategory.getCategoryId());

        // Map to response
        return categoryMapper.toCategoryResponse(deletedCategory);
    }

    /**
     * Get all categories in tree structure
     */
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getAllCategoriesTree() {
        log.info("Fetching all categories in tree structure");

        // Fetch ALL non-deleted categories at once (no lazy loading)
        List<Category> allCategories = categoryRepository.findAll().stream()
                .filter(c -> !c.getHasDeleted())
                .collect(Collectors.toList());

        // Build a map for quick lookup
        Map<Long, List<Category>> childrenMap = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();

        for (Category category : allCategories) {
            if (category.getParent() == null) {
                rootCategories.add(category);
            } else {
                Long parentId = category.getParent().getCategoryId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
            }
        }

        // Build tree from root categories
        List<CategoryTreeResponse> tree = rootCategories.stream()
                .map(root -> buildCategoryTreeFromMap(root, childrenMap))
                .collect(Collectors.toList());

        log.info("Retrieved {} root categories with tree structure", tree.size());
        return tree;
    }

    private CategoryTreeResponse buildCategoryTreeFromMap(Category category, Map<Long, List<Category>> childrenMap) {
        CategoryTreeResponse response = categoryMapper.toCategoryTreeResponse(category);

        // Get children from map (no lazy loading)
        List<Category> children = childrenMap.get(category.getCategoryId());
        if (children != null && !children.isEmpty()) {
            List<CategoryTreeResponse> childResponses = children.stream()
                    .map(child -> buildCategoryTreeFromMap(child, childrenMap))
                    .collect(Collectors.toList());
            response.setChildren(childResponses);
        }

        return response;
    }

    /**
     * Recursively build category tree
     */
    private CategoryTreeResponse buildCategoryTree(Category category) {
        CategoryTreeResponse response = categoryMapper.toCategoryTreeResponse(category);

        // Get non-deleted children - convert to ArrayList to avoid
        // ConcurrentModificationException
        // Don't call isEmpty() as it triggers lazy loading
        if (category.getSubCategories() != null) {
            List<CategoryTreeResponse> children = new ArrayList<>(category.getSubCategories()).stream()
                    .filter(child -> !child.getHasDeleted())
                    .map(this::buildCategoryTree)
                    .collect(Collectors.toList());

            response.setChildren(children.isEmpty() ? null : children);
        }

        return response;
    }

    /**
     * Get all root categories (parent = null)
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        log.info("Fetching all root categories");

        // Get all non-deleted root categories (parent = null)
        List<Category> rootCategories = categoryRepository.findAll().stream()
                .filter(category -> !category.getHasDeleted() && category.getParent() == null)
                .collect(Collectors.toList());

        // Map to response
        List<CategoryResponse> response = rootCategories.stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} root categories", response.size());
        return response;
    }
}
