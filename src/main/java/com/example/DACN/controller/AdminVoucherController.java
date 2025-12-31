package com.example.DACN.controller;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "Admin Voucher Management", description = "APIs for admins to manage platform vouchers")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create platform voucher", description = "Create a new platform-wide voucher (shop_id = null). Only admins can create platform vouchers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Platform voucher created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or voucher code already exists"),
            @ApiResponse(responseCode = "403", description = "Unauthorized - not an admin")
    })
    public ResponseEntity<CreateVoucherResponse> createPlatformVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {
        CreateVoucherResponse response = voucherService.createPlatformVoucher(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
