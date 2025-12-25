package com.example.DACN.mapper;

import com.example.DACN.dto.response.AddCartItemResponse;
import com.example.DACN.dto.response.CartItemResponse;
import com.example.DACN.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "message", ignore = true)
    AddCartItemResponse toAddCartItemResponse(CartItem cartItem);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "imageUrl", expression = "java(getMainImage(cartItem.getProduct()))")
    @Mapping(target = "subtotal", expression = "java(cartItem.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity())))")
    CartItemResponse toCartItemResponse(CartItem cartItem);

    default String getMainImage(com.example.DACN.entity.Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .sorted(java.util.Comparator.comparingInt(com.example.DACN.entity.ProductImage::getDisplayOrder))
                    .findFirst()
                    .map(com.example.DACN.entity.ProductImage::getImageUrl)
                    .orElse(null);
        }
        return null;
    }

}
