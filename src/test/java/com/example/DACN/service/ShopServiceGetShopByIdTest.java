package com.example.DACN.service;

import com.example.DACN.dto.response.ShopDetailResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.ShopMapper;
import com.example.DACN.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService - Get Shop By ID Tests")
class ShopServiceGetShopByIdTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ShopMapper shopMapper;

    @InjectMocks
    private ShopService shopService;

    private Shop approvedShop;
    private Shop unapprovedShop;
    private Shop deletedShop;
    private ShopDetailResponse shopDetailResponse;

    @BeforeEach
    void setUp() {
        // Setup approved shop
        approvedShop = new Shop();
        approvedShop.setShopId(1L);
        approvedShop.setShopName("Approved Shop");
        approvedShop.setShopDescription("This is an approved shop");
        approvedShop.setLogoUrl("https://cloudinary.com/logo.png");
        approvedShop.setRatingAvg(new BigDecimal("4.5"));
        approvedShop.setIsApproved(true);
        approvedShop.setHasDeleted(false);
        approvedShop.setCreatedAt(LocalDateTime.now().minusDays(30));
        approvedShop.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // Setup unapproved shop
        unapprovedShop = new Shop();
        unapprovedShop.setShopId(2L);
        unapprovedShop.setShopName("Unapproved Shop");
        unapprovedShop.setShopDescription("This shop is not approved");
        unapprovedShop.setLogoUrl("https://cloudinary.com/logo2.png");
        unapprovedShop.setRatingAvg(BigDecimal.ZERO);
        unapprovedShop.setIsApproved(false);
        unapprovedShop.setHasDeleted(false);
        unapprovedShop.setCreatedAt(LocalDateTime.now().minusDays(20));
        unapprovedShop.setUpdatedAt(LocalDateTime.now());

        // Setup deleted shop
        deletedShop = new Shop();
        deletedShop.setShopId(3L);
        deletedShop.setShopName("Deleted Shop");
        deletedShop.setShopDescription("This shop is deleted");
        deletedShop.setLogoUrl("https://cloudinary.com/logo3.png");
        deletedShop.setRatingAvg(new BigDecimal("3.5"));
        deletedShop.setIsApproved(true);
        deletedShop.setHasDeleted(true);
        deletedShop.setCreatedAt(LocalDateTime.now().minusDays(10));
        deletedShop.setUpdatedAt(LocalDateTime.now());

        // Setup response
        shopDetailResponse = ShopDetailResponse.builder()
                .shopId(1L)
                .shopName("Approved Shop")
                .shopDescription("This is an approved shop")
                .logoUrl("https://cloudinary.com/logo.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(approvedShop.getCreatedAt())
                .updatedAt(approvedShop.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Should retrieve approved shop by ID successfully")
    void testGetShopByIdSuccess() {
        // Given
        Long shopId = 1L;
        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.of(approvedShop));
        when(shopMapper.toShopDetailResponse(approvedShop)).thenReturn(shopDetailResponse);

        // When
        ShopDetailResponse result = shopService.getShopById(shopId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShopId()).isEqualTo(1L);
        assertThat(result.getShopName()).isEqualTo("Approved Shop");
        assertThat(result.getShopDescription()).isEqualTo("This is an approved shop");
        assertThat(result.getRatingAvg()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(result.getIsApproved()).isTrue();

        verify(shopRepository).findByShopIdAndHasDeletedFalse(shopId);
        verify(shopMapper).toShopDetailResponse(approvedShop);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when shop not found")
    void testGetShopByIdNotFound() {
        // Given
        Long shopId = 999L;
        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.getShopById(shopId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shop not found");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(shopId);
        verify(shopMapper, never()).toShopDetailResponse(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when shop is deleted")
    void testGetShopByIdDeleted() {
        // Given
        Long shopId = 3L;
        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.getShopById(shopId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shop not found");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(shopId);
        verify(shopMapper, never()).toShopDetailResponse(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when shop is not approved")
    void testGetShopByIdNotApproved() {
        // Given
        Long shopId = 2L;
        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.of(unapprovedShop));

        // When & Then
        assertThatThrownBy(() -> shopService.getShopById(shopId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shop not found");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(shopId);
        verify(shopMapper, never()).toShopDetailResponse(any());
    }

    @Test
    @DisplayName("Should verify correct response structure mapping")
    void testGetShopByIdResponseStructure() {
        // Given
        Long shopId = 1L;
        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.of(approvedShop));
        when(shopMapper.toShopDetailResponse(approvedShop)).thenReturn(shopDetailResponse);

        // When
        ShopDetailResponse result = shopService.getShopById(shopId);

        // Then
        assertThat(result.getShopId()).isNotNull();
        assertThat(result.getShopName()).isNotNull();
        assertThat(result.getRatingAvg()).isNotNull();
        assertThat(result.getIsApproved()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(shopMapper).toShopDetailResponse(argThat(shop -> shop.getShopId().equals(1L) &&
                shop.getIsApproved() &&
                !shop.getHasDeleted()));
    }

    @Test
    @DisplayName("Should handle shop with null optional fields")
    void testGetShopByIdWithNullOptionalFields() {
        // Given
        Long shopId = 1L;
        approvedShop.setShopDescription(null);
        approvedShop.setLogoUrl(null);

        ShopDetailResponse responseWithNulls = ShopDetailResponse.builder()
                .shopId(1L)
                .shopName("Approved Shop")
                .shopDescription(null)
                .logoUrl(null)
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(approvedShop.getCreatedAt())
                .updatedAt(approvedShop.getUpdatedAt())
                .build();

        when(shopRepository.findByShopIdAndHasDeletedFalse(shopId)).thenReturn(Optional.of(approvedShop));
        when(shopMapper.toShopDetailResponse(approvedShop)).thenReturn(responseWithNulls);

        // When
        ShopDetailResponse result = shopService.getShopById(shopId);

        // Then
        assertThat(result.getShopDescription()).isNull();
        assertThat(result.getLogoUrl()).isNull();
        assertThat(result.getShopName()).isNotNull();
        assertThat(result.getShopId()).isEqualTo(1L);
    }
}
