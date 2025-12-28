package com.example.DACN.service;

import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Cancel Order Tests")
class OrderServiceCancelOrderTest {

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
    private com.example.DACN.mapper.OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private User customer;
    private User seller;
    private Shop shop;
    private Product product;
    private OrderItem orderItem;
    private OrderStatusHistory statusHistory;

    @BeforeEach
    void setUp() {
        // Setup customer
        customer = new User();
        customer.setUserId(UUID.randomUUID());
        customer.setEmail("customer@test.com");

        // Setup seller
        seller = new User();
        seller.setUserId(UUID.randomUUID());
        seller.setEmail("seller@test.com");

        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setUser(seller);

        // Setup product
        product = new Product();
        product.setProductId(1L);
        product.setStockQuantity(100);
        product.setSoldCount(10);

        // Setup order
        order = new Order();
        order.setOrderId(1L);
        order.setUser(customer);
        order.setShop(shop);
        order.setHasDeleted(false);

        // Setup order item
        orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(5);
        orderItem.setPriceAtPurchase(new BigDecimal("100.00"));

        // Setup status history
        statusHistory = new OrderStatusHistory();
        statusHistory.setOrder(order);
        statusHistory.setStatus("Pending");
        statusHistory.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should cancel order successfully by customer")
    void testCancelOrderByCustomerSuccess() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(Arrays.asList(orderItem));

        // When
        orderService.cancelOrder(1L, "customer@test.com");

        // Then
        verify(orderRepository).save(argThat(o -> o.getHasDeleted()));
        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 105 && p.getSoldCount() == 5));
        verify(orderStatusHistoryRepository).save(argThat(h -> "Cancelled".equals(h.getStatus()) &&
                h.getDescription().contains("customer")));
    }

    @Test
    @DisplayName("Should cancel order successfully by seller")
    void testCancelOrderBySellerSuccess() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(Arrays.asList(orderItem));

        // When
        orderService.cancelOrder(1L, "seller@test.com");

        // Then
        verify(orderRepository).save(argThat(o -> o.getHasDeleted()));
        verify(productRepository).save(any(Product.class));
        verify(orderStatusHistoryRepository).save(argThat(h -> "Cancelled".equals(h.getStatus()) &&
                h.getDescription().contains("seller")));
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testCancelOrderNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(999L, "customer@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user has no permission")
    void testCancelOrderUnauthorized() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, "other@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You do not have permission");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order status is Shipping")
    void testCancelOrderAlreadyShipping() {
        // Given
        statusHistory.setStatus("Shipping");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order with status: Shipping");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order status is Delivered")
    void testCancelOrderAlreadyDelivered() {
        // Given
        statusHistory.setStatus("Delivered");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order with status: Delivered");
    }

    @Test
    @DisplayName("Should throw exception when order status is Completed")
    void testCancelOrderAlreadyCompleted() {
        // Given
        statusHistory.setStatus("Completed");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order with status: Completed");
    }

    @Test
    @DisplayName("Should throw exception when order status not found")
    void testCancelOrderStatusNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order status not found");
    }

    @Test
    @DisplayName("Should restore stock for multiple order items")
    void testCancelOrderMultipleItems() {
        // Given
        Product product2 = new Product();
        product2.setProductId(2L);
        product2.setStockQuantity(50);
        product2.setSoldCount(20);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProduct(product2);
        orderItem2.setQuantity(3);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L))
                .thenReturn(Arrays.asList(orderItem, orderItem2));

        // When
        orderService.cancelOrder(1L, "customer@test.com");

        // Then
        verify(productRepository, times(2)).save(any(Product.class));
        verify(productRepository).save(argThat(p -> p.getProductId() == 1L && p.getStockQuantity() == 105));
        verify(productRepository).save(argThat(p -> p.getProductId() == 2L && p.getStockQuantity() == 53));
    }

    @Test
    @DisplayName("Should allow cancellation when status is Pending")
    void testCancelOrderWithPendingStatus() {
        // Given
        statusHistory.setStatus("Pending");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(Arrays.asList(orderItem));

        // When
        orderService.cancelOrder(1L, "customer@test.com");

        // Then
        verify(orderRepository).save(any(Order.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should allow cancellation when status is Paid")
    void testCancelOrderWithPaidStatus() {
        // Given
        statusHistory.setStatus("Paid");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(Arrays.asList(orderItem));

        // When
        orderService.cancelOrder(1L, "customer@test.com");

        // Then
        verify(orderRepository).save(any(Order.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should allow cancellation when status is Processing")
    void testCancelOrderWithProcessingStatus() {
        // Given
        statusHistory.setStatus("Processing");
        when(orderRepository.findByOrderIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(statusHistory));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(Arrays.asList(orderItem));

        // When
        orderService.cancelOrder(1L, "customer@test.com");

        // Then
        verify(orderRepository).save(any(Order.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }
}
