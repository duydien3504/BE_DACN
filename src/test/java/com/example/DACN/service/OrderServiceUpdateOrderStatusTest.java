package com.example.DACN.service;

import com.example.DACN.dto.response.UpdateOrderStatusResponse;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Update Order Status Tests")
class OrderServiceUpdateOrderStatusTest {

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
    private User seller;
    private User customer;
    private Shop shop;
    private Order order;
    private OrderStatusHistory pendingStatus;
    private OrderStatusHistory paidStatus;
    private OrderStatusHistory shippingStatus;
    private OrderStatusHistory deliveredStatus;

    @BeforeEach
    void setUp() {
        orderId = 1L;

        // Setup seller
        seller = new User();
        seller.setUserId(UUID.randomUUID());
        seller.setEmail("seller@test.com");
        seller.setFullName("Seller Name");

        // Setup customer
        customer = new User();
        customer.setUserId(UUID.randomUUID());
        customer.setEmail("customer@test.com");
        customer.setFullName("Customer Name");

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
        pendingStatus = new OrderStatusHistory();
        pendingStatus.setHistoryId(1L);
        pendingStatus.setOrder(order);
        pendingStatus.setStatus("Pending");
        pendingStatus.setDescription("Order created and pending confirmation");
        pendingStatus.setCreatedAt(LocalDateTime.now().minusHours(2));

        paidStatus = new OrderStatusHistory();
        paidStatus.setHistoryId(2L);
        paidStatus.setOrder(order);
        paidStatus.setStatus("Paid");
        paidStatus.setDescription("Payment confirmed");
        paidStatus.setCreatedAt(LocalDateTime.now().minusHours(1));

        shippingStatus = new OrderStatusHistory();
        shippingStatus.setHistoryId(3L);
        shippingStatus.setOrder(order);
        shippingStatus.setStatus("Shipping");
        shippingStatus.setDescription("Order is being shipped to customer");
        shippingStatus.setCreatedAt(LocalDateTime.now());

        deliveredStatus = new OrderStatusHistory();
        deliveredStatus.setHistoryId(4L);
        deliveredStatus.setOrder(order);
        deliveredStatus.setStatus("Delivered");
        deliveredStatus.setDescription("Order has been delivered to customer");
        deliveredStatus.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully update status from Pending to Shipping")
    void testUpdateStatusFromPendingToShipping() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(pendingStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(shippingStatus);

        // When
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("Shipping");
        assertThat(response.getDescription()).isEqualTo("Order is being shipped to customer");
        assertThat(response.getMessage()).isEqualTo("Order status updated successfully");
        assertThat(response.getUpdatedAt()).isNotNull();

        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(captor.capture());
        OrderStatusHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("Shipping");
        assertThat(savedHistory.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("Should successfully update status from Paid to Shipping")
    void testUpdateStatusFromPaidToShipping() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(paidStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(shippingStatus);

        // When
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Shipping");
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should successfully update status from Shipping to Delivered")
    void testUpdateStatusFromShippingToDelivered() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(shippingStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(deliveredStatus);

        // When
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, "Delivered", seller.getEmail());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("Delivered");
        assertThat(response.getDescription()).isEqualTo("Order has been delivered to customer");

        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(captor.capture());
        OrderStatusHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("Delivered");
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testUpdateStatusOrderNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with ID: " + orderId);

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user is not the seller")
    void testUpdateStatusUnauthorizedUser() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", "other@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You do not have permission to update this order");

        verify(orderStatusHistoryRepository, never()).findTopByOrderOrderIdOrderByCreatedAtDesc(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when current status not found")
    void testUpdateStatusCurrentStatusNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order status not found");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to ship from invalid status")
    void testUpdateStatusInvalidTransitionToShipping() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(shippingStatus)); // Already shipping

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change status to Shipping from Shipping");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to deliver from non-shipping status")
    void testUpdateStatusInvalidTransitionToDelivered() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(pendingStatus)); // Still pending

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Delivered", seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change status to Delivered from Pending")
                .hasMessageContaining("Order must be in Shipping status");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order is already delivered")
    void testUpdateStatusAlreadyDelivered() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(deliveredStatus));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update order with status: Delivered");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order is cancelled")
    void testUpdateStatusOrderCancelled() {
        // Given
        OrderStatusHistory cancelledStatus = new OrderStatusHistory();
        cancelledStatus.setStatus("Cancelled");
        cancelledStatus.setOrder(order);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(cancelledStatus));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update order with status: Cancelled");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order is completed")
    void testUpdateStatusOrderCompleted() {
        // Given
        OrderStatusHistory completedStatus = new OrderStatusHistory();
        completedStatus.setStatus("Completed");
        completedStatus.setOrder(order);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(completedStatus));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update order with status: Completed");

        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify correct status description for Shipping")
    void testStatusDescriptionForShipping() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(paidStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(shippingStatus);

        // When
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail());

        // Then
        assertThat(response.getDescription()).isEqualTo("Order is being shipped to customer");
    }

    @Test
    @DisplayName("Should verify correct status description for Delivered")
    void testStatusDescriptionForDelivered() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(shippingStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(deliveredStatus);

        // When
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, "Delivered", seller.getEmail());

        // Then
        assertThat(response.getDescription()).isEqualTo("Order has been delivered to customer");
    }

    @Test
    @DisplayName("Should verify order repository is called with correct ID")
    void testOrderRepositoryCalledWithCorrectId() {
        // Given
        Long specificOrderId = 999L;
        Order specificOrder = new Order();
        specificOrder.setOrderId(specificOrderId);
        specificOrder.setShop(shop);
        specificOrder.setUser(customer);
        specificOrder.setHasDeleted(false);

        when(orderRepository.findByOrderIdAndHasDeletedFalse(specificOrderId))
                .thenReturn(Optional.of(specificOrder));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(specificOrderId))
                .thenReturn(Optional.of(paidStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(shippingStatus);

        // When
        orderService.updateOrderStatus(specificOrderId, "Shipping", seller.getEmail());

        // Then
        verify(orderRepository).findByOrderIdAndHasDeletedFalse(specificOrderId);
        verify(orderStatusHistoryRepository).findTopByOrderOrderIdOrderByCreatedAtDesc(specificOrderId);
    }

    @Test
    @DisplayName("Should verify status history is saved with correct order reference")
    void testStatusHistorySavedWithCorrectOrder() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(paidStatus));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(shippingStatus);

        // When
        orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail());

        // Then
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(captor.capture());
        OrderStatusHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getOrder()).isEqualTo(order);
        assertThat(savedHistory.getStatus()).isEqualTo("Shipping");
        assertThat(savedHistory.getDescription()).isNotNull();
    }

    @Test
    @DisplayName("Should handle deleted order correctly")
    void testUpdateStatusDeletedOrder() {
        // Given
        when(orderRepository.findByOrderIdAndHasDeletedFalse(orderId))
                .thenReturn(Optional.empty()); // Deleted orders are not returned

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "Shipping", seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderStatusHistoryRepository, never()).save(any());
    }
}
