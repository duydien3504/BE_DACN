package com.example.DACN.mapper;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CollectVoucherResponse;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.dto.response.DeleteVoucherResponse;
import com.example.DACN.entity.UserVoucher;
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

    @Mapping(source = "userVoucherId", target = "userVoucherId")
    @Mapping(source = "voucher.voucherId", target = "voucherId")
    @Mapping(source = "voucher.code", target = "code")
    @Mapping(source = "voucher.discountType", target = "discountType")
    @Mapping(source = "voucher.discountValue", target = "discountValue")
    @Mapping(source = "voucher.minOrderValue", target = "minOrderValue")
    @Mapping(source = "voucher.maxDiscountAmount", target = "maxDiscountAmount")
    @Mapping(source = "voucher.startDate", target = "startDate")
    @Mapping(source = "voucher.endDate", target = "endDate")
    @Mapping(source = "createdAt", target = "collectedAt")
    @Mapping(target = "message", ignore = true)
    CollectVoucherResponse toCollectVoucherResponse(UserVoucher userVoucher);

    @Mapping(source = "voucherId", target = "voucherId")
    @Mapping(target = "message", ignore = true)
    DeleteVoucherResponse toDeleteVoucherResponse(Voucher voucher);
}
