package com.example.DACN.mapper;

import com.example.DACN.dto.request.AddAddressRequest;
import com.example.DACN.dto.response.AddAddressResponse;
import com.example.DACN.dto.response.AddressListResponse;
import com.example.DACN.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "userAddressId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "hasDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    UserAddress toEntity(AddAddressRequest request);

    @Mapping(source = "userAddressId", target = "userAddressId")
    @Mapping(target = "message", constant = "Address added successfully")
    AddAddressResponse toAddAddressResponse(UserAddress userAddress);

    AddressListResponse toAddressListResponse(UserAddress userAddress);

    List<AddressListResponse> toAddressListResponseList(List<UserAddress> userAddresses);
}
