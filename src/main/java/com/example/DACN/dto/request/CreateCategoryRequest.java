package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request for creating a new category")
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category name", example = "Electronics", required = true)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 150, message = "Slug must not exceed 150 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase, alphanumeric, and can contain hyphens")
    @Schema(description = "URL-friendly slug", example = "electronics", required = true)
    private String slug;

    @Schema(description = "Parent category ID for subcategories", example = "1")
    private Long parentId;
}
