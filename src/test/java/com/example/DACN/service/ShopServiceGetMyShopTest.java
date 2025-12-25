package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.response.MyShopResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.mapper.ShopMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService - Get My Shop Tests")
class ShopServiceGetMyShopTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopMapper shopMapper;

    @InjectMocks
    private ShopService shopService;

    private User sellerUser;
    private User customerUser;
    private User adminUser;
    private Shop shop;
    private MyShopResponse myShopResponse;
    private Role sellerRole;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Setup roles
        sellerRole = new Role();
        sellerRole.setRoleId(3L);
        sellerRole.setRoleName(RoleConstants.SELLER);

        customerRole = new Role();
        customerRole.setRoleId(2L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName(RoleConstants.ADMIN);

        // Setup seller user
        sellerUser = new User();
        sellerUser.setUserId(UUID.randomUUID());
        sellerUser.setEmail("seller@example.com");
        sellerUser.setFullName("Seller User");
        sellerUser.setRole(sellerRole);

        // Setup customer user
        customerUser = new User();
        customerUser.setUserId(UUID.randomUUID());
        customerUser.setEmail("customer@example.com");
        customerUser.setFullName("Customer User");
        customerUser.setRole(customerRole);

        // Setup admin user
        adminUser = new User();
        adminUser.setUserId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("Admin User");
        adminUser.setRole(adminRole);

        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setUser(sellerUser);
        shop.setShopName("My Awesome Shop");
        shop.setShopDescription("Best products in town");
        shop.setLogoUrl("https://cloudinary.com/logo.png");
        shop.setRatingAvg(new BigDecimal("4.5"));
        shop.setIsApproved(true);
        shop.setHasDeleted(false);
        shop.setCreatedAt(LocalDateTime.now().minusDays(30));
        shop.setUpdatedAt(LocalDateTime.now());

        // Setup response
        myShopResponse = MyShopResponse.builder()
                .shopId(1L)
                .shopName("My Awesome Shop")
                .shopDescription("Best products in town")
                .logoUrl("https://cloudinary.com/logo.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Should retrieve shop successfully for seller")
    void testGetMyShopSuccess() {
        // Given
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId()))
                .thenReturn(List.of(shop));
        when(shopMapper.toMyShopResponse(shop)).thenReturn(myShopResponse);

        // When
        MyShopResponse response = shopService.getMyShop("seller@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShopId()).isEqualTo(1L);
        assertThat(response.getShopName()).isEqualTo("My Awesome Shop");
        assertThat(response.getShopDescription()).isEqualTo("Best products in town");
        assertThat(response.getLogoUrl()).isEqualTo("https://cloudinary.com/logo.png");
        assertThat(response.getRatingAvg()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(response.getIsApproved()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();

        verify(userRepository).findByEmail("seller@example.com");
        verify(shopRepository).findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId());
        verify(shopMapper).toMyShopResponse(shop);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testGetMyShopUserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.getMyShop("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(shopRepository, never()).findByUserUserIdAndHasDeletedFalse(any());
        verify(shopMapper, never()).toMyShopResponse(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is a customer")
    void testGetMyShopUserIsCustomer() {
        // Given
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        // When & Then
        assertThatThrownBy(() -> shopService.getMyShop("customer@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can access shop information");

        verify(userRepository).findByEmail("customer@example.com");
        verify(shopRepository, never()).findByUserUserIdAndHasDeletedFalse(any());
        verify(shopMapper, never()).toMyShopResponse(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is an admin")
    void testGetMyShopUserIsAdmin() {
        // Given
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        // When & Then
        assertThatThrownBy(() -> shopService.getMyShop("admin@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can access shop information");

        verify(userRepository).findByEmail("admin@example.com");
        verify(shopRepository, never()).findByUserUserIdAndHasDeletedFalse(any());
        verify(shopMapper, never()).toMyShopResponse(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when seller has no shop")
    void testGetMyShopNoShopFound() {
        // Given
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId()))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> shopService.getMyShop("seller@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shop not found");

        verify(userRepository).findByEmail("seller@example.com");
        verify(shopRepository).findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId());
        verify(shopMapper, never()).toMyShopResponse(any());
    }

    @Test
    @DisplayName("Should verify correct response structure mapping")
    void testGetMyShopResponseStructure() {
        // Given
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId()))
                .thenReturn(List.of(shop));
        when(shopMapper.toMyShopResponse(shop)).thenReturn(myShopResponse);

        // When
        MyShopResponse response = shopService.getMyShop("seller@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).isInstanceOf(MyShopResponse.class);
        assertThat(response.getShopId()).isNotNull();
        assertThat(response.getShopName()).isNotNull();
        assertThat(response.getRatingAvg()).isNotNull();
        assertThat(response.getIsApproved()).isNotNull();

        verify(shopMapper).toMyShopResponse(argThat(s -> s.getShopId().equals(1L) &&
                s.getShopName().equals("My Awesome Shop") &&
                s.getIsApproved().equals(true) &&
                !s.getHasDeleted()));
    }

    @Test
    @DisplayName("Should only retrieve non-deleted shops")
    void testGetMyShopExcludesDeletedShops() {
        // Given
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId()))
                .thenReturn(List.of(shop));
        when(shopMapper.toMyShopResponse(shop)).thenReturn(myShopResponse);

        // When
        shopService.getMyShop("seller@example.com");

        // Then
        verify(shopRepository).findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId());
        verify(shopRepository, never()).findByUserUserId(any());
    }

    @Test
    @DisplayName("Should handle shop with null optional fields")
    void testGetMyShopWithNullOptionalFields() {
        // Given
        shop.setShopDescription(null);
        shop.setLogoUrl(null);

        MyShopResponse responseWithNulls = MyShopResponse.builder()
                .shopId(1L)
                .shopName("My Awesome Shop")
                .shopDescription(null)
                .logoUrl(null)
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserIdAndHasDeletedFalse(sellerUser.getUserId()))
                .thenReturn(List.of(shop));
        when(shopMapper.toMyShopResponse(shop)).thenReturn(responseWithNulls);

        // When
        MyShopResponse response = shopService.getMyShop("seller@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShopDescription()).isNull();
        assertThat(response.getLogoUrl()).isNull();
        assertThat(response.getShopName()).isNotNull();
    }
}
