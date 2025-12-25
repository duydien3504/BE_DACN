package com.example.DACN.controller;

import com.example.DACN.dto.request.ShopRegisterRequest;
import com.example.DACN.dto.response.MyShopResponse;
import com.example.DACN.dto.response.ShopDetailResponse;
import com.example.DACN.dto.response.ShopListResponse;
import com.example.DACN.dto.response.ShopRegisterResponse;
import com.example.DACN.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Management", description = "APIs for shop registration and management")
public class ShopController {

    private final ShopService shopService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a new shop", description = "Allows a customer to register a new shop with logo upload and initiate PayPal payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Shop registration initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or file format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User already has a shop")
    })
    public ResponseEntity<ShopRegisterResponse> registerShop(
            Authentication authentication,
            @RequestParam("shopName") String shopName,
            @RequestParam(value = "shopDescription", required = false) String shopDescription,
            @RequestParam(value = "logo", required = false) MultipartFile logo) throws IOException {

        String email = authentication.getName();

        // Create request object
        ShopRegisterRequest request = new ShopRegisterRequest();
        request.setShopName(shopName);
        request.setShopDescription(shopDescription);

        ShopRegisterResponse response = shopService.registerShop(email, request, logo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/my-shop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update my shop", description = "Allows a seller to update their shop information with optional logo upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or file format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not a seller"),
            @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<com.example.DACN.dto.response.UpdateShopResponse> updateMyShop(
            Authentication authentication,
            @RequestParam(value = "shopName", required = false) String shopName,
            @RequestParam(value = "shopDescription", required = false) String shopDescription,
            @RequestParam(value = "logo", required = false) MultipartFile logo) throws IOException {

        String email = authentication.getName();

        // Create request object
        com.example.DACN.dto.request.UpdateShopRequest request = new com.example.DACN.dto.request.UpdateShopRequest();
        request.setShopName(shopName);
        request.setShopDescription(shopDescription);

        com.example.DACN.dto.response.UpdateShopResponse response = shopService.updateMyShop(email, request, logo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-shop")
    @Operation(summary = "Get my shop", description = "Allows a seller to retrieve their shop information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not a seller"),
            @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<MyShopResponse> getMyShop(Authentication authentication) {
        String email = authentication.getName();
        MyShopResponse response = shopService.getMyShop(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all shops", description = "Retrieve a list of all approved shops on the platform")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully")
    })
    public ResponseEntity<List<ShopListResponse>> getAllShops() {
        List<ShopListResponse> response = shopService.getAllShops();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shop_id}")
    @Operation(summary = "Get shop by ID", description = "Retrieve detailed information about a specific shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ShopDetailResponse> getShopById(@PathVariable("shop_id") Long shopId) {
        ShopDetailResponse response = shopService.getShopById(shopId);
        return ResponseEntity.ok(response);
    }
}
