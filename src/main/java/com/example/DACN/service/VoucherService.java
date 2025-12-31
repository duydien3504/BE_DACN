package com.example.DACN.service;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.VoucherMapper;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    @Transactional
    public CreateVoucherResponse createVoucher(CreateVoucherRequest request, String userEmail) {
        log.info("Creating voucher with code: {} by user: {}", request.getCode(), userEmail);

        // 1. Validate voucher code is unique
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Voucher code already exists: " + request.getCode());
        }

        // 2. Find user's shop
        com.example.DACN.entity.User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for user"));

        if (!shop.getIsApproved()) {
            throw new IllegalStateException("Shop is not approved. Cannot create vouchers.");
        }

        // 3. Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // 4. Validate discount value based on type
        if ("PERCENT".equals(request.getDiscountType())) {
            if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Percentage discount must be greater than 0");
            }
        }

        // 5. Create voucher entity
        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setShop(shop);
        voucher.setHasDeleted(false);

        // 6. Save voucher
        Voucher savedVoucher = voucherRepository.save(voucher);

        log.info("Voucher created successfully with ID: {}", savedVoucher.getVoucherId());

        // 7. Build response
        CreateVoucherResponse response = voucherMapper.toCreateVoucherResponse(savedVoucher);
        response.setMessage("Voucher created successfully");

        return response;
    }

    @Transactional
    public CreateVoucherResponse createPlatformVoucher(CreateVoucherRequest request) {
        log.info("Creating platform voucher with code: {} by admin", request.getCode());

        // 1. Validate voucher code is unique
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Voucher code already exists: " + request.getCode());
        }

        // 2. Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // 3. Validate discount value based on type
        if ("PERCENT".equals(request.getDiscountType())) {
            if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Percentage discount must be greater than 0");
            }
        }

        // 4. Create platform voucher entity (shop = null for platform vouchers)
        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setShop(null); // Platform voucher - no specific shop
        voucher.setHasDeleted(false);

        // 5. Save voucher
        Voucher savedVoucher = voucherRepository.save(voucher);

        log.info("Platform voucher created successfully with ID: {}", savedVoucher.getVoucherId());

        // 6. Build response
        CreateVoucherResponse response = voucherMapper.toCreateVoucherResponse(savedVoucher);
        response.setMessage("Platform voucher created successfully");

        return response;
    }

    @Transactional(readOnly = true)
    public java.util.List<com.example.DACN.dto.response.VoucherResponse> getShopVouchers(Long shopId) {
        log.info("Retrieving active vouchers for shop ID: {}", shopId);

        // 1. Verify shop exists and is approved
        Shop shop = shopRepository.findByShopIdAndHasDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        if (!shop.getIsApproved()) {
            throw new IllegalArgumentException("Shop is not approved");
        }

        // 2. Get active active vouchers
        java.util.List<Voucher> vouchers = voucherRepository.findActiveShopVouchers(shopId,
                java.time.LocalDateTime.now());

        // 3. Map to response DTOs
        return vouchers.stream()
                .map(voucherMapper::toVoucherResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}
