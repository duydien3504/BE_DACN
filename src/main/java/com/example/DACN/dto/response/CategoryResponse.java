package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing category information")
public class CategoryResponse {

    @Schema(description = "Category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Category name", example = "Electronics")
    private String name;

    @Schema(description = "URL-friendly slug", example = "electronics")
    private String slug;

    @Schema(description = "Icon URL from Cloudinary", example = "https://res.cloudinary.com/...")
    private String iconUrl;

    @Schema(description = "Parent category ID", example = "null")
    private Long parentId;

    @Schema(description = "Whether category is deleted", example = "false")
    private Boolean hasDeleted;
}
