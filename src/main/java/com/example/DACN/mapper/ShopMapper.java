package com.example.DACN.mapper;

import com.example.DACN.dto.response.MyShopResponse;
import com.example.DACN.dto.response.ShopDetailResponse;
import com.example.DACN.dto.response.ShopListResponse;
import com.example.DACN.dto.response.UpdateShopResponse;
import com.example.DACN.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShopMapper {

    @Mapping(source = "shopId", target = "shopId")
    @Mapping(source = "shopName", target = "shopName")
    @Mapping(source = "shopDescription", target = "shopDescription")
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(source = "isApproved", target = "isApproved")
    @Mapping(source = "updatedAt", target = "updatedAt")
    UpdateShopResponse toUpdateShopResponse(Shop shop);

    @Mapping(source = "shopId", target = "shopId")
    @Mapping(source = "shopName", target = "shopName")
    @Mapping(source = "shopDescription", target = "shopDescription")
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(source = "ratingAvg", target = "ratingAvg")
    @Mapping(source = "isApproved", target = "isApproved")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    MyShopResponse toMyShopResponse(Shop shop);

    @Mapping(source = "shopId", target = "shopId")
    @Mapping(source = "shopName", target = "shopName")
    @Mapping(source = "shopDescription", target = "shopDescription")
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(source = "ratingAvg", target = "ratingAvg")
    @Mapping(source = "isApproved", target = "isApproved")
    @Mapping(source = "createdAt", target = "createdAt")
    ShopListResponse toShopListResponse(Shop shop);

    @Mapping(source = "shopId", target = "shopId")
    @Mapping(source = "shopName", target = "shopName")
    @Mapping(source = "shopDescription", target = "shopDescription")
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(source = "ratingAvg", target = "ratingAvg")
    @Mapping(source = "isApproved", target = "isApproved")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ShopDetailResponse toShopDetailResponse(Shop shop);
}
