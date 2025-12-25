package com.example.DACN.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetailResponse {

    Long productId;
    String name;
    String description;
    BigDecimal price;
    Integer stockQuantity;
    Integer soldCount;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Shop information
    Long shopId;
    String shopName;
    String shopDescription;

    // Category information
    Long categoryId;
    String categoryName;

    // Product images
    List<String> images;
}
