package com.example.DACN.dto.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductRequest {

    @Size(max = 200, message = "Product name must not exceed 200 characters")
    String name;

    String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price;

    Long categoryId;

    @Min(value = 0, message = "Stock quantity must be non-negative")
    Integer stockQuantity;

    @Pattern(regexp = "^(Active|Inactive)$", message = "Status must be either 'Active' or 'Inactive'")
    String status;
}
