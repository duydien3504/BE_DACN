package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PaypalService paypalService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void capturePayPalPayment(String token, Long orderId) {
        log.info("Capturing PayPal payment for token: {} and order: {}", token, orderId);

        try {
            // 1. Get order
            Order order = orderRepository.findByOrderIdAndHasDeletedFalse(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

            // 2. Verify payment method
            if (!"PAYPAL".equals(order.getPaymentMethod())) {
                throw new IllegalArgumentException("Order payment method is not PAYPAL");
            }

            // 3. Check if payment already exists
            if (paymentRepository.findByOrderOrderId(orderId).isPresent()) {
                log.warn("Payment already exists for order: {}", orderId);
                return;
            }

            // 4. Capture payment from PayPal
            String accessToken = paypalService.getAccessToken();
            String captureUrl = getPayPalApiBase() + "/v2/checkout/orders/" + token + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<String> response = restTemplate.postForEntity(captureUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String status = jsonNode.get("status").asText();

                if ("COMPLETED".equals(status)) {
                    // Extract transaction details
                    JsonNode purchaseUnits = jsonNode.get("purchase_units");
                    JsonNode payments = purchaseUnits.get(0).get("payments");
                    JsonNode captures = payments.get("captures");
                    JsonNode capture = captures.get(0);

                    String transactionId = capture.get("id").asText();
                    BigDecimal amount = new BigDecimal(capture.get("amount").get("value").asText());

                    // 5. Create payment record
                    Payment payment = new Payment();
                    payment.setOrder(order);
                    payment.setTransactionCode(transactionId);
                    payment.setAmount(order.getFinalAmount());
                    payment.setStatus("Success");
                    payment.setPaymentTime(LocalDateTime.now());
                    paymentRepository.save(payment);

                    log.info("Payment record created for order: {} with transaction: {}", orderId, transactionId);

                    // 6. Update order status
                    OrderStatusHistory statusHistory = new OrderStatusHistory();
                    statusHistory.setOrder(order);
                    statusHistory.setStatus("Paid");
                    statusHistory.setDescription("Payment confirmed via PayPal. Transaction ID: " + transactionId);
                    orderStatusHistoryRepository.save(statusHistory);

                    log.info("Order status updated to Paid for order: {}", orderId);
                } else {
                    throw new IllegalStateException("PayPal payment status is not COMPLETED: " + status);
                }
            } else {
                throw new IllegalStateException("Failed to capture PayPal payment");
            }
        } catch (Exception e) {
            log.error("Error capturing PayPal payment for order: {}", orderId, e);
            throw new IllegalStateException("Failed to capture PayPal payment: " + e.getMessage());
        }
    }

    @Transactional
    public void processPayPalWebhook(Map<String, Object> webhookData) {
        log.info("Processing PayPal webhook");

        try {
            String eventType = (String) webhookData.get("event_type");

            if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
                Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");
                String transactionId = (String) resource.get("id");

                // Check if payment already processed
                if (paymentRepository.existsByTransactionCode(transactionId)) {
                    log.info("Payment already processed for transaction: {}", transactionId);
                    return;
                }

                // Extract custom_id which should contain order_id
                String customId = (String) resource.get("custom_id");
                if (customId != null) {
                    Long orderId = Long.parseLong(customId);

                    Order order = orderRepository.findByOrderIdAndHasDeletedFalse(orderId)
                            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

                    // Create payment record
                    Map<String, Object> amountData = (Map<String, Object>) resource.get("amount");
                    BigDecimal amount = new BigDecimal((String) amountData.get("value"));

                    Payment payment = new Payment();
                    payment.setOrder(order);
                    payment.setTransactionCode(transactionId);
                    payment.setAmount(amount);
                    payment.setStatus("Success");
                    payment.setPaymentTime(LocalDateTime.now());
                    paymentRepository.save(payment);

                    // Update order status
                    OrderStatusHistory statusHistory = new OrderStatusHistory();
                    statusHistory.setOrder(order);
                    statusHistory.setStatus("Paid");
                    statusHistory
                            .setDescription("Payment confirmed via PayPal webhook. Transaction ID: " + transactionId);
                    orderStatusHistoryRepository.save(statusHistory);

                    log.info("Webhook processed successfully for order: {}", orderId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing PayPal webhook", e);
            throw new IllegalStateException("Failed to process webhook: " + e.getMessage());
        }
    }

    @Transactional
    public void captureShopRegistrationPayment(String token) {
        log.info("Capturing PayPal payment for shop registration with token: {}", token);

        try {
            // 1. Capture payment from PayPal
            String accessToken = paypalService.getAccessToken();
            String captureUrl = getPayPalApiBase() + "/v2/checkout/orders/" + token + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<String> response = restTemplate.postForEntity(captureUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String status = jsonNode.get("status").asText();

                if ("COMPLETED".equals(status)) {
                    // Extract transaction details
                    JsonNode purchaseUnits = jsonNode.get("purchase_units");
                    JsonNode payments = purchaseUnits.get(0).get("payments");
                    JsonNode captures = payments.get("captures");
                    JsonNode capture = captures.get(0);

                    String transactionId = capture.get("id").asText();

                    // 2. Find payment record by PayPal order ID (token)
                    Payment payment = paymentRepository.findByTransactionCode(token)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Payment not found for PayPal order: " + token));

                    // Check if already processed
                    if ("SUCCESS".equals(payment.getStatus())) {
                        log.info("Payment already processed for token: {}", token);
                        return;
                    }

                    // 3. Update payment record
                    payment.setTransactionCode(transactionId); // Update with actual transaction ID
                    payment.setStatus("SUCCESS");
                    payment.setPaymentTime(LocalDateTime.now());
                    paymentRepository.save(payment);

                    log.info("Payment record updated for shop registration with transaction: {}", transactionId);

                    // 4. Find and approve the most recent unapproved shop
                    // Since Payment doesn't have shop_id, we find the latest unapproved shop
                    Shop shop = shopRepository.findByIsApprovedFalseAndHasDeletedFalse().stream()
                            .max((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                            .orElseThrow(() -> new ResourceNotFoundException("No pending shop found for approval"));

                    // Verify shop has a user
                    if (shop.getUser() == null) {
                        throw new ResourceNotFoundException("Shop has no associated user");
                    }

                    // 5. Approve the shop
                    shop.setIsApproved(true);
                    shopRepository.save(shop);
                    log.info("Approved shop with ID: {} for user: {}", shop.getShopId(), shop.getUser().getEmail());

                    // 6. Update user role to SELLER
                    User user = shop.getUser();
                    Role sellerRole = roleRepository.findByRoleName(RoleConstants.SELLER)
                            .orElseThrow(() -> new ResourceNotFoundException("Seller role not found"));

                    user.setRole(sellerRole);
                    userRepository.save(user);
                    log.info("Updated user {} role to SELLER", user.getUserId());

                } else {
                    throw new IllegalStateException("PayPal payment status is not COMPLETED: " + status);
                }
            } else {
                throw new IllegalStateException("Failed to capture PayPal payment");
            }
        } catch (Exception e) {
            log.error("Error capturing PayPal payment for shop registration", e);
            throw new IllegalStateException("Failed to capture PayPal payment: " + e.getMessage());
        }
    }

    private String getPayPalApiBase() {
        // This should match PaypalService mode configuration
        return "https://api-m.sandbox.paypal.com";
    }
}
