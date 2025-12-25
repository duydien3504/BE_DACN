package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for updating a category")
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category name", example = "Electronics")
    private String name;

    @Size(max = 150, message = "Slug must not exceed 150 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase, alphanumeric, and can contain hyphens")
    @Schema(description = "URL-friendly slug", example = "electronics")
    private String slug;

    @Schema(description = "Parent category ID for subcategories", example = "1")
    private Long parentId;
}
