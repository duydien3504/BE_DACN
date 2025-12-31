package com.example.DACN.service;

import com.example.DACN.dto.request.CreateOrderRequest;
import com.example.DACN.dto.response.CreateOrderResponse;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.OrderMapper;
import com.example.DACN.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserAddressRepository userAddressRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final PaypalService paypalService;
    private final OrderMapper orderMapper;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, UUID userId) {
        log.info("Creating order for user: {} from shop: {}", userId, request.getShopId());

        // 1. Validate shop exists and is active
        Shop shop = shopRepository.findByShopIdAndHasDeletedFalse(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + request.getShopId()));

        if (!shop.getIsApproved()) {
            throw new IllegalArgumentException("Shop is not approved");
        }

        // 2. Validate address exists and belongs to user
        UserAddress address = userAddressRepository.findById(request.getAddressId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Address not found with ID: " + request.getAddressId()));

        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to the user");
        }

        if (address.getHasDeleted()) {
            throw new IllegalArgumentException("Address has been deleted");
        }

        // 3. Validate products and calculate amounts
        List<OrderItemData> orderItemsData = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findByProductIdAndHasDeletedFalse(item.getProductId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Product not found with ID: " + item.getProductId()));

            // Validate product belongs to the shop
            if (!product.getShop().getShopId().equals(request.getShopId())) {
                throw new IllegalArgumentException(
                        "Product " + item.getProductId() + " does not belong to shop " + request.getShopId());
            }

            // Validate product is active
            if (!"Active".equals(product.getStatus())) {
                throw new IllegalArgumentException("Product " + product.getName() + " is not active");
            }

            // Validate stock
            if (product.getStockQuantity() < item.getQty()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockQuantity() + ", Requested: " + item.getQty());
            }

            // Calculate item total
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQty()));
            totalAmount = totalAmount.add(itemTotal);

            orderItemsData.add(new OrderItemData(product, item.getQty(), product.getPrice()));
        }

        // 4. Apply voucher discount if provided
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        UserVoucher userVoucher = null;

        if (request.getVoucherId() != null) {
            userVoucher = userVoucherRepository
                    .findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, request.getVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher not found or already used"));

            Voucher voucher = userVoucher.getVoucher();

            // Validate voucher is not deleted
            if (voucher.getHasDeleted()) {
                throw new IllegalArgumentException("Voucher has been deleted");
            }

            // Validate voucher date range
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
                throw new IllegalArgumentException("Voucher is not valid at this time");
            }

            // Validate minimum order value
            if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new IllegalArgumentException("Order total does not meet minimum order value for voucher");
            }

            // Validate voucher belongs to shop or is platform voucher
            if (voucher.getShop() != null && !voucher.getShop().getShopId().equals(request.getShopId())) {
                throw new IllegalArgumentException("Voucher does not belong to this shop");
            }

            // Calculate discount
            if ("PERCENT".equals(voucher.getDiscountType())) {
                voucherDiscount = totalAmount.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (voucher.getMaxDiscountAmount() != null
                        && voucherDiscount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    voucherDiscount = voucher.getMaxDiscountAmount();
                }
            } else if ("FIXED".equals(voucher.getDiscountType())) {
                voucherDiscount = voucher.getDiscountValue();
            }

            // Ensure discount doesn't exceed total
            if (voucherDiscount.compareTo(totalAmount) > 0) {
                voucherDiscount = totalAmount;
            }
        }

        // 5. Calculate final amount
        BigDecimal finalAmount = totalAmount.subtract(voucherDiscount);

        // 6. Create order
        Order order = new Order();
        order.setUser(address.getUser());
        order.setShop(shop);
        order.setTotalAmount(totalAmount);
        order.setVoucherDiscount(voucherDiscount);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setHasDeleted(false);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getOrderId());

        // 7. Create order items and update product stock
        for (OrderItemData itemData : orderItemsData) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(itemData.getProduct());
            orderItem.setQuantity(itemData.getQuantity());
            orderItem.setPriceAtPurchase(itemData.getPrice());
            orderItemRepository.save(orderItem);

            // Update product stock and sold count
            Product product = itemData.getProduct();
            product.setStockQuantity(product.getStockQuantity() - itemData.getQuantity());
            product.setSoldCount(product.getSoldCount() + itemData.getQuantity());
            productRepository.save(product);
            log.info("Updated product {} - Stock: {}, Sold: {}",
                    product.getProductId(), product.getStockQuantity(), product.getSoldCount());
        }

        // 8. Create order status history
        OrderStatusHistory statusHistory = new OrderStatusHistory();
        statusHistory.setOrder(savedOrder);
        statusHistory.setStatus("Pending");
        statusHistory.setDescription("Order created and pending confirmation");
        orderStatusHistoryRepository.save(statusHistory);
        log.info("Order status history created: Pending");

        // 9. Mark voucher as used if applicable
        if (userVoucher != null) {
            userVoucher.setIsUsed(true);
            userVoucher.setUsedAtOrder(savedOrder);
            userVoucherRepository.save(userVoucher);
            log.info("Voucher {} marked as used", request.getVoucherId());
        }

        // 10. Delete cart items for ordered products
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            cartItemRepository.deleteByCartCartIdAndProductProductId(
                    cart.getCartId(),
                    item.getProductId());
            log.info("Removed product {} from cart", item.getProductId());
        }

        // 11. Create PayPal payment if payment method is PAYPAL
        CreateOrderResponse response = orderMapper.toCreateOrderResponse(savedOrder);

        if ("PAYPAL".equals(request.getPaymentMethod())) {
            try {
                log.info("Creating PayPal payment for order: {}", savedOrder.getOrderId());
                // Pass orderId to PayPal for tracking and callback
                java.util.Map<String, String> paypalResult = paypalService.createOrder(finalAmount,
                        savedOrder.getOrderId());
                String approvalUrl = paypalResult.get("approvalUrl");
                response.setPaymentUrl(approvalUrl);
                log.info("PayPal payment created successfully with approval URL");
            } catch (Exception e) {
                log.error("Failed to create PayPal payment for order: {}", savedOrder.getOrderId(), e);
                throw new IllegalStateException("Failed to create PayPal payment: " + e.getMessage());
            }
        } else {
            // COD payment - no payment URL needed
            response.setPaymentUrl(null);
            log.info("COD payment selected - no payment URL required");
        }

        return response;
    }

    @Transactional
    public void cancelOrder(Long orderId, String userEmail) {
        log.info("Cancelling order: {} by user: {}", orderId, userEmail);

        // 1. Find order
        Order order = orderRepository.findByOrderIdAndHasDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // 2. Verify user has permission to cancel
        // Customer can cancel their own orders, Seller can cancel orders from their
        // shop
        boolean isCustomer = order.getUser().getEmail().equals(userEmail);
        boolean isSeller = order.getShop().getUser().getEmail().equals(userEmail);

        if (!isCustomer && !isSeller) {
            throw new IllegalArgumentException("You do not have permission to cancel this order");
        }

        // 3. Get latest order status
        OrderStatusHistory latestStatus = orderStatusHistoryRepository
                .findTopByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order status not found"));

        // 4. Check if order can be cancelled (not yet Shipping)
        String currentStatus = latestStatus.getStatus();
        if ("Shipping".equals(currentStatus) || "Delivered".equals(currentStatus)
                || "Completed".equals(currentStatus)) {
            throw new IllegalStateException("Cannot cancel order with status: " + currentStatus);
        }

        // 5. Mark order as deleted
        order.setHasDeleted(true);
        orderRepository.save(order);
        log.info("Marked order {} as deleted", orderId);

        // 6. Restore product stock
        orderItemRepository.findByOrderOrderId(orderId).forEach(orderItem -> {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();

            // Restore stock
            product.setStockQuantity(product.getStockQuantity() + quantity);
            // Decrease sold count
            product.setSoldCount(product.getSoldCount() - quantity);
            productRepository.save(product);

            log.info("Restored stock for product {}: +{} units", product.getProductId(), quantity);
        });

        // 7. Create status history for cancellation
        OrderStatusHistory cancelHistory = new OrderStatusHistory();
        cancelHistory.setOrder(order);
        cancelHistory.setStatus("Cancelled");
        cancelHistory.setDescription("Order cancelled by " + (isCustomer ? "customer" : "seller"));
        orderStatusHistoryRepository.save(cancelHistory);

        log.info("Order {} cancelled successfully", orderId);
    }

    @Transactional(readOnly = true)
    public List<com.example.DACN.dto.response.CustomerOrderResponse> getCustomerOrders(UUID userId) {
        log.info("Retrieving orders for customer: {}", userId);

        // Get all orders for customer
        List<Order> orders = orderRepository.findByUserUserIdAndHasDeletedFalseOrderByCreatedAtDesc(userId);

        // Map to response DTOs
        return orders.stream()
                .map(order -> {
                    com.example.DACN.dto.response.CustomerOrderResponse response = orderMapper
                            .toCustomerOrderResponse(order);

                    // Get latest status
                    OrderStatusHistory latestStatus = orderStatusHistoryRepository
                            .findTopByOrderOrderIdOrderByCreatedAtDesc(order.getOrderId())
                            .orElse(null);

                    if (latestStatus != null) {
                        response.setCurrentStatus(latestStatus.getStatus());
                    } else {
                        response.setCurrentStatus("Unknown");
                    }

                    // Get item count
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    response.setItemCount(items.size());

                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.example.DACN.dto.response.SellerOrderResponse> getSellerOrders(Long shopId) {
        log.info("Retrieving orders for shop: {}", shopId);

        // Get all orders for shop
        List<Order> orders = orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId);

        // Map to response DTOs
        return orders.stream()
                .map(order -> {
                    com.example.DACN.dto.response.SellerOrderResponse response = orderMapper
                            .toSellerOrderResponse(order);

                    // Get latest status
                    OrderStatusHistory latestStatus = orderStatusHistoryRepository
                            .findTopByOrderOrderIdOrderByCreatedAtDesc(order.getOrderId())
                            .orElse(null);

                    if (latestStatus != null) {
                        response.setCurrentStatus(latestStatus.getStatus());
                    } else {
                        response.setCurrentStatus("Unknown");
                    }

                    // Get item count
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    response.setItemCount(items.size());

                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public com.example.DACN.dto.response.UpdateOrderStatusResponse updateOrderStatus(Long orderId, String newStatus,
            String userEmail) {
        log.info("Updating order status for order: {} to status: {} by user: {}", orderId, newStatus, userEmail);

        // 1. Find order
        Order order = orderRepository.findByOrderIdAndHasDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // 2. Verify user is the seller of the shop
        if (!order.getShop().getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You do not have permission to update this order");
        }

        // 3. Get current status
        OrderStatusHistory currentStatus = orderStatusHistoryRepository
                .findTopByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order status not found"));

        // 4. Validate status transition
        String currentStatusValue = currentStatus.getStatus();
        validateStatusTransition(currentStatusValue, newStatus);

        // 5. Create new status history entry
        OrderStatusHistory newStatusHistory = new OrderStatusHistory();
        newStatusHistory.setOrder(order);
        newStatusHistory.setStatus(newStatus);
        newStatusHistory.setDescription(generateStatusDescription(newStatus));
        OrderStatusHistory savedHistory = orderStatusHistoryRepository.save(newStatusHistory);

        log.info("Order {} status updated from {} to {}", orderId, currentStatusValue, newStatus);

        // 6. Build response
        return com.example.DACN.dto.response.UpdateOrderStatusResponse.builder()
                .orderId(orderId)
                .status(newStatus)
                .description(savedHistory.getDescription())
                .updatedAt(savedHistory.getCreatedAt())
                .message("Order status updated successfully")
                .build();
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Valid transitions:
        // Pending/Paid -> Shipping
        // Shipping -> Delivered

        if ("Shipping".equals(newStatus)) {
            if (!"Pending".equals(currentStatus) && !"Paid".equals(currentStatus)) {
                throw new IllegalStateException("Cannot change status to Shipping from " + currentStatus +
                        ". Order must be in Pending or Paid status");
            }
        } else if ("Delivered".equals(newStatus)) {
            if (!"Shipping".equals(currentStatus)) {
                throw new IllegalStateException("Cannot change status to Delivered from " + currentStatus +
                        ". Order must be in Shipping status");
            }
        }

        // Cannot update if order is already delivered, completed, or cancelled
        if ("Delivered".equals(currentStatus) || "Completed".equals(currentStatus)
                || "Cancelled".equals(currentStatus)) {
            throw new IllegalStateException("Cannot update order with status: " + currentStatus);
        }
    }

    private String generateStatusDescription(String status) {
        if ("Shipping".equals(status)) {
            return "Order is being shipped to customer";
        } else if ("Delivered".equals(status)) {
            return "Order has been delivered to customer";
        } else {
            return "Order status updated to " + status;
        }
    }

    @Transactional(readOnly = true)
    public List<com.example.DACN.dto.response.OrderStatusHistoryResponse> getOrderHistory(Long orderId,
            String userEmail) {
        log.info("Retrieving order history for order: {} by user: {}", orderId, userEmail);

        // 1. Find order
        Order order = orderRepository.findByOrderIdAndHasDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // 2. Verify user has permission to view (customer or seller)
        boolean isCustomer = order.getUser().getEmail().equals(userEmail);
        boolean isSeller = order.getShop().getUser().getEmail().equals(userEmail);

        if (!isCustomer && !isSeller) {
            throw new IllegalArgumentException("You do not have permission to view this order history");
        }

        // 3. Get order status history ordered by creation date descending
        List<OrderStatusHistory> historyList = orderStatusHistoryRepository
                .findByOrderOrderIdOrderByCreatedAtDesc(orderId);

        // 4. Map to response DTOs
        return historyList.stream()
                .map(orderMapper::toOrderStatusHistoryResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // Helper class to hold order item data
    private static class OrderItemData {
        private final Product product;
        private final Integer quantity;
        private final BigDecimal price;

        public OrderItemData(Product product, Integer quantity, BigDecimal price) {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
        }

        public Product getProduct() {
            return product;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
