package com.example.DACN.controller;

import com.example.DACN.dto.response.CollectVoucherResponse;
import com.example.DACN.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Customer Voucher Management", description = "APIs for customers to collect and manage vouchers")
public class CustomerVoucherController {

    private final VoucherService voucherService;

    @PostMapping("/collect/{voucher_id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Collect voucher", description = "Allows a customer to collect a voucher by its ID. The voucher must be active, available, and not already collected by the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Voucher collected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - voucher expired, out of stock, not yet available, or already collected"),
            @ApiResponse(responseCode = "403", description = "Unauthorized - not a customer"),
            @ApiResponse(responseCode = "404", description = "Voucher not found"),
            @ApiResponse(responseCode = "409", description = "Voucher already collected by user")
    })
    public ResponseEntity<CollectVoucherResponse> collectVoucher(@PathVariable("voucher_id") Long voucherId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        CollectVoucherResponse response = voucherService.collectVoucher(voucherId, userEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
