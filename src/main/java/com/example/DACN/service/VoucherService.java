package com.example.DACN.service;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CollectVoucherResponse;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.dto.response.DeleteVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.UserVoucher;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.mapper.VoucherMapper;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.repository.UserVoucherRepository;
import com.example.DACN.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final UserVoucherRepository userVoucherRepository;
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

    @Transactional
    public CollectVoucherResponse collectVoucher(Long voucherId, String userEmail) {
        log.info("User {} attempting to collect voucher ID: {}", userEmail, voucherId);

        // 1. Find user
        com.example.DACN.entity.User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Find voucher and validate it exists and is not deleted
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with ID: " + voucherId));

        if (voucher.getHasDeleted()) {
            throw new IllegalArgumentException("Voucher is no longer available");
        }

        // 3. Check if user already collected this voucher
        if (userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(user.getUserId(), voucherId)) {
            throw new DuplicateResourceException("You have already collected this voucher");
        }

        // 4. Validate voucher is currently active (within date range)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartDate())) {
            throw new IllegalArgumentException("Voucher is not yet available");
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw new IllegalArgumentException("Voucher has expired");
        }

        // 5. Check if voucher has available quantity
        if (voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Voucher is out of stock");
        }

        // 6. Create UserVoucher entity
        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setUser(user);
        userVoucher.setVoucher(voucher);
        userVoucher.setIsUsed(false);
        userVoucher.setUsedAtOrder(null);

        // 7. Decrease voucher quantity
        voucher.setQuantity(voucher.getQuantity() - 1);

        // 8. Save both entities
        voucherRepository.save(voucher);
        UserVoucher savedUserVoucher = userVoucherRepository.save(userVoucher);

        log.info("User {} successfully collected voucher ID: {}. Remaining quantity: {}",
                userEmail, voucherId, voucher.getQuantity());

        // 9. Build response
        CollectVoucherResponse response = voucherMapper.toCollectVoucherResponse(savedUserVoucher);
        response.setMessage("Voucher collected successfully");

        return response;
    }

    @Transactional
    public DeleteVoucherResponse deleteVoucher(Long voucherId, String userEmail) {
        log.info("Seller {} attempting to delete voucher ID: {}", userEmail, voucherId);

        // 1. Find user
        com.example.DACN.entity.User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Find user's shop
        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for user"));

        // 3. Find voucher and validate it exists and is not already deleted
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with ID: " + voucherId));

        if (voucher.getHasDeleted()) {
            throw new ResourceNotFoundException("Voucher not found with ID: " + voucherId);
        }

        // 4. Validate voucher belongs to seller's shop
        if (voucher.getShop() == null || !voucher.getShop().getShopId().equals(shop.getShopId())) {
            throw new UnauthorizedException("You can only delete your own shop vouchers");
        }

        // 5. Soft delete the voucher
        voucher.setHasDeleted(true);
        Voucher deletedVoucher = voucherRepository.save(voucher);

        log.info("Voucher ID: {} soft deleted by seller: {}", voucherId, userEmail);

        // 6. Build response
        DeleteVoucherResponse response = voucherMapper.toDeleteVoucherResponse(deletedVoucher);
        response.setMessage("Voucher deleted successfully");

        return response;
    }

    @Transactional
    public DeleteVoucherResponse deleteVoucherByAdmin(Long voucherId) {
        log.info("Admin attempting to delete voucher ID: {}", voucherId);

        // 1. Find voucher and validate it exists and is not already deleted
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with ID: " + voucherId));

        if (voucher.getHasDeleted()) {
            throw new ResourceNotFoundException("Voucher not found with ID: " + voucherId);
        }

        // 2. Soft delete the voucher (admin can delete any voucher - platform or shop)
        voucher.setHasDeleted(true);
        Voucher deletedVoucher = voucherRepository.save(voucher);

        log.info("Voucher ID: {} soft deleted by admin", voucherId);

        // 3. Build response
        DeleteVoucherResponse response = voucherMapper.toDeleteVoucherResponse(deletedVoucher);
        response.setMessage("Voucher deleted successfully");

        return response;
    }
}
