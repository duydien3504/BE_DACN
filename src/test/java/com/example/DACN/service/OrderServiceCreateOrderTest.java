package com.example.DACN.service;

import com.example.DACN.dto.request.CreateOrderRequest;
import com.example.DACN.dto.response.CreateOrderResponse;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.OrderMapper;
import com.example.DACN.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Create Order Tests")
class OrderServiceCreateOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PaypalService paypalService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private Shop shop;
    private Product product1;
    private Product product2;
    private UserAddress userAddress;
    private User user;
    private Cart cart;
    private Voucher voucher;
    private UserVoucher userVoucher;
    private Order savedOrder;
    private CreateOrderRequest request;
    private CreateOrderResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // Setup User
        user = new User();
        user.setUserId(userId);
        user.setEmail("customer@test.com");
        user.setFullName("Test Customer");

        // Setup Shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setIsApproved(true);
        shop.setHasDeleted(false);

        // Setup Products
        product1 = new Product();
        product1.setProductId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("100.00"));
        product1.setStockQuantity(10);
        product1.setSoldCount(5);
        product1.setStatus("Active");
        product1.setShop(shop);
        product1.setHasDeleted(false);

        product2 = new Product();
        product2.setProductId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("50.00"));
        product2.setStockQuantity(20);
        product2.setSoldCount(10);
        product2.setStatus("Active");
        product2.setShop(shop);
        product2.setHasDeleted(false);

        // Setup User Address
        userAddress = new UserAddress();
        userAddress.setUserAddressId(5L);
        userAddress.setUser(user);
        userAddress.setRecipientName("Test Customer");
        userAddress.setPhone("0123456789");
        userAddress.setProvince("HCM");
        userAddress.setDistrict("District 1");
        userAddress.setWard("Ward 1");
        userAddress.setStreetAddress("123 Test St");
        userAddress.setIsDefault(true);
        userAddress.setHasDeleted(false);

        // Setup Cart
        cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);

        // Setup Voucher
        voucher = new Voucher();
        voucher.setVoucherId(10L);
        voucher.setCode("SAVE10");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(new BigDecimal("10"));
        voucher.setMinOrderValue(new BigDecimal("100"));
        voucher.setMaxDiscountAmount(new BigDecimal("50"));
        voucher.setStartDate(LocalDateTime.now().minusDays(1));
        voucher.setEndDate(LocalDateTime.now().plusDays(30));
        voucher.setQuantity(100);
        voucher.setHasDeleted(false);
        voucher.setShop(shop);

        // Setup UserVoucher
        userVoucher = new UserVoucher();
        userVoucher.setUserVoucherId(1L);
        userVoucher.setUser(user);
        userVoucher.setVoucher(voucher);
        userVoucher.setIsUsed(false);

        // Setup Saved Order
        savedOrder = new Order();
        savedOrder.setOrderId(100L);
        savedOrder.setUser(user);
        savedOrder.setShop(shop);
        savedOrder.setTotalAmount(new BigDecimal("200.00"));
        savedOrder.setVoucherDiscount(new BigDecimal("20.00"));
        savedOrder.setFinalAmount(new BigDecimal("180.00"));
        savedOrder.setPaymentMethod("COD");
        savedOrder.setHasDeleted(false);

        // Setup Request
        request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setAddressId(5L);
        request.setPaymentMethod("COD");

        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest();
        item1.setProductId(1L);
        item1.setQty(2);

        request.setItems(Arrays.asList(item1));

        // Setup Response
        response = CreateOrderResponse.builder()
                .orderId(100L)
                .build();
    }

    @Test
    @DisplayName("Should create order successfully with COD payment")
    void testCreateOrderSuccessWithCOD() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        CreateOrderResponse result = orderService.createOrder(request, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getPaymentUrl()).isNull();

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(userAddressRepository).findById(5L);
        verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(cartItemRepository).deleteByCartCartIdAndProductProductId(1L, 1L);
        verify(paypalService, never()).createOrder(any());
    }

    @Test
    @DisplayName("Should create order successfully with PAYPAL payment and return payment URL")
    void testCreateOrderSuccessWithPayPal() {
        // Given
        request.setPaymentMethod("PAYPAL");
        String paypalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=EC-12345";
        java.util.Map<String, String> paypalResult = new java.util.HashMap<>();
        paypalResult.put("orderId", "PAYPAL-ORDER-123");
        paypalResult.put("approvalUrl", paypalUrl);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);
        when(paypalService.createOrder(any(BigDecimal.class))).thenReturn(paypalResult);

        // When
        CreateOrderResponse result = orderService.createOrder(request, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getPaymentUrl()).isEqualTo(paypalUrl);

        verify(paypalService).createOrder(any(BigDecimal.class));
        verify(cartItemRepository).deleteByCartCartIdAndProductProductId(1L, 1L);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when PayPal payment creation fails")
    void testCreateOrderPayPalFailure() {
        // Given
        request.setPaymentMethod("PAYPAL");

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);
        when(paypalService.createOrder(any(BigDecimal.class))).thenThrow(new RuntimeException("PayPal API error"));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create PayPal payment");

        verify(paypalService).createOrder(any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should create order successfully with voucher")
    void testCreateOrderSuccessWithVoucher() {
        // Given
        request.setVoucherId(10L);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.of(userVoucher));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        CreateOrderResponse result = orderService.createOrder(request, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100L);

        verify(userVoucherRepository).findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L);
        verify(userVoucherRepository).save(argThat(uv -> uv.getIsUsed()));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when shop not found")
    void testCreateOrderShopNotFound() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop not found");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when shop is not approved")
    void testCreateOrderShopNotApproved() {
        // Given
        shop.setIsApproved(false);
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shop is not approved");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when address not found")
    void testCreateOrderAddressNotFound() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(userAddressRepository).findById(5L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when address does not belong to user")
    void testCreateOrderAddressNotBelongToUser() {
        // Given
        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        userAddress.setUser(otherUser);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address does not belong to the user");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when address is deleted")
    void testCreateOrderAddressDeleted() {
        // Given
        userAddress.setHasDeleted(true);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address has been deleted");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void testCreateOrderProductNotFound() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product does not belong to shop")
    void testCreateOrderProductNotBelongToShop() {
        // Given
        Shop otherShop = new Shop();
        otherShop.setShopId(2L);
        product1.setShop(otherShop);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to shop");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product is not active")
    void testCreateOrderProductNotActive() {
        // Given
        product1.setStatus("Inactive");

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not active");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when insufficient stock")
    void testCreateOrderInsufficientStock() {
        // Given
        product1.setStockQuantity(1);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when voucher not found")
    void testCreateOrderVoucherNotFound() {
        // Given
        request.setVoucherId(10L);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when voucher is deleted")
    void testCreateOrderVoucherDeleted() {
        // Given
        request.setVoucherId(10L);
        voucher.setHasDeleted(true);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.of(userVoucher));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher has been deleted");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when voucher is expired")
    void testCreateOrderVoucherExpired() {
        // Given
        request.setVoucherId(10L);
        voucher.setEndDate(LocalDateTime.now().minusDays(1));

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.of(userVoucher));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher is not valid at this time");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when order does not meet minimum value for voucher")
    void testCreateOrderVoucherMinimumNotMet() {
        // Given
        request.setVoucherId(10L);
        voucher.setMinOrderValue(new BigDecimal("500"));

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.of(userVoucher));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum order value");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when voucher does not belong to shop")
    void testCreateOrderVoucherNotBelongToShop() {
        // Given
        request.setVoucherId(10L);
        Shop otherShop = new Shop();
        otherShop.setShopId(2L);
        voucher.setShop(otherShop);

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(userVoucherRepository.findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(userId, 10L))
                .thenReturn(Optional.of(userVoucher));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher does not belong to this shop");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update product stock and sold count correctly")
    void testCreateOrderUpdatesProductStock() {
        // Given
        int initialStock = product1.getStockQuantity();
        int initialSold = product1.getSoldCount();
        int orderQty = 2;

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        orderService.createOrder(request, userId);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getStockQuantity()).isEqualTo(initialStock - orderQty);
        assertThat(savedProduct.getSoldCount()).isEqualTo(initialSold + orderQty);
    }

    @Test
    @DisplayName("Should create order status history with Pending status")
    void testCreateOrderCreatesStatusHistory() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        orderService.createOrder(request, userId);

        // Then
        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());

        OrderStatusHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("Pending");
        assertThat(savedHistory.getDescription()).contains("pending");
    }

    @Test
    @DisplayName("Should delete cart items after order creation")
    void testCreateOrderDeletesCartItems() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        orderService.createOrder(request, userId);

        // Then
        verify(cartRepository).findByUserUserId(userId);
        verify(cartItemRepository).deleteByCartCartIdAndProductProductId(1L, 1L);
    }

    @Test
    @DisplayName("Should handle multiple products in order")
    void testCreateOrderWithMultipleProducts() {
        // Given
        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest();
        item2.setProductId(2L);
        item2.setQty(1);
        request.setItems(Arrays.asList(request.getItems().get(0), item2));

        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findByProductIdAndHasDeletedFalse(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        CreateOrderResponse result = orderService.createOrder(request, userId);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        verify(productRepository).findByProductIdAndHasDeletedFalse(2L);
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(productRepository, times(2)).save(any(Product.class));
        verify(cartItemRepository).deleteByCartCartIdAndProductProductId(1L, 1L);
        verify(cartItemRepository).deleteByCartCartIdAndProductProductId(1L, 2L);
    }

    @Test
    @DisplayName("Should verify correct order entity is saved")
    void testCreateOrderSavesCorrectEntity() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(userAddressRepository.findById(5L)).thenReturn(Optional.of(userAddress));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(cartRepository.findByUserUserId(userId)).thenReturn(Optional.of(cart));
        when(orderMapper.toCreateOrderResponse(savedOrder)).thenReturn(response);

        // When
        orderService.createOrder(request, userId);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getUser()).isEqualTo(user);
        assertThat(capturedOrder.getShop()).isEqualTo(shop);
        assertThat(capturedOrder.getPaymentMethod()).isEqualTo("COD");
        assertThat(capturedOrder.getHasDeleted()).isFalse();
        assertThat(capturedOrder.getTotalAmount()).isNotNull();
        assertThat(capturedOrder.getFinalAmount()).isNotNull();
    }
}
