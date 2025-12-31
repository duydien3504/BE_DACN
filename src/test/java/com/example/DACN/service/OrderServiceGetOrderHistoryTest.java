package com.example.DACN.service;

import com.example.DACN.dto.response.OrderStatusHistoryResponse;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
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
@DisplayName("OrderService - Get Order History Tests")
class OrderServiceGetOrderHistoryTest {

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

    private Long orderId;
    private User customer;
    private User seller;
    private Shop shop;
    private Order order;
    private OrderStatusHistory pendingHistory;
    private OrderStatusHistory paidHistory;
    private OrderStatusHistory shippingHistory;
    private OrderStatusHistoryResponse pendingResponse;
    private OrderStatusHistoryResponse paidResponse;
    private OrderStatusHistoryResponse shippingResponse;

    @BeforeEach
    void setUp() {
        orderId = 1L;

        // Setup customer
        customer = new User();
        customer.setUserId(UUID.randomUUID());
        customer.setEmail("customer@test.com");
        customer.setFullName("Customer Name");

        // Setup seller
        seller = new User();
        seller.setUserId(UUID.randomUUID());
        seller.setEmail("seller@test.com");
        seller.setFullName("Seller Name");

        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setUser(seller);

        // Setup order
        order = new Order();
        order.setOrderId(orderId);
        order.setUser(customer);
        order.setShop(shop);
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setFinalAmount(new BigDecimal("980000"));
        order.setPaymentMethod("PAYPAL");
        order.setHasDeleted(false);

        // Setup status histories
        pendingHistory = new OrderStatusHistory();
        pendingHistory.setHistoryId(1L);
        pendingHistory.setOrder(order);
        pendingHistory.setStatus("Pending");
        pendingHistory.setDescription("Order created and pending confirmation");
        pendingHistory.setCreatedAt(LocalDateTime.now().minusHours(3));

        paidHistory = new OrderStatusHistory();
        paidHistory.setHistoryId(2L);
        paidHistory.setOrder(order);
        paidHistory.setStatus("Paid");
        paidHistory.setDescription("Payment confirmed");
        paidHistory.setCreatedAt(LocalDateTime.now().minusHours(2));

        shippingHistory = new OrderStatusHistory();
        shippingHistory.setHistoryId(3L);
        shippingHistory.setOrder(order);
        shippingHistory.setStatus("Shipping");
        shippingHistory.setDescription("Order is being shipped");
        shippingHistory.setCreatedAt(LocalDateTime.now().minusHours(1));

        // Setup responses
        pendingResponse = OrderStatusHistoryResponse.builder()
                .historyId(1L)
                .status("Pending")
                .description("Order created and pending confirmation")
                .createdAt(pendingHistory.getCreatedAt())
                .build();

        paidResponse = OrderStatusHistoryResponse.builder()
                .historyId(2L)
                .status("Paid")
                .description("Payment confirmed")
                .createdAt(paidHistory.getCreatedAt())
                .build();

        shippingResponse = OrderStatusHistoryResponse.builder()
                .historyId(3L)
                .status("Shipping")
                .description("Order is being shipped")
                .createdAt(shippingHistory.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should successfully retrieve order history for customer")
    void testGetOrderHistoryAsCustomer() {
        // Given
        List<OrderStatusHistory> historyList = Arrays.asList(shippingHistory, paidHistory, pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(shippingHistory)).thenReturn(shippingResponse);
        when(orderMapper.toOrderStatusHistoryResponse(paidHistory)).thenReturn(paidResponse);
        when(orderMapper.toOrderStatusHistoryResponse(pendingHistory)).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo("Shipping");
        assertThat(result.get(1).getStatus()).isEqualTo("Paid");
        assertThat(result.get(2).getStatus()).isEqualTo("Pending");

        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
        verify(orderStatusHistoryRepository).findByOrderOrderIdOrderByCreatedAtDesc(orderId);
        verify(orderMapper, times(3)).toOrderStatusHistoryResponse(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should successfully retrieve order history for seller")
    void testGetOrderHistoryAsSeller() {
        // Given
        List<OrderStatusHistory> historyList = Arrays.asList(shippingHistory, paidHistory, pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(any(OrderStatusHistory.class)))
                .thenReturn(shippingResponse, paidResponse, pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
        verify(orderStatusHistoryRepository).findByOrderOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testGetOrderHistoryOrderNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderHistory(orderId, customer.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with ID: " + orderId);

        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
        verify(orderStatusHistoryRepository, never()).findByOrderOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should throw exception when user is neither customer nor seller")
    void testGetOrderHistoryUnauthorizedUser() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderHistory(orderId, "other@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You do not have permission to view this order history");

        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
        verify(orderStatusHistoryRepository, never()).findByOrderOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should return empty list when no history exists")
    void testGetOrderHistoryEmptyList() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Collections.emptyList());

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
        verify(orderStatusHistoryRepository).findByOrderOrderIdOrderByCreatedAtDesc(orderId);
        verify(orderMapper, never()).toOrderStatusHistoryResponse(any());
    }

    @Test
    @DisplayName("Should return history in descending order by creation date")
    void testGetOrderHistoryOrderedByCreatedAtDesc() {
        // Given
        List<OrderStatusHistory> historyList = Arrays.asList(shippingHistory, paidHistory, pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(shippingHistory)).thenReturn(shippingResponse);
        when(orderMapper.toOrderStatusHistoryResponse(paidHistory)).thenReturn(paidResponse);
        when(orderMapper.toOrderStatusHistoryResponse(pendingHistory)).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCreatedAt()).isAfter(result.get(1).getCreatedAt());
        assertThat(result.get(1).getCreatedAt()).isAfter(result.get(2).getCreatedAt());
    }

    @Test
    @DisplayName("Should handle single status history entry")
    void testGetOrderHistorySingleEntry() {
        // Given
        List<OrderStatusHistory> historyList = Collections.singletonList(pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(pendingHistory)).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("Pending");
        verify(orderMapper, times(1)).toOrderStatusHistoryResponse(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should verify correct repository method is called")
    void testGetOrderHistoryRepositoryMethodCalled() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Collections.emptyList());

        // When
        orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        verify(orderStatusHistoryRepository).findByOrderOrderIdOrderByCreatedAtDesc(orderId);
        verify(orderStatusHistoryRepository, never()).findTopByOrderOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should handle order with multiple status changes")
    void testGetOrderHistoryMultipleStatusChanges() {
        // Given
        OrderStatusHistory deliveredHistory = new OrderStatusHistory();
        deliveredHistory.setHistoryId(4L);
        deliveredHistory.setOrder(order);
        deliveredHistory.setStatus("Delivered");
        deliveredHistory.setDescription("Order delivered");
        deliveredHistory.setCreatedAt(LocalDateTime.now());

        OrderStatusHistoryResponse deliveredResponse = OrderStatusHistoryResponse.builder()
                .historyId(4L)
                .status("Delivered")
                .description("Order delivered")
                .createdAt(deliveredHistory.getCreatedAt())
                .build();

        List<OrderStatusHistory> historyList = Arrays.asList(
                deliveredHistory, shippingHistory, paidHistory, pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(deliveredHistory)).thenReturn(deliveredResponse);
        when(orderMapper.toOrderStatusHistoryResponse(shippingHistory)).thenReturn(shippingResponse);
        when(orderMapper.toOrderStatusHistoryResponse(paidHistory)).thenReturn(paidResponse);
        when(orderMapper.toOrderStatusHistoryResponse(pendingHistory)).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getStatus()).isEqualTo("Delivered");
        assertThat(result.get(3).getStatus()).isEqualTo("Pending");
    }

    @Test
    @DisplayName("Should verify all history entries are mapped")
    void testGetOrderHistoryAllEntriesMapped() {
        // Given
        List<OrderStatusHistory> historyList = Arrays.asList(shippingHistory, paidHistory, pendingHistory);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(historyList);
        when(orderMapper.toOrderStatusHistoryResponse(any(OrderStatusHistory.class)))
                .thenReturn(shippingResponse, paidResponse, pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).hasSize(3);
        verify(orderMapper).toOrderStatusHistoryResponse(shippingHistory);
        verify(orderMapper).toOrderStatusHistoryResponse(paidHistory);
        verify(orderMapper).toOrderStatusHistoryResponse(pendingHistory);
    }

    @Test
    @DisplayName("Should handle deleted order correctly")
    void testGetOrderHistoryDeletedOrder() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderHistory(orderId, customer.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderStatusHistoryRepository, never()).findByOrderOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should verify customer can access their own order history")
    void testGetOrderHistoryCustomerAccess() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Collections.singletonList(pendingHistory));
        when(orderMapper.toOrderStatusHistoryResponse(any())).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
    }

    @Test
    @DisplayName("Should verify seller can access order history for their shop")
    void testGetOrderHistorySellerAccess() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Collections.singletonList(pendingHistory));
        when(orderMapper.toOrderStatusHistoryResponse(any())).thenReturn(pendingResponse);

        // When
        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(orderId);
    }

    @Test
    @DisplayName("Should verify correct order ID is used in repository calls")
    void testGetOrderHistoryCorrectOrderId() {
        // Given
        Long specificOrderId = 999L;
        Order specificOrder = new Order();
        specificOrder.setOrderId(specificOrderId);
        specificOrder.setUser(customer);
        specificOrder.setShop(shop);
        specificOrder.setHasDeleted(false);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(specificOrderId))
                .thenReturn(Optional.of(specificOrder));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtDesc(specificOrderId))
                .thenReturn(Collections.emptyList());

        // When
        orderService.getOrderHistory(specificOrderId, customer.getEmail());

        // Then
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(specificOrderId);
        verify(orderStatusHistoryRepository).findByOrderOrderIdOrderByCreatedAtDesc(specificOrderId);
    }
}
