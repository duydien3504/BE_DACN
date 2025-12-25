package com.example.DACN.mapper;

import com.example.DACN.dto.response.CreateProductResponse;
import com.example.DACN.dto.response.DeleteProductResponse;
import com.example.DACN.dto.response.ProductDetailResponse;
import com.example.DACN.dto.response.ProductListItemResponse;
import com.example.DACN.dto.response.UpdateProductResponse;
import com.example.DACN.dto.response.UpdateProductStatusResponse;
import com.example.DACN.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "message", constant = "Product created successfully")
    CreateProductResponse toCreateProductResponse(Product product);

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "message", constant = "Product updated successfully")
    UpdateProductResponse toUpdateProductResponse(Product product);

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "message", constant = "Product deleted successfully")
    DeleteProductResponse toDeleteProductResponse(Product product);

    @Mapping(target = "shopId", source = "shop.shopId")
    @Mapping(target = "shopName", source = "shop.shopName")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.name")
    ProductListItemResponse toProductListItemResponse(Product product);

    @Mapping(target = "shopId", source = "shop.shopId")
    @Mapping(target = "shopName", source = "shop.shopName")
    @Mapping(target = "shopDescription", source = "shop.shopDescription")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "images", ignore = true)
    ProductDetailResponse toProductDetailResponse(Product product);

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "message", constant = "Product status updated successfully")
    UpdateProductStatusResponse toUpdateProductStatusResponse(Product product);
}
