package com.example.DACN.controller;

import com.example.DACN.dto.request.CreateOrderRequest;
import com.example.DACN.dto.request.UpdateOrderStatusRequest;
import com.example.DACN.dto.response.CancelOrderResponse;
import com.example.DACN.dto.response.CreateOrderResponse;
import com.example.DACN.dto.response.CustomerOrderResponse;
import com.example.DACN.dto.response.OrderStatusHistoryResponse;
import com.example.DACN.dto.response.SellerOrderResponse;
import com.example.DACN.dto.response.UpdateOrderStatusResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.service.OrderService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

        private final OrderService orderService;
        private final UserRepository userRepository;
        private final ShopRepository shopRepository;

        @PostMapping
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Create order from cart", description = "Create a new order from cart items with optional voucher discount")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Order created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
                        @ApiResponse(responseCode = "404", description = "Shop, product, address, or voucher not found"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized access")
        })
        public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                CreateOrderResponse response = orderService.createOrder(request, user.getUserId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/customer")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Get customer orders", description = "Retrieve all orders for the authenticated customer")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<List<CustomerOrderResponse>> getCustomerOrders() {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                List<CustomerOrderResponse> orders = orderService.getCustomerOrders(user.getUserId());
                return ResponseEntity.ok(orders);
        }

        @GetMapping("/seller")
        @PreAuthorize("hasRole('SELLER')")
        @Operation(summary = "Get seller orders", description = "Retrieve all orders for the authenticated seller's shop")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
                        @ApiResponse(responseCode = "404", description = "User or shop not found")
        })
        public ResponseEntity<List<SellerOrderResponse>> getSellerOrders() {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Shop shop = shopRepository.findByUserUserId(user.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for user"));

                List<SellerOrderResponse> orders = orderService.getSellerOrders(shop.getShopId());
                return ResponseEntity.ok(orders);
        }

        @GetMapping("/{orderId}/history")
        @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER')")
        @Operation(summary = "Get order status history", description = "Retrieve the complete status history of an order. Accessible by the customer who placed the order or the seller of the shop.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized - not your order"),
                        @ApiResponse(responseCode = "404", description = "Order not found")
        })
        public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(@PathVariable Long orderId) {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                List<OrderStatusHistoryResponse> history = orderService.getOrderHistory(orderId, userEmail);
                return ResponseEntity.ok(history);
        }

        @PatchMapping("/{orderId}/status")
        @PreAuthorize("hasRole('SELLER')")
        @Operation(summary = "Update order status", description = "Update order status to Shipping or Delivered. Only the seller of the shop can update the status.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid status or invalid status transition"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized - not your shop's order"),
                        @ApiResponse(responseCode = "404", description = "Order not found")
        })
        public ResponseEntity<UpdateOrderStatusResponse> updateOrderStatus(
                        @PathVariable Long orderId,
                        @Valid @RequestBody UpdateOrderStatusRequest request) {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                UpdateOrderStatusResponse response = orderService.updateOrderStatus(
                                orderId,
                                request.getStatus(),
                                userEmail);

                return ResponseEntity.ok(response);
        }

        @PatchMapping("/{orderId}/cancel")
        @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER')")
        @Operation(summary = "Cancel order", description = "Cancel an order if it hasn't been shipped yet. Restores product stock.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
                        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled (already shipping/delivered)"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized - not your order"),
                        @ApiResponse(responseCode = "404", description = "Order not found")
        })
        public ResponseEntity<CancelOrderResponse> cancelOrder(@PathVariable Long orderId) {
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

                orderService.cancelOrder(orderId, userEmail);

                CancelOrderResponse response = CancelOrderResponse.builder()
                                .orderId(orderId)
                                .message("Order cancelled successfully")
                                .cancelled(true)
                                .build();

                return ResponseEntity.ok(response);
        }
}
