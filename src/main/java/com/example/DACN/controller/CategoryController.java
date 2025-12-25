package com.example.DACN.controller;

import com.example.DACN.dto.request.CreateCategoryRequest;
import com.example.DACN.dto.request.UpdateCategoryRequest;
import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing categories (Admin only)")
public class CategoryController {

        private final CategoryService categoryService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Create a new category", description = "Allows admin to create a new category with optional icon upload")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Category created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request data"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - user is not an admin"),
                        @ApiResponse(responseCode = "404", description = "Parent category not found"),
                        @ApiResponse(responseCode = "409", description = "Category with slug already exists")
        })
        public ResponseEntity<CategoryResponse> createCategory(
                        @RequestParam("name") String name,
                        @RequestParam("slug") String slug,
                        @RequestParam(value = "parentId", required = false) Long parentId,
                        @RequestParam(value = "icon", required = false) MultipartFile icon) throws IOException {

                // Create request object
                CreateCategoryRequest request = CreateCategoryRequest.builder()
                                .name(name)
                                .slug(slug)
                                .parentId(parentId)
                                .build();

                CategoryResponse response = categoryService.createCategory(request, icon);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping(value = "/{category_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Update a category", description = "Allows admin to update an existing category with optional icon upload")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request data"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - user is not an admin"),
                        @ApiResponse(responseCode = "404", description = "Category or parent category not found"),
                        @ApiResponse(responseCode = "409", description = "Category with slug already exists")
        })
        public ResponseEntity<CategoryResponse> updateCategory(
                        @PathVariable("category_id") Long categoryId,
                        @RequestParam(value = "name", required = false) String name,
                        @RequestParam(value = "slug", required = false) String slug,
                        @RequestParam(value = "parentId", required = false) Long parentId,
                        @RequestParam(value = "icon", required = false) MultipartFile icon) throws IOException {

                // Create request object
                UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                                .name(name)
                                .slug(slug)
                                .parentId(parentId)
                                .build();

                CategoryResponse response = categoryService.updateCategory(categoryId, request, icon);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{category_id}")
        @Operation(summary = "Delete a category", description = "Allows admin to soft delete a category (sets hasDeleted = true)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - user is not an admin"),
                        @ApiResponse(responseCode = "404", description = "Category not found or already deleted"),
                        @ApiResponse(responseCode = "409", description = "Cannot delete category with existing products")
        })
        public ResponseEntity<CategoryResponse> deleteCategory(
                        @PathVariable("category_id") Long categoryId) {

                CategoryResponse response = categoryService.deleteCategory(categoryId);
                return ResponseEntity.ok(response);
        }
}
