package com.example.DACN.service;

import com.example.DACN.dto.response.VoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.VoucherMapper;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.VoucherRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService - Get Shop Vouchers Tests")
class VoucherServiceGetShopVouchersTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherService voucherService;

    private Shop shop;
    private Voucher voucher1;
    private Voucher voucher2;
    private VoucherResponse voucherResponse1;
    private VoucherResponse voucherResponse2;

    @BeforeEach
    void setUp() {
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setIsApproved(true);
        shop.setHasDeleted(false);

        voucher1 = new Voucher();
        voucher1.setVoucherId(1L);
        voucher1.setShop(shop);
        voucher1.setCode("VOUCHER1");

        voucher2 = new Voucher();
        voucher2.setVoucherId(2L);
        voucher2.setShop(shop);
        voucher2.setCode("VOUCHER2");

        voucherResponse1 = VoucherResponse.builder()
                .voucherId(1L)
                .code("VOUCHER1")
                .shopId(1L)
                .build();

        voucherResponse2 = VoucherResponse.builder()
                .voucherId(2L)
                .code("VOUCHER2")
                .shopId(1L)
                .build();
    }

    @Test
    @DisplayName("Should retrieve active vouchers for valid approved shop")
    void testGetShopVouchersSuccess() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(voucherRepository.findActiveShopVouchers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(voucher1, voucher2));
        when(voucherMapper.toVoucherResponse(voucher1)).thenReturn(voucherResponse1);
        when(voucherMapper.toVoucherResponse(voucher2)).thenReturn(voucherResponse2);

        // When
        List<VoucherResponse> result = voucherService.getShopVouchers(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("VOUCHER1");
        assertThat(result.get(1).getCode()).isEqualTo("VOUCHER2");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(voucherRepository).findActiveShopVouchers(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should throw exception when shop not found")
    void testGetShopVouchersShopNotFound() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.getShopVouchers(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop not found with ID: 1");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(voucherRepository, never()).findActiveShopVouchers(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when shop is not approved")
    void testGetShopVouchersShopNotApproved() {
        // Given
        shop.setIsApproved(false);
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> voucherService.getShopVouchers(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shop is not approved");

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(voucherRepository, never()).findActiveShopVouchers(any(), any());
    }

    @Test
    @DisplayName("Should return empty list when no active vouchers found")
    void testGetShopVouchersEmptyList() {
        // Given
        when(shopRepository.findByShopIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(shop));
        when(voucherRepository.findActiveShopVouchers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<VoucherResponse> result = voucherService.getShopVouchers(1L);

        // Then
        assertThat(result).isEmpty();

        verify(shopRepository).findByShopIdAndHasDeletedFalse(1L);
        verify(voucherRepository).findActiveShopVouchers(eq(1L), any(LocalDateTime.class));
    }
}
