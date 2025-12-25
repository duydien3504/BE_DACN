package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.PaypalWebhookRequest;
import com.example.DACN.entity.Payment;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.repository.PaymentRepository;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Webhook Processing Tests")
class ShopServiceWebhookTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PaypalService paypalService;

    @InjectMocks
    private ShopService shopService;

    private Payment payment;
    private Shop shop;
    private User user;
    private Role customerRole;
    private Role sellerRole;
    private PaypalWebhookRequest webhookRequest;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setRoleId(2L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        sellerRole = new Role();
        sellerRole.setRoleId(3L);
        sellerRole.setRoleName(RoleConstants.SELLER);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(customerRole);

        shop = new Shop();
        shop.setShopId(1L);
        shop.setUser(user);
        shop.setShopName("Test Shop");
        shop.setIsApproved(false);
        shop.setHasDeleted(false);
        shop.setCreatedAt(LocalDateTime.now());

        payment = new Payment();
        payment.setPaymentId(1L);
        payment.setTransactionCode("PAYPAL123456");
        payment.setAmount(new BigDecimal("50000"));
        payment.setStatus("PENDING");

        webhookRequest = new PaypalWebhookRequest();
        webhookRequest.setEventType("PAYMENT.CAPTURE.COMPLETED");

        PaypalWebhookRequest.Resource resource = new PaypalWebhookRequest.Resource();
        PaypalWebhookRequest.SupplementaryData supplementaryData = new PaypalWebhookRequest.SupplementaryData();
        PaypalWebhookRequest.RelatedIds relatedIds = new PaypalWebhookRequest.RelatedIds();
        relatedIds.setOrderId("PAYPAL123456");
        supplementaryData.setRelatedIds(relatedIds);
        resource.setSupplementaryData(supplementaryData);
        webhookRequest.setResource(resource);
    }

    @Test
    @DisplayName("Should process webhook and approve shop successfully")
    void testProcessWebhookSuccess() {
        // Given
        when(paymentRepository.findByTransactionCode(anyString())).thenReturn(Optional.of(payment));
        when(shopRepository.findByIsApprovedFalseAndHasDeletedFalse()).thenReturn(Collections.singletonList(shop));
        when(roleRepository.findByRoleName(RoleConstants.SELLER)).thenReturn(Optional.of(sellerRole));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        shopService.processPaymentWebhook(webhookRequest);

        // Then
        verify(paymentRepository).findByTransactionCode("PAYPAL123456");
        verify(paymentRepository).save(argThat(p -> p.getStatus().equals("SUCCESS") && p.getPaymentTime() != null));
        verify(shopRepository).save(argThat(s -> s.getIsApproved()));
        verify(userRepository).save(argThat(u -> u.getRole().getRoleName().equals(RoleConstants.SELLER)));
    }

    @Test
    @DisplayName("Should ignore non-payment-capture events")
    void testProcessWebhookIgnoreOtherEvents() {
        // Given
        webhookRequest.setEventType("ORDER.CREATED");

        // When
        shopService.processPaymentWebhook(webhookRequest);

        // Then
        verify(paymentRepository, never()).findByTransactionCode(anyString());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should handle idempotency - skip if already processed")
    void testProcessWebhookIdempotency() {
        // Given
        payment.setStatus("SUCCESS");
        payment.setPaymentTime(LocalDateTime.now());

        when(paymentRepository.findByTransactionCode(anyString())).thenReturn(Optional.of(payment));

        // When
        shopService.processPaymentWebhook(webhookRequest);

        // Then
        verify(paymentRepository).findByTransactionCode("PAYPAL123456");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(shopRepository, never()).save(any(Shop.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void testProcessWebhookPaymentNotFound() {
        // Given
        when(paymentRepository.findByTransactionCode(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.processPaymentWebhook(webhookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");

        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should throw exception when no pending shop found")
    void testProcessWebhookNoPendingShop() {
        // Given
        when(paymentRepository.findByTransactionCode(anyString())).thenReturn(Optional.of(payment));
        when(shopRepository.findByIsApprovedFalseAndHasDeletedFalse()).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> shopService.processPaymentWebhook(webhookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No pending shop found");

        verify(paymentRepository).save(any(Payment.class)); // Payment still updated
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when seller role not found")
    void testProcessWebhookSellerRoleNotFound() {
        // Given
        when(paymentRepository.findByTransactionCode(anyString())).thenReturn(Optional.of(payment));
        when(shopRepository.findByIsApprovedFalseAndHasDeletedFalse()).thenReturn(Collections.singletonList(shop));
        when(roleRepository.findByRoleName(RoleConstants.SELLER)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.processPaymentWebhook(webhookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Seller role not found");

        verify(shopRepository).save(any(Shop.class)); // Shop still approved
        verify(userRepository, never()).save(any(User.class));
    }
}
