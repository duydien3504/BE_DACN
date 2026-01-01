package com.example.DACN.service;

import com.example.DACN.dto.response.DeleteVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.VoucherMapper;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.repository.UserVoucherRepository;
import com.example.DACN.repository.VoucherRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService - Delete Voucher By Admin Tests")
class VoucherServiceDeleteVoucherByAdminTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherService voucherService;

    private Shop shop;
    private Voucher shopVoucher;
    private Voucher platformVoucher;
    private DeleteVoucherResponse response;

    @BeforeEach
    void setUp() {
        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setIsApproved(true);

        // Setup shop voucher
        shopVoucher = new Voucher();
        shopVoucher.setVoucherId(1L);
        shopVoucher.setShop(shop);
        shopVoucher.setCode("SHOP20");
        shopVoucher.setDiscountType("PERCENT");
        shopVoucher.setDiscountValue(new BigDecimal("20"));
        shopVoucher.setMinOrderValue(new BigDecimal("100000"));
        shopVoucher.setMaxDiscountAmount(new BigDecimal("50000"));
        shopVoucher.setStartDate(LocalDateTime.now().minusDays(1));
        shopVoucher.setEndDate(LocalDateTime.now().plusDays(30));
        shopVoucher.setQuantity(100);
        shopVoucher.setHasDeleted(false);
        shopVoucher.setCreatedAt(LocalDateTime.now());

        // Setup platform voucher
        platformVoucher = new Voucher();
        platformVoucher.setVoucherId(2L);
        platformVoucher.setShop(null); // Platform voucher
        platformVoucher.setCode("PLATFORM50");
        platformVoucher.setDiscountType("FIXED");
        platformVoucher.setDiscountValue(new BigDecimal("50000"));
        platformVoucher.setMinOrderValue(new BigDecimal("200000"));
        platformVoucher.setMaxDiscountAmount(null);
        platformVoucher.setStartDate(LocalDateTime.now().minusDays(1));
        platformVoucher.setEndDate(LocalDateTime.now().plusDays(30));
        platformVoucher.setQuantity(50);
        platformVoucher.setHasDeleted(false);
        platformVoucher.setCreatedAt(LocalDateTime.now());

        // Setup response
        response = DeleteVoucherResponse.builder()
                .voucherId(1L)
                .message("Voucher deleted successfully")
                .build();
    }

    @Test
    @DisplayName("Should successfully delete shop voucher as admin")
    void testDeleteShopVoucherByAdmin() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Voucher deleted successfully");

        verify(voucherRepository).findById(1L);
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should successfully delete platform voucher as admin")
    void testDeletePlatformVoucherByAdmin() {
        // Given
        DeleteVoucherResponse platformResponse = DeleteVoucherResponse.builder()
                .voucherId(2L)
                .message("Voucher deleted successfully")
                .build();

        when(voucherRepository.findById(2L)).thenReturn(Optional.of(platformVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(platformVoucher);
        when(voucherMapper.toDeleteVoucherResponse(platformVoucher)).thenReturn(platformResponse);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherId()).isEqualTo(2L);
        assertThat(result.getMessage()).isEqualTo("Voucher deleted successfully");

        verify(voucherRepository).findById(2L);
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should set hasDeleted to true when admin deletes voucher")
    void testDeleteVoucherByAdminSetsHasDeleted() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        voucherService.deleteVoucherByAdmin(1L);

        // Then
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();

        assertThat(savedVoucher.getHasDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when voucher not found")
    void testDeleteVoucherByAdminVoucherNotFound() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucherByAdmin(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found with ID: 1");

        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher already deleted")
    void testDeleteVoucherByAdminAlreadyDeleted() {
        // Given
        shopVoucher.setHasDeleted(true);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucherByAdmin(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found with ID: 1");

        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify validation order")
    void testDeleteVoucherByAdminValidationOrder() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        voucherService.deleteVoucherByAdmin(1L);

        // Then
        var inOrder = inOrder(voucherRepository);
        inOrder.verify(voucherRepository).findById(1L);
        inOrder.verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void testDeleteVoucherByAdminMapperCall() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        voucherService.deleteVoucherByAdmin(1L);

        // Then
        verify(voucherMapper).toDeleteVoucherResponse(shopVoucher);
    }

    @Test
    @DisplayName("Should return response with success message")
    void testDeleteVoucherByAdminResponseMessage() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result.getMessage()).isEqualTo("Voucher deleted successfully");
    }

    @Test
    @DisplayName("Should delete voucher with FIXED discount type")
    void testDeleteVoucherByAdminFixedDiscount() {
        // Given
        when(voucherRepository.findById(2L)).thenReturn(Optional.of(platformVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(platformVoucher);
        when(voucherMapper.toDeleteVoucherResponse(platformVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(2L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete voucher regardless of quantity")
    void testDeleteVoucherByAdminWithZeroQuantity() {
        // Given
        shopVoucher.setQuantity(0);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete expired voucher")
    void testDeleteExpiredVoucherByAdmin() {
        // Given
        shopVoucher.setEndDate(LocalDateTime.now().minusDays(1));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete voucher that hasn't started yet")
    void testDeleteFutureVoucherByAdmin() {
        // Given
        shopVoucher.setStartDate(LocalDateTime.now().plusDays(1));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should not require user or shop lookup")
    void testDeleteVoucherByAdminNoUserShopLookup() {
        // Given
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        voucherService.deleteVoucherByAdmin(1L);

        // Then
        verify(userRepository, never()).findByEmail(any());
        verify(shopRepository, never()).findByUserUserId(any());
    }

    @Test
    @DisplayName("Should handle voucher with large quantity")
    void testDeleteVoucherByAdminLargeQuantity() {
        // Given
        shopVoucher.setQuantity(10000);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should handle voucher with 100% discount")
    void testDeleteVoucherByAdminFullDiscount() {
        // Given
        shopVoucher.setDiscountValue(new BigDecimal("100"));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(shopVoucher);
        when(voucherMapper.toDeleteVoucherResponse(shopVoucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucherByAdmin(1L);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }
}
