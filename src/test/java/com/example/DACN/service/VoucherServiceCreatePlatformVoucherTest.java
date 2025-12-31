package com.example.DACN.service;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.entity.Voucher;
import com.example.DACN.mapper.VoucherMapper;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService - Create Platform Voucher Tests")
class VoucherServiceCreatePlatformVoucherTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherService voucherService;

    private CreateVoucherRequest request;
    private Voucher voucher;
    private CreateVoucherResponse response;

    @BeforeEach
    void setUp() {
        // Setup request
        request = CreateVoucherRequest.builder()
                .code("PLATFORM20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .quantity(1000)
                .build();

        // Setup voucher entity (platform voucher with shop = null)
        voucher = new Voucher();
        voucher.setVoucherId(1L);
        voucher.setShop(null); // Platform voucher
        voucher.setCode("PLATFORM20");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(new BigDecimal("20"));
        voucher.setMinOrderValue(new BigDecimal("100000"));
        voucher.setMaxDiscountAmount(new BigDecimal("50000"));
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setQuantity(1000);
        voucher.setHasDeleted(false);
        voucher.setCreatedAt(LocalDateTime.now());

        // Setup response
        response = CreateVoucherResponse.builder()
                .voucherId(1L)
                .shopId(null) // Platform voucher has no shop
                .code("PLATFORM20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .quantity(1000)
                .createdAt(voucher.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should successfully create platform voucher with PERCENT discount type")
    void testCreatePlatformVoucherPercentType() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherId()).isEqualTo(1L);
        assertThat(result.getShopId()).isNull(); // Platform voucher has no shop
        assertThat(result.getCode()).isEqualTo("PLATFORM20");
        assertThat(result.getDiscountType()).isEqualTo("PERCENT");
        assertThat(result.getDiscountValue()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(result.getMessage()).isEqualTo("Platform voucher created successfully");

        verify(voucherRepository).existsByCode("PLATFORM20");
        verify(voucherRepository).save(any(Voucher.class));
        verify(userRepository, never()).findByEmail(any()); // No user lookup for platform vouchers
        verify(shopRepository, never()).findByUserUserId(any()); // No shop lookup for platform vouchers
    }

    @Test
    @DisplayName("Should successfully create platform voucher with FIXED discount type")
    void testCreatePlatformVoucherFixedType() {
        // Given
        request.setDiscountType("FIXED");
        request.setDiscountValue(new BigDecimal("100000"));
        voucher.setDiscountType("FIXED");
        voucher.setDiscountValue(new BigDecimal("100000"));

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should throw exception when voucher code already exists")
    void testCreatePlatformVoucherCodeAlreadyExists() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> voucherService.createPlatformVoucher(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher code already exists: PLATFORM20");

        verify(voucherRepository).existsByCode("PLATFORM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when end date is before start date")
    void testCreatePlatformVoucherInvalidDateRange() {
        // Given
        request.setEndDate(LocalDateTime.now().minusDays(1));
        request.setStartDate(LocalDateTime.now().plusDays(1));

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.createPlatformVoucher(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");

        verify(voucherRepository).existsByCode("PLATFORM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when percentage discount exceeds 100")
    void testCreatePlatformVoucherPercentageExceeds100() {
        // Given
        request.setDiscountValue(new BigDecimal("150"));

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.createPlatformVoucher(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentage discount cannot exceed 100%");

        verify(voucherRepository).existsByCode("PLATFORM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when percentage discount is zero or negative")
    void testCreatePlatformVoucherPercentageZeroOrNegative() {
        // Given
        request.setDiscountValue(BigDecimal.ZERO);

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.createPlatformVoucher(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentage discount must be greater than 0");

        verify(voucherRepository).existsByCode("PLATFORM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set shop to null for platform voucher")
    void testCreatePlatformVoucherSetsShopToNull() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createPlatformVoucher(request);

        // Then
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();

        assertThat(savedVoucher.getShop()).isNull(); // Platform voucher must have shop = null
        assertThat(savedVoucher.getHasDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should verify all validations are performed in correct order")
    void testCreatePlatformVoucherValidationOrder() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createPlatformVoucher(request);

        // Then
        var inOrder = inOrder(voucherRepository);
        inOrder.verify(voucherRepository).existsByCode("PLATFORM20");
        inOrder.verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should handle platform voucher with minimum required fields")
    void testCreatePlatformVoucherMinimumFields() {
        // Given
        request.setMinOrderValue(null);
        request.setMaxDiscountAmount(null);

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void testCreatePlatformVoucherMapperCalls() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createPlatformVoucher(request);

        // Then
        verify(voucherMapper).toEntity(request);
        verify(voucherMapper).toCreateVoucherResponse(voucher);
    }

    @Test
    @DisplayName("Should return response with platform voucher success message")
    void testCreatePlatformVoucherResponseMessage() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result.getMessage()).isEqualTo("Platform voucher created successfully");
    }

    @Test
    @DisplayName("Should handle platform voucher with 100% discount")
    void testCreatePlatformVoucherWith100PercentDiscount() {
        // Given
        request.setDiscountValue(new BigDecimal("100"));
        voucher.setDiscountValue(new BigDecimal("100"));

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify platform voucher is saved with correct data")
    void testCreatePlatformVoucherSavedData() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createPlatformVoucher(request);

        // Then
        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());
        Voucher saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo("PLATFORM20");
        assertThat(saved.getDiscountType()).isEqualTo("PERCENT");
        assertThat(saved.getQuantity()).isEqualTo(1000);
        assertThat(saved.getShop()).isNull(); // Most important - platform voucher
    }

    @Test
    @DisplayName("Should create platform voucher with large quantity")
    void testCreatePlatformVoucherLargeQuantity() {
        // Given
        request.setQuantity(10000);
        voucher.setQuantity(10000);

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should not check user or shop for platform voucher")
    void testCreatePlatformVoucherNoUserShopCheck() {
        // Given
        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createPlatformVoucher(request);

        // Then
        verify(userRepository, never()).findByEmail(any());
        verify(shopRepository, never()).findByUserUserId(any());
        verify(shopRepository, never()).findByShopIdAndHasDeletedFalse(any());
    }

    @Test
    @DisplayName("Should handle platform voucher with negative discount for FIXED type")
    void testCreatePlatformVoucherFixedTypeNoPercentageValidation() {
        // Given
        request.setDiscountType("FIXED");
        request.setDiscountValue(new BigDecimal("200000")); // Large fixed amount
        voucher.setDiscountType("FIXED");
        voucher.setDiscountValue(new BigDecimal("200000"));

        when(voucherRepository.existsByCode("PLATFORM20")).thenReturn(false);
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createPlatformVoucher(request);

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }
}
