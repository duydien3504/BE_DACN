package com.example.DACN.service;

import com.example.DACN.dto.response.ShopListResponse;
import com.example.DACN.entity.Shop;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService - Get All Shops Tests")
class ShopServiceGetAllShopsTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ShopMapper shopMapper;

    @InjectMocks
    private ShopService shopService;

    private Shop shop1;
    private Shop shop2;
    private Shop shop3;
    private ShopListResponse response1;
    private ShopListResponse response2;
    private ShopListResponse response3;

    @BeforeEach
    void setUp() {
        // Setup shop 1
        shop1 = new Shop();
        shop1.setShopId(1L);
        shop1.setShopName("Shop One");
        shop1.setShopDescription("First shop");
        shop1.setLogoUrl("https://cloudinary.com/logo1.png");
        shop1.setRatingAvg(new BigDecimal("4.5"));
        shop1.setIsApproved(true);
        shop1.setHasDeleted(false);
        shop1.setCreatedAt(LocalDateTime.now().minusDays(30));

        // Setup shop 2
        shop2 = new Shop();
        shop2.setShopId(2L);
        shop2.setShopName("Shop Two");
        shop2.setShopDescription("Second shop");
        shop2.setLogoUrl("https://cloudinary.com/logo2.png");
        shop2.setRatingAvg(new BigDecimal("4.8"));
        shop2.setIsApproved(true);
        shop2.setHasDeleted(false);
        shop2.setCreatedAt(LocalDateTime.now().minusDays(20));

        // Setup shop 3
        shop3 = new Shop();
        shop3.setShopId(3L);
        shop3.setShopName("Shop Three");
        shop3.setShopDescription("Third shop");
        shop3.setLogoUrl("https://cloudinary.com/logo3.png");
        shop3.setRatingAvg(new BigDecimal("4.2"));
        shop3.setIsApproved(true);
        shop3.setHasDeleted(false);
        shop3.setCreatedAt(LocalDateTime.now().minusDays(10));

        // Setup responses
        response1 = ShopListResponse.builder()
                .shopId(1L)
                .shopName("Shop One")
                .shopDescription("First shop")
                .logoUrl("https://cloudinary.com/logo1.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(shop1.getCreatedAt())
                .build();

        response2 = ShopListResponse.builder()
                .shopId(2L)
                .shopName("Shop Two")
                .shopDescription("Second shop")
                .logoUrl("https://cloudinary.com/logo2.png")
                .ratingAvg(new BigDecimal("4.8"))
                .isApproved(true)
                .createdAt(shop2.getCreatedAt())
                .build();

        response3 = ShopListResponse.builder()
                .shopId(3L)
                .shopName("Shop Three")
                .shopDescription("Third shop")
                .logoUrl("https://cloudinary.com/logo3.png")
                .ratingAvg(new BigDecimal("4.2"))
                .isApproved(true)
                .createdAt(shop3.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should retrieve all approved shops successfully")
    void testGetAllShopsSuccess() {
        // Given
        List<Shop> shops = Arrays.asList(shop1, shop2, shop3);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(shops);
        when(shopMapper.toShopListResponse(shop1)).thenReturn(response1);
        when(shopMapper.toShopListResponse(shop2)).thenReturn(response2);
        when(shopMapper.toShopListResponse(shop3)).thenReturn(response3);

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getShopId()).isEqualTo(1L);
        assertThat(result.get(0).getShopName()).isEqualTo("Shop One");
        assertThat(result.get(1).getShopId()).isEqualTo(2L);
        assertThat(result.get(1).getShopName()).isEqualTo("Shop Two");
        assertThat(result.get(2).getShopId()).isEqualTo(3L);
        assertThat(result.get(2).getShopName()).isEqualTo("Shop Three");

        verify(shopRepository).findByIsApprovedTrueAndHasDeletedFalse();
        verify(shopMapper, times(3)).toShopListResponse(any(Shop.class));
    }

    @Test
    @DisplayName("Should return empty list when no approved shops exist")
    void testGetAllShopsEmptyList() {
        // Given
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(Collections.emptyList());

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shopRepository).findByIsApprovedTrueAndHasDeletedFalse();
        verify(shopMapper, never()).toShopListResponse(any(Shop.class));
    }

    @Test
    @DisplayName("Should only retrieve approved shops")
    void testGetAllShopsOnlyApproved() {
        // Given
        List<Shop> approvedShops = Arrays.asList(shop1, shop2);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(approvedShops);
        when(shopMapper.toShopListResponse(shop1)).thenReturn(response1);
        when(shopMapper.toShopListResponse(shop2)).thenReturn(response2);

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(shop -> shop.getIsApproved());

        verify(shopRepository).findByIsApprovedTrueAndHasDeletedFalse();
    }

    @Test
    @DisplayName("Should only retrieve non-deleted shops")
    void testGetAllShopsOnlyNonDeleted() {
        // Given
        List<Shop> nonDeletedShops = Arrays.asList(shop1, shop2, shop3);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(nonDeletedShops);
        when(shopMapper.toShopListResponse(any(Shop.class))).thenReturn(response1, response2, response3);

        // When
        shopService.getAllShops();

        // Then
        verify(shopRepository).findByIsApprovedTrueAndHasDeletedFalse();
        verify(shopRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should verify correct response structure mapping")
    void testGetAllShopsResponseStructure() {
        // Given
        List<Shop> shops = Arrays.asList(shop1);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(shops);
        when(shopMapper.toShopListResponse(shop1)).thenReturn(response1);

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).isNotEmpty();
        ShopListResponse response = result.get(0);
        assertThat(response.getShopId()).isNotNull();
        assertThat(response.getShopName()).isNotNull();
        assertThat(response.getRatingAvg()).isNotNull();
        assertThat(response.getIsApproved()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();

        verify(shopMapper).toShopListResponse(argThat(shop -> shop.getShopId().equals(1L) &&
                shop.getIsApproved() &&
                !shop.getHasDeleted()));
    }

    @Test
    @DisplayName("Should handle multiple shops correctly")
    void testGetAllShopsMultipleShops() {
        // Given
        List<Shop> shops = Arrays.asList(shop1, shop2, shop3);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(shops);
        when(shopMapper.toShopListResponse(shop1)).thenReturn(response1);
        when(shopMapper.toShopListResponse(shop2)).thenReturn(response2);
        when(shopMapper.toShopListResponse(shop3)).thenReturn(response3);

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting("shopId").containsExactly(1L, 2L, 3L);
        assertThat(result).extracting("shopName").containsExactly("Shop One", "Shop Two", "Shop Three");
    }

    @Test
    @DisplayName("Should handle shops with null optional fields")
    void testGetAllShopsWithNullOptionalFields() {
        // Given
        shop1.setShopDescription(null);
        shop1.setLogoUrl(null);

        ShopListResponse responseWithNulls = ShopListResponse.builder()
                .shopId(1L)
                .shopName("Shop One")
                .shopDescription(null)
                .logoUrl(null)
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(shop1.getCreatedAt())
                .build();

        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(Arrays.asList(shop1));
        when(shopMapper.toShopListResponse(shop1)).thenReturn(responseWithNulls);

        // When
        List<ShopListResponse> result = shopService.getAllShops();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShopDescription()).isNull();
        assertThat(result.get(0).getLogoUrl()).isNull();
        assertThat(result.get(0).getShopName()).isNotNull();
    }

    @Test
    @DisplayName("Should call mapper for each shop")
    void testGetAllShopsMapperCalls() {
        // Given
        List<Shop> shops = Arrays.asList(shop1, shop2);
        when(shopRepository.findByIsApprovedTrueAndHasDeletedFalse()).thenReturn(shops);
        when(shopMapper.toShopListResponse(any(Shop.class))).thenReturn(response1, response2);

        // When
        shopService.getAllShops();

        // Then
        verify(shopMapper, times(2)).toShopListResponse(any(Shop.class));
        verify(shopMapper).toShopListResponse(shop1);
        verify(shopMapper).toShopListResponse(shop2);
    }
}
