package com.example.DACN.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductListItemResponse {

    Long productId;
    String name;
    String description;
    BigDecimal price;
    Integer stockQuantity;
    Integer soldCount;
    String status;
    LocalDateTime createdAt;
    Long shopId;
    String shopName;
    Long categoryId;
    String categoryName;
}
