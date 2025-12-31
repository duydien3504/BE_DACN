package com.example.DACN.mapper;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(target = "voucherId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "hasDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "userVouchers", ignore = true)
    Voucher toEntity(CreateVoucherRequest request);

    @Mapping(source = "shop.shopId", target = "shopId")
    @Mapping(target = "message", ignore = true)
    CreateVoucherResponse toCreateVoucherResponse(Voucher voucher);

    @Mapping(source = "shop.shopId", target = "shopId")
    com.example.DACN.dto.response.VoucherResponse toVoucherResponse(Voucher voucher);
}
