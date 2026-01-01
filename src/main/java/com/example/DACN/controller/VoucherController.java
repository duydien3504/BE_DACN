package com.example.DACN.controller;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.dto.response.DeleteVoucherResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/vouchers")
@RequiredArgsConstructor
@Tag(name = "Seller Voucher Management", description = "APIs for sellers to manage vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create voucher", description = "Create a new voucher for the seller's shop. Only approved shops can create vouchers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Voucher created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or voucher code already exists"),
            @ApiResponse(responseCode = "403", description = "Unauthorized - not a seller or shop not approved"),
            @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<CreateVoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        CreateVoucherResponse response = voucherService.createVoucher(request, userEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{voucher_id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete voucher", description = "Soft delete a voucher. Sellers can only delete their own shop vouchers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Voucher deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized - not a seller or trying to delete another shop's voucher"),
            @ApiResponse(responseCode = "404", description = "Voucher not found or shop not found")
    })
    public ResponseEntity<DeleteVoucherResponse> deleteVoucher(@PathVariable("voucher_id") Long voucherId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        DeleteVoucherResponse response = voucherService.deleteVoucher(voucherId, userEmail);

        return ResponseEntity.ok(response);
    }
}
