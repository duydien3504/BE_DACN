package com.example.DACN.controller;

import com.example.DACN.dto.request.AddAddressRequest;
import com.example.DACN.dto.response.AddAddressResponse;
import com.example.DACN.dto.response.AddressListResponse;
import com.example.DACN.dto.response.DeleteAddressResponse;
import com.example.DACN.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "User address management APIs")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add new address", description = "Add a new delivery address for the authenticated customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address added successfully", content = @Content(schema = @Schema(implementation = AddAddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only customers can add addresses"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AddAddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody AddAddressRequest request) {

        String email = authentication.getName();
        AddAddressResponse response = addressService.addAddress(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all addresses", description = "Get all delivery addresses for the authenticated customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressListResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only customers can view addresses"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<AddressListResponse>> getAddresses(Authentication authentication) {
        String email = authentication.getName();
        List<AddressListResponse> addresses = addressService.getAddresses(email);
        return ResponseEntity.ok(addresses);
    }

    @DeleteMapping("/{user_address_id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete address", description = "Soft delete an address for the authenticated customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address deleted successfully", content = @Content(schema = @Schema(implementation = DeleteAddressResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot delete address of another user"),
            @ApiResponse(responseCode = "404", description = "Address or User not found")
    })
    public ResponseEntity<DeleteAddressResponse> deleteAddress(
            Authentication authentication,
            @Parameter(description = "ID of the address to delete") @PathVariable("user_address_id") Long userAddressId) {

        String email = authentication.getName();
        DeleteAddressResponse response = addressService.deleteAddress(email, userAddressId);
        return ResponseEntity.ok(response);
    }
}
