package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.ShopRegisterRequest;
import com.example.DACN.dto.response.ShopRegisterResponse;
import com.example.DACN.entity.Payment;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.DuplicateResourceException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Registration Tests")
class ShopServiceTest {

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

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ShopService shopService;

    private User user;
    private Role customerRole;
    private ShopRegisterRequest request;
    private Map<String, String> paypalOrder;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setRoleId(2L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole(customerRole);

        request = new ShopRegisterRequest();
        request.setShopName("My Test Shop");
        request.setShopDescription("A test shop description");

        paypalOrder = new HashMap<>();
        paypalOrder.put("orderId", "PAYPAL123456");
        paypalOrder.put("approvalUrl", "https://paypal.com/approve/PAYPAL123456");
    }

    @Test
    @DisplayName("Should register shop successfully")
    void testRegisterShopSuccess() throws Exception {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setShopId(1L);
            return shop;
        });
        when(paypalService.createOrder(any(BigDecimal.class))).thenReturn(paypalOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ShopRegisterResponse response = shopService.registerShop("test@example.com", request, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShopId()).isEqualTo(1L);
        assertThat(response.getPaypalOrderId()).isEqualTo("PAYPAL123456");
        assertThat(response.getPayUrl()).isEqualTo("https://paypal.com/approve/PAYPAL123456");
        assertThat(response.getMessage()).contains("Shop registration initiated");

        verify(userRepository).findByEmail("test@example.com");
        verify(shopRepository).findByUserUserId(user.getUserId());
        verify(shopRepository).save(any(Shop.class));
        verify(paypalService).createOrder(new BigDecimal("50000"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testRegisterShopUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.registerShop("test@example.com", request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("test@example.com");
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should throw exception when user already has a shop")
    void testRegisterShopDuplicateShop() {
        // Given
        Shop existingShop = new Shop();
        existingShop.setShopId(1L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(existingShop));

        // When & Then
        assertThatThrownBy(() -> shopService.registerShop("test@example.com", request, null))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User already has a shop");

        verify(shopRepository).findByUserUserId(user.getUserId());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should throw exception when user is not a customer")
    void testRegisterShopNotCustomer() {
        // Given
        Role sellerRole = new Role();
        sellerRole.setRoleId(3L);
        sellerRole.setRoleName(RoleConstants.SELLER);
        user.setRole(sellerRole);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.registerShop("test@example.com", request, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only customers can register a shop");

        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should create shop with correct properties")
    void testShopCreatedWithCorrectProperties() throws Exception {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setShopId(1L);
            return shop;
        });
        when(paypalService.createOrder(any(BigDecimal.class))).thenReturn(paypalOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        shopService.registerShop("test@example.com", request, null);

        // Then
        verify(shopRepository).save(argThat(shop -> shop.getShopName().equals("My Test Shop") &&
                shop.getShopDescription().equals("A test shop description") &&
                shop.getLogoUrl() == null &&
                !shop.getIsApproved() &&
                !shop.getHasDeleted() &&
                shop.getRatingAvg().equals(BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("Should create payment with pending status")
    void testPaymentCreatedWithPendingStatus() throws Exception {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setShopId(1L);
            return shop;
        });
        when(paypalService.createOrder(any(BigDecimal.class))).thenReturn(paypalOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        shopService.registerShop("test@example.com", request, null);

        // Then
        verify(paymentRepository).save(argThat(payment -> payment.getTransactionCode().equals("PAYPAL123456") &&
                payment.getAmount().equals(new BigDecimal("50000")) &&
                payment.getStatus().equals("PENDING") &&
                payment.getPaymentTime() == null));
    }
}
