package com.example.DACN.controller;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Public Category APIs", description = "Public APIs for browsing categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Get all categories in tree structure (parent-child hierarchy)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> categories = categoryService.getAllCategoriesTree();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root categories", description = "Get all root categories (parent_id IS NULL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Root categories retrieved successfully")
    })
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        List<CategoryResponse> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }
}
