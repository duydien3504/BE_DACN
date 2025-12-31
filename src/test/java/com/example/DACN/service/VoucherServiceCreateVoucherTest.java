package com.example.DACN.service;

import com.example.DACN.dto.request.CreateVoucherRequest;
import com.example.DACN.dto.response.CreateVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.ResourceNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService - Create Voucher Tests")
class VoucherServiceCreateVoucherTest {

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

    private User seller;
    private Shop shop;
    private CreateVoucherRequest request;
    private Voucher voucher;
    private CreateVoucherResponse response;

    @BeforeEach
    void setUp() {
        // Setup seller
        seller = new User();
        seller.setUserId(UUID.randomUUID());
        seller.setEmail("seller@test.com");
        seller.setFullName("Seller Name");

        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setUser(seller);
        shop.setIsApproved(true);

        // Setup request
        request = CreateVoucherRequest.builder()
                .code("GIAM20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .quantity(100)
                .build();

        // Setup voucher entity
        voucher = new Voucher();
        voucher.setVoucherId(1L);
        voucher.setShop(shop);
        voucher.setCode("GIAM20");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(new BigDecimal("20"));
        voucher.setMinOrderValue(new BigDecimal("100000"));
        voucher.setMaxDiscountAmount(new BigDecimal("50000"));
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setQuantity(100);
        voucher.setHasDeleted(false);
        voucher.setCreatedAt(LocalDateTime.now());

        // Setup response
        response = CreateVoucherResponse.builder()
                .voucherId(1L)
                .shopId(1L)
                .code("GIAM20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .quantity(100)
                .createdAt(voucher.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should successfully create voucher with PERCENT discount type")
    void testCreateVoucherPercentType() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createVoucher(request, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("GIAM20");
        assertThat(result.getDiscountType()).isEqualTo("PERCENT");
        assertThat(result.getDiscountValue()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(result.getMessage()).isEqualTo("Voucher created successfully");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should successfully create voucher with FIXED discount type")
    void testCreateVoucherFixedType() {
        // Given
        request.setDiscountType("FIXED");
        request.setDiscountValue(new BigDecimal("50000"));
        voucher.setDiscountType("FIXED");
        voucher.setDiscountValue(new BigDecimal("50000"));

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createVoucher(request, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should throw exception when voucher code already exists")
    void testCreateVoucherCodeAlreadyExists() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher code already exists: GIAM20");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(userRepository, never()).findByEmail(any());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testCreateVoucherUserNotFound() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository, never()).findByUserUserId(any());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when shop not found")
    void testCreateVoucherShopNotFound() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop not found for user");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when shop is not approved")
    void testCreateVoucherShopNotApproved() {
        // Given
        shop.setIsApproved(false);

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Shop is not approved");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when end date is before start date")
    void testCreateVoucherInvalidDateRange() {
        // Given
        request.setEndDate(LocalDateTime.now().minusDays(1));
        request.setStartDate(LocalDateTime.now().plusDays(1));

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when percentage discount exceeds 100")
    void testCreateVoucherPercentageExceeds100() {
        // Given
        request.setDiscountValue(new BigDecimal("150"));

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentage discount cannot exceed 100%");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when percentage discount is zero or negative")
    void testCreateVoucherPercentageZeroOrNegative() {
        // Given
        request.setDiscountValue(BigDecimal.ZERO);

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));

        // When & Then
        assertThatThrownBy(() -> voucherService.createVoucher(request, seller.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentage discount must be greater than 0");

        verify(voucherRepository).existsByCode("GIAM20");
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set shop and hasDeleted correctly")
    void testCreateVoucherSetsShopAndHasDeleted() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createVoucher(request, seller.getEmail());

        // Then
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();

        assertThat(savedVoucher.getShop()).isEqualTo(shop);
        assertThat(savedVoucher.getHasDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should verify all validations are performed in correct order")
    void testCreateVoucherValidationOrder() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createVoucher(request, seller.getEmail());

        // Then
        var inOrder = inOrder(voucherRepository, userRepository, shopRepository);
        inOrder.verify(voucherRepository).existsByCode("GIAM20");
        inOrder.verify(userRepository).findByEmail(seller.getEmail());
        inOrder.verify(shopRepository).findByUserUserId(seller.getUserId());
        inOrder.verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should handle voucher with minimum required fields")
    void testCreateVoucherMinimumFields() {
        // Given
        request.setMinOrderValue(null);
        request.setMaxDiscountAmount(null);

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createVoucher(request, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void testCreateVoucherMapperCalls() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createVoucher(request, seller.getEmail());

        // Then
        verify(voucherMapper).toEntity(request);
        verify(voucherMapper).toCreateVoucherResponse(voucher);
    }

    @Test
    @DisplayName("Should return response with success message")
    void testCreateVoucherResponseMessage() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createVoucher(request, seller.getEmail());

        // Then
        assertThat(result.getMessage()).isEqualTo("Voucher created successfully");
    }

    @Test
    @DisplayName("Should handle voucher with 100% discount")
    void testCreateVoucherWith100PercentDiscount() {
        // Given
        request.setDiscountValue(new BigDecimal("100"));
        voucher.setDiscountValue(new BigDecimal("100"));

        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        CreateVoucherResponse result = voucherService.createVoucher(request, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify voucher is saved with correct data")
    void testCreateVoucherSavedData() {
        // Given
        when(voucherRepository.existsByCode("GIAM20")).thenReturn(false);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toCreateVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.createVoucher(request, seller.getEmail());

        // Then
        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());
        Voucher saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo("GIAM20");
        assertThat(saved.getDiscountType()).isEqualTo("PERCENT");
        assertThat(saved.getQuantity()).isEqualTo(100);
    }
}
