package com.example.DACN.service;

import com.example.DACN.dto.response.SellerOrderResponse;
import com.example.DACN.entity.*;
import com.example.DACN.mapper.OrderMapper;
import com.example.DACN.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Get Seller Orders Tests")
class OrderServiceGetSellerOrdersTest {

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

    private Long shopId;
    private User seller;
    private User customer1;
    private User customer2;
    private Shop shop;
    private Order order1;
    private Order order2;
    private OrderStatusHistory statusHistory1;
    private OrderStatusHistory statusHistory2;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private SellerOrderResponse response1;
    private SellerOrderResponse response2;

    @BeforeEach
    void setUp() {
        shopId = 1L;

        // Setup seller
        seller = new User();
        seller.setUserId(UUID.randomUUID());
        seller.setEmail("seller@test.com");
        seller.setFullName("Seller Name");

        // Setup customers
        customer1 = new User();
        customer1.setUserId(UUID.randomUUID());
        customer1.setEmail("customer1@test.com");
        customer1.setFullName("Customer One");

        customer2 = new User();
        customer2.setUserId(UUID.randomUUID());
        customer2.setEmail("customer2@test.com");
        customer2.setFullName("Customer Two");

        // Setup shop
        shop = new Shop();
        shop.setShopId(shopId);
        shop.setShopName("Test Shop");
        shop.setUser(seller);

        // Setup order 1
        order1 = new Order();
        order1.setOrderId(1L);
        order1.setUser(customer1);
        order1.setShop(shop);
        order1.setTotalAmount(new BigDecimal("1000000"));
        order1.setShippingFee(new BigDecimal("30000"));
        order1.setVoucherDiscount(new BigDecimal("50000"));
        order1.setFinalAmount(new BigDecimal("980000"));
        order1.setPaymentMethod("PAYPAL");
        order1.setCreatedAt(LocalDateTime.now());
        order1.setHasDeleted(false);

        // Setup order 2
        order2 = new Order();
        order2.setOrderId(2L);
        order2.setUser(customer2);
        order2.setShop(shop);
        order2.setTotalAmount(new BigDecimal("500000"));
        order2.setShippingFee(new BigDecimal("20000"));
        order2.setVoucherDiscount(BigDecimal.ZERO);
        order2.setFinalAmount(new BigDecimal("520000"));
        order2.setPaymentMethod("COD");
        order2.setCreatedAt(LocalDateTime.now().minusDays(1));
        order2.setHasDeleted(false);

        // Setup status histories
        statusHistory1 = new OrderStatusHistory();
        statusHistory1.setOrder(order1);
        statusHistory1.setStatus("Paid");
        statusHistory1.setCreatedAt(LocalDateTime.now());

        statusHistory2 = new OrderStatusHistory();
        statusHistory2.setOrder(order2);
        statusHistory2.setStatus("Pending");
        statusHistory2.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Setup order items
        orderItem1 = new OrderItem();
        orderItem1.setOrderItemId(1L);
        orderItem1.setOrder(order1);

        orderItem2 = new OrderItem();
        orderItem2.setOrderItemId(2L);
        orderItem2.setOrder(order1);

        // Setup responses
        response1 = SellerOrderResponse.builder()
                .orderId(1L)
                .customerEmail("customer1@test.com")
                .customerName("Customer One")
                .totalAmount(new BigDecimal("1000000"))
                .shippingFee(new BigDecimal("30000"))
                .voucherDiscount(new BigDecimal("50000"))
                .finalAmount(new BigDecimal("980000"))
                .paymentMethod("PAYPAL")
                .createdAt(order1.getCreatedAt())
                .build();

        response2 = SellerOrderResponse.builder()
                .orderId(2L)
                .customerEmail("customer2@test.com")
                .customerName("Customer Two")
                .totalAmount(new BigDecimal("500000"))
                .shippingFee(new BigDecimal("20000"))
                .voucherDiscount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("520000"))
                .paymentMethod("COD")
                .createdAt(order2.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should return list of seller orders successfully")
    void testGetSellerOrdersSuccess() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory1));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(statusHistory2));
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Arrays.asList(orderItem1, orderItem2));
        when(orderItemRepository.findByOrderOrderId(2L))
                .thenReturn(Collections.singletonList(orderItem2));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(0).getCustomerEmail()).isEqualTo("customer1@test.com");
        assertThat(result.get(0).getCustomerName()).isEqualTo("Customer One");
        assertThat(result.get(0).getCurrentStatus()).isEqualTo("Paid");
        assertThat(result.get(0).getItemCount()).isEqualTo(2);
        assertThat(result.get(1).getOrderId()).isEqualTo(2L);
        assertThat(result.get(1).getCustomerEmail()).isEqualTo("customer2@test.com");
        assertThat(result.get(1).getCustomerName()).isEqualTo("Customer Two");
        assertThat(result.get(1).getCurrentStatus()).isEqualTo("Pending");
        assertThat(result.get(1).getItemCount()).isEqualTo(1);

        verify(orderRepository).findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId);
        verify(orderMapper, times(2)).toSellerOrderResponse(any(Order.class));
        verify(orderStatusHistoryRepository, times(2)).findTopByOrderOrderIdOrderByCreatedAtDesc(any(Long.class));
        verify(orderItemRepository, times(2)).findByOrderOrderId(any(Long.class));
    }

    @Test
    @DisplayName("Should return empty list when shop has no orders")
    void testGetSellerOrdersEmpty() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.emptyList());

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(orderRepository).findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId);
        verify(orderMapper, never()).toSellerOrderResponse(any(Order.class));
    }

    @Test
    @DisplayName("Should set status to Unknown when status history not found")
    void testGetSellerOrdersWithNoStatusHistory() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.singletonList(order1));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentStatus()).isEqualTo("Unknown");
        assertThat(result.get(0).getItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle order with no items")
    void testGetSellerOrdersWithNoItems() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.singletonList(order1));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Collections.emptyList());

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return orders sorted by creation date descending")
    void testGetSellerOrdersSortedByDate() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2)); // order1 is newer
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(any()))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L); // Newer order first
        assertThat(result.get(1).getOrderId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should handle different order statuses correctly")
    void testGetSellerOrdersWithDifferentStatuses() {
        // Given
        OrderStatusHistory shippingStatus = new OrderStatusHistory();
        shippingStatus.setStatus("Shipping");

        OrderStatusHistory deliveredStatus = new OrderStatusHistory();
        deliveredStatus.setStatus("Delivered");

        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(shippingStatus));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(deliveredStatus));
        when(orderItemRepository.findByOrderOrderId(any()))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrentStatus()).isEqualTo("Shipping");
        assertThat(result.get(1).getCurrentStatus()).isEqualTo("Delivered");
    }

    @Test
    @DisplayName("Should handle multiple items per order")
    void testGetSellerOrdersWithMultipleItems() {
        // Given
        OrderItem item3 = new OrderItem();
        item3.setOrderItemId(3L);

        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.singletonList(order1));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Arrays.asList(orderItem1, orderItem2, item3));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should only return non-deleted orders")
    void testGetSellerOrdersExcludesDeleted() {
        // Given
        Order deletedOrder = new Order();
        deletedOrder.setOrderId(3L);
        deletedOrder.setHasDeleted(true);

        // Repository should already filter out deleted orders
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2)); // Only non-deleted

        when(orderMapper.toSellerOrderResponse(any())).thenReturn(response1);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(any()))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(orderRepository).findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId);
    }

    @Test
    @DisplayName("Should handle orders from different customers")
    void testGetSellerOrdersFromDifferentCustomers() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(any()))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomerEmail()).isEqualTo("customer1@test.com");
        assertThat(result.get(1).getCustomerEmail()).isEqualTo("customer2@test.com");
        assertThat(result.get(0).getCustomerName()).isEqualTo("Customer One");
        assertThat(result.get(1).getCustomerName()).isEqualTo("Customer Two");
    }

    @Test
    @DisplayName("Should handle different payment methods")
    void testGetSellerOrdersWithDifferentPaymentMethods() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Arrays.asList(order1, order2));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(any()))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPaymentMethod()).isEqualTo("PAYPAL");
        assertThat(result.get(1).getPaymentMethod()).isEqualTo("COD");
    }

    @Test
    @DisplayName("Should correctly map order amounts and discounts")
    void testGetSellerOrdersAmountsMapping() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.singletonList(order1));
        when(orderMapper.toSellerOrderResponse(order1)).thenReturn(response1);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory1));
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Collections.singletonList(orderItem1));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.get(0).getShippingFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.get(0).getVoucherDiscount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.get(0).getFinalAmount()).isEqualByComparingTo(new BigDecimal("980000"));
    }

    @Test
    @DisplayName("Should handle order with zero voucher discount")
    void testGetSellerOrdersWithZeroVoucherDiscount() {
        // Given
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(shopId))
                .thenReturn(Collections.singletonList(order2));
        when(orderMapper.toSellerOrderResponse(order2)).thenReturn(response2);
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(statusHistory2));
        when(orderItemRepository.findByOrderOrderId(2L))
                .thenReturn(Collections.singletonList(orderItem2));

        // When
        List<SellerOrderResponse> result = orderService.getSellerOrders(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVoucherDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should verify correct repository method is called")
    void testGetSellerOrdersRepositoryMethodCall() {
        // Given
        Long specificShopId = 999L;
        when(orderRepository.findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(specificShopId))
                .thenReturn(Collections.emptyList());

        // When
        orderService.getSellerOrders(specificShopId);

        // Then
        verify(orderRepository).findByShopShopIdAndHasDeletedFalseOrderByCreatedAtDesc(specificShopId);
        verify(orderRepository, never()).findByUserUserIdAndHasDeletedFalseOrderByCreatedAtDesc(any());
    }
}
