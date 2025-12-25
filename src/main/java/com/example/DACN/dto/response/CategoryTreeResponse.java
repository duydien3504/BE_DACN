package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category tree response with children")
public class CategoryTreeResponse {

    @Schema(description = "Category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Category name", example = "Electronics")
    private String name;

    @Schema(description = "URL-friendly slug", example = "electronics")
    private String slug;

    @Schema(description = "Icon URL", example = "https://cloudinary.com/icon.png")
    private String iconUrl;

    @Schema(description = "Deletion status", example = "false")
    private Boolean hasDeleted;

    @Schema(description = "Child categories")
    private List<CategoryTreeResponse> children;
}
