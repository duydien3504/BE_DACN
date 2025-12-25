package com.example.DACN.mapper;

import com.example.DACN.dto.request.RegisterRequest;
import com.example.DACN.dto.response.RegisterResponse;
import com.example.DACN.dto.response.UserProfileResponse;
import com.example.DACN.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isEmailVerified", constant = "false")
    @Mapping(target = "status", constant = "Active")
    @Mapping(target = "hasDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "shops", ignore = true)
    @Mapping(target = "wishlists", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "wallet", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(source = "userId", target = "userId")
    @Mapping(target = "message", constant = "Success")
    RegisterResponse toRegisterResponse(User user);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "role.roleName", target = "role")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "isEmailVerified", target = "isEmailVerified")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    UserProfileResponse toUserProfileResponse(User user);
}
