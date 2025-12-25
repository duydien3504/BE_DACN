package com.example.DACN.mapper;

import com.example.DACN.dto.response.DeleteWishlistResponse;
import com.example.DACN.dto.response.WishlistResponse;
import com.example.DACN.entity.Wishlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WishlistMapper {

    @Mapping(target = "wishlistId", source = "wishlistId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "message", constant = "Added to wishlist successfully")
    WishlistResponse toWishlistResponse(Wishlist wishlist);

    @Mapping(target = "wishlistId", source = "wishlistId")
    @Mapping(target = "message", constant = "Removed from wishlist successfully")
    DeleteWishlistResponse toDeleteWishlistResponse(Wishlist wishlist);
}
