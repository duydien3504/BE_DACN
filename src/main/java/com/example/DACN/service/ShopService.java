package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.PaypalWebhookRequest;
import com.example.DACN.dto.request.ShopRegisterRequest;
import com.example.DACN.dto.response.MyShopResponse;
import com.example.DACN.dto.response.ShopDetailResponse;
import com.example.DACN.dto.response.ShopListResponse;
import com.example.DACN.dto.response.ShopRegisterResponse;
import com.example.DACN.entity.Payment;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.repository.PaymentRepository;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RoleRepository roleRepository;
    private final PaypalService paypalService;
    private final CloudinaryService cloudinaryService;
    private final com.example.DACN.mapper.ShopMapper shopMapper;

    private static final BigDecimal SHOP_REGISTRATION_FEE_VND = new BigDecimal("50000");
    private static final String PAYMENT_STATUS_PENDING = "PENDING";
    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String PAYMENT_CAPTURE_COMPLETED = "PAYMENT.CAPTURE.COMPLETED";

    /**
     * Register a new shop and initiate PayPal payment
     */
    @Transactional
    public ShopRegisterResponse registerShop(String userEmail, ShopRegisterRequest request, MultipartFile logoFile)
            throws IOException {
        log.info("Starting shop registration for user: {}", userEmail);

        // Validate user exists
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has a shop
        shopRepository.findByUserUserId(user.getUserId()).ifPresent(existingShop -> {
            throw new DuplicateResourceException("User already has a shop");
        });

        // Verify user is a customer
        if (!RoleConstants.CUSTOMER.equals(user.getRole().getRoleName())) {
            throw new IllegalStateException("Only customers can register a shop");
        }

        // Upload logo to Cloudinary if provided
        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            log.info("Uploading logo to Cloudinary");
            logoUrl = cloudinaryService.uploadImage(logoFile);
            log.info("Logo uploaded successfully: {}", logoUrl);
        }

        // Create shop with pending approval
        Shop shop = new Shop();
        shop.setUser(user);
        shop.setShopName(request.getShopName());
        shop.setShopDescription(request.getShopDescription());
        shop.setLogoUrl(logoUrl);
        shop.setIsApproved(false);
        shop.setHasDeleted(false);
        shop.setRatingAvg(BigDecimal.ZERO);

        Shop savedShop = shopRepository.save(shop);
        log.info("Created shop with ID: {}", savedShop.getShopId());

        // Create PayPal order
        Map<String, String> paypalOrder = paypalService.createOrder(SHOP_REGISTRATION_FEE_VND);
        String paypalOrderId = paypalOrder.get("orderId");
        String approvalUrl = paypalOrder.get("approvalUrl");

        // Create payment record with pending status
        Payment payment = new Payment();
        payment.setOrder(null); // No order associated with shop registration
        payment.setTransactionCode(paypalOrderId);
        payment.setAmount(SHOP_REGISTRATION_FEE_VND);
        payment.setStatus(PAYMENT_STATUS_PENDING);
        payment.setPaymentTime(null); // Will be set when payment is completed

        paymentRepository.save(payment);
        log.info("Created payment record with transaction code: {}", paypalOrderId);

        // Build response
        ShopRegisterResponse response = new ShopRegisterResponse();
        response.setShopId(savedShop.getShopId());
        response.setPaypalOrderId(paypalOrderId);
        response.setPayUrl(approvalUrl);
        response.setMessage("Shop registration initiated. Please complete payment.");

        log.info("Shop registration initiated successfully for user: {}", userEmail);
        return response;
    }

    /**
     * Process PayPal webhook for payment completion
     */
    @Transactional
    public void processPaymentWebhook(PaypalWebhookRequest webhookRequest) {
        log.info("Processing PayPal webhook event: {}", webhookRequest.getEventType());

        // Only process PAYMENT.CAPTURE.COMPLETED events
        if (!PAYMENT_CAPTURE_COMPLETED.equals(webhookRequest.getEventType())) {
            log.info("Ignoring webhook event type: {}", webhookRequest.getEventType());
            return;
        }

        // Extract PayPal order ID with null-safe checks
        log.info("Webhook payload - Event Type: {}", webhookRequest.getEventType());
        log.info("Webhook payload - Resource: {}", webhookRequest.getResource());

        final String paypalOrderId;
        try {
            String tempOrderId = null;
            if (webhookRequest.getResource() != null) {
                log.info("Resource exists");
                if (webhookRequest.getResource().getSupplementaryData() != null) {
                    log.info("SupplementaryData exists");
                    if (webhookRequest.getResource().getSupplementaryData().getRelatedIds() != null) {
                        log.info("RelatedIds exists");
                        tempOrderId = webhookRequest.getResource().getSupplementaryData().getRelatedIds().getOrderId();
                        log.info("Extracted order ID: {}", tempOrderId);
                    } else {
                        log.warn("RelatedIds is null");
                    }
                } else {
                    log.warn("SupplementaryData is null");
                }
            } else {
                log.warn("Resource is null");
            }
            paypalOrderId = tempOrderId;
        } catch (Exception e) {
            log.error("Error extracting order ID from webhook", e);
            throw new IllegalArgumentException("Invalid webhook payload: missing order ID");
        }

        if (paypalOrderId == null) {
            log.error("PayPal order ID not found in webhook payload");
            throw new IllegalArgumentException("Invalid webhook payload: missing order ID");
        }

        log.info("Processing payment for PayPal order: {}", paypalOrderId);

        // Check idempotency - if payment already processed, skip
        Payment payment = paymentRepository.findByTransactionCode(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + paypalOrderId));

        if (PAYMENT_STATUS_SUCCESS.equals(payment.getStatus())) {
            log.info("Payment already processed for order: {}", paypalOrderId);
            return;
        }

        // Update payment status
        payment.setStatus(PAYMENT_STATUS_SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Updated payment status to SUCCESS for order: {}", paypalOrderId);

        // Find the shop by user - we need to get user from a shop-payment relationship
        // Since Payment doesn't have direct user link, we find the most recent
        // unapproved shop
        // In production, you should add a user_id field to Payment table for better
        // tracking
        Shop shop = shopRepository.findByIsApprovedFalseAndHasDeletedFalse().stream()
                .max((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("No pending shop found"));

        // Verify this shop belongs to a valid user
        if (shop.getUser() == null) {
            throw new ResourceNotFoundException("Shop has no associated user");
        }

        // Approve the shop
        shop.setIsApproved(true);
        shopRepository.save(shop);
        log.info("Approved shop with ID: {}", shop.getShopId());

        // Update user role to SELLER
        User user = shop.getUser();
        Role sellerRole = roleRepository.findByRoleName(RoleConstants.SELLER)
                .orElseThrow(() -> new ResourceNotFoundException("Seller role not found"));

        user.setRole(sellerRole);
        userRepository.save(user);
        log.info("Updated user {} role to SELLER", user.getUserId());

        log.info("Successfully processed payment webhook for order: {}", paypalOrderId);
    }

    /**
     * Update shop information for the authenticated seller
     */
    @Transactional
    public com.example.DACN.dto.response.UpdateShopResponse updateMyShop(
            String userEmail,
            com.example.DACN.dto.request.UpdateShopRequest request,
            MultipartFile logoFile) throws IOException {

        log.info("Updating shop for user: {}", userEmail);

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Verify user is a seller
        if (!RoleConstants.SELLER.equals(user.getRole().getRoleName())) {
            throw new IllegalStateException("Only sellers can update shop information");
        }

        // Find shop by user
        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for user: " + userEmail));

        // Update shop name if provided
        if (request.getShopName() != null && !request.getShopName().isBlank()) {
            shop.setShopName(request.getShopName());
            log.info("Updated shop name to: {}", request.getShopName());
        }

        // Update shop description if provided
        if (request.getShopDescription() != null && !request.getShopDescription().isBlank()) {
            shop.setShopDescription(request.getShopDescription());
            log.info("Updated shop description");
        }

        // Upload new logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            log.info("Uploading new logo to Cloudinary");
            String logoUrl = cloudinaryService.uploadImage(logoFile);
            shop.setLogoUrl(logoUrl);
            log.info("Logo uploaded successfully: {}", logoUrl);
        }

        // Update timestamp
        shop.setUpdatedAt(LocalDateTime.now());

        // Save shop
        Shop updatedShop = shopRepository.save(shop);
        log.info("Shop updated successfully with ID: {}", updatedShop.getShopId());

        // Map to response using ShopMapper
        return shopMapper.toUpdateShopResponse(updatedShop);
    }

    /**
     * Get shop information for the authenticated seller
     */
    public MyShopResponse getMyShop(String userEmail) {
        log.info("Retrieving shop for user: {}", userEmail);

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify user is a seller
        if (!RoleConstants.SELLER.equals(user.getRole().getRoleName())) {
            log.warn("User {} attempted to access shop but is not a seller", userEmail);
            throw new UnauthorizedException("Only sellers can access shop information");
        }

        // Find shop by user ID (non-deleted only)
        Shop shop = shopRepository.findByUserUserIdAndHasDeletedFalse(user.getUserId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        log.info("Successfully retrieved shop with ID: {}", shop.getShopId());

        // Map to response using ShopMapper
        return shopMapper.toMyShopResponse(shop);
    }

    /**
     * Get all approved shops for public listing
     */
    public List<ShopListResponse> getAllShops() {
        log.info("Retrieving all approved shops");

        // Get all approved and non-deleted shops
        List<Shop> shops = shopRepository.findByIsApprovedTrueAndHasDeletedFalse();

        log.info("Found {} approved shops", shops.size());

        // Map to response list using ShopMapper
        return shops.stream()
                .map(shopMapper::toShopListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get shop by ID for public access
     */
    public ShopDetailResponse getShopById(Long shopId) {
        log.info("Retrieving shop with ID: {}", shopId);

        // Find shop by ID (non-deleted only)
        Shop shop = shopRepository.findByShopIdAndHasDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        // Verify shop is approved for public access
        if (!shop.getIsApproved()) {
            log.warn("Attempted to access unapproved shop with ID: {}", shopId);
            throw new ResourceNotFoundException("Shop not found");
        }

        log.info("Successfully retrieved shop with ID: {}", shopId);

        // Map to response using ShopMapper
        return shopMapper.toShopDetailResponse(shop);
    }
}
