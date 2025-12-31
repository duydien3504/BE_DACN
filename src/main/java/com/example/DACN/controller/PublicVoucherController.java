package com.example.DACN.controller;

import com.example.DACN.dto.response.VoucherResponse;
import com.example.DACN.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Public Voucher Management", description = "Public APIs for retrieving vouchers")
public class PublicVoucherController {

    private final VoucherService voucherService;

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "Get shop active vouchers", description = "Retrieve all active and available vouchers for a specific shop. Publicly accessible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vouchers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Shop not found or not approved")
    })
    public ResponseEntity<List<VoucherResponse>> getShopVouchers(@PathVariable Long shopId) {
        List<VoucherResponse> vouchers = voucherService.getShopVouchers(shopId);
        return ResponseEntity.ok(vouchers);
    }
}
