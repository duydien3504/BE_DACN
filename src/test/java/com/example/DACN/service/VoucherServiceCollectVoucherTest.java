package com.example.DACN.service;

import com.example.DACN.dto.response.CollectVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.entity.UserVoucher;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.DuplicateResourceException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService - Collect Voucher Tests")
class VoucherServiceCollectVoucherTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherService voucherService;

    private User customer;
    private Shop shop;
    private Voucher voucher;
    private UserVoucher userVoucher;
    private CollectVoucherResponse response;

    @BeforeEach
    void setUp() {
        // Setup customer
        customer = new User();
        customer.setUserId(UUID.randomUUID());
        customer.setEmail("customer@test.com");
        customer.setFullName("Customer Name");

        // Setup shop
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setIsApproved(true);

        // Setup voucher (active, valid dates, has quantity)
        voucher = new Voucher();
        voucher.setVoucherId(1L);
        voucher.setShop(shop);
        voucher.setCode("GIAM20");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(new BigDecimal("20"));
        voucher.setMinOrderValue(new BigDecimal("100000"));
        voucher.setMaxDiscountAmount(new BigDecimal("50000"));
        voucher.setStartDate(LocalDateTime.now().minusDays(1));
        voucher.setEndDate(LocalDateTime.now().plusDays(30));
        voucher.setQuantity(100);
        voucher.setHasDeleted(false);
        voucher.setCreatedAt(LocalDateTime.now());

        // Setup user voucher
        userVoucher = new UserVoucher();
        userVoucher.setUserVoucherId(1L);
        userVoucher.setUser(customer);
        userVoucher.setVoucher(voucher);
        userVoucher.setIsUsed(false);
        userVoucher.setUsedAtOrder(null);
        userVoucher.setCreatedAt(LocalDateTime.now());

        // Setup response
        response = CollectVoucherResponse.builder()
                .userVoucherId(1L)
                .voucherId(1L)
                .code("GIAM20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .collectedAt(userVoucher.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should successfully collect voucher")
    void testCollectVoucherSuccess() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserVoucherId()).isEqualTo(1L);
        assertThat(result.getVoucherId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("GIAM20");
        assertThat(result.getMessage()).isEqualTo("Voucher collected successfully");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository).existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L);
        verify(voucherRepository).save(voucher);
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should decrease voucher quantity when collected")
    void testCollectVoucherDecreasesQuantity() {
        // Given
        int initialQuantity = voucher.getQuantity();
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();

        assertThat(savedVoucher.getQuantity()).isEqualTo(initialQuantity - 1);
    }

    @Test
    @DisplayName("Should create UserVoucher with correct properties")
    void testCollectVoucherCreatesUserVoucher() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        ArgumentCaptor<UserVoucher> userVoucherCaptor = ArgumentCaptor.forClass(UserVoucher.class);
        verify(userVoucherRepository).save(userVoucherCaptor.capture());
        UserVoucher savedUserVoucher = userVoucherCaptor.getValue();

        assertThat(savedUserVoucher.getUser()).isEqualTo(customer);
        assertThat(savedUserVoucher.getVoucher()).isEqualTo(voucher);
        assertThat(savedUserVoucher.getIsUsed()).isFalse();
        assertThat(savedUserVoucher.getUsedAtOrder()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testCollectVoucherUserNotFound() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository, never()).findById(any());
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher not found")
    void testCollectVoucherVoucherNotFound() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found with ID: 1");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher is deleted")
    void testCollectVoucherVoucherDeleted() {
        // Given
        voucher.setHasDeleted(true);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher is no longer available");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already collected voucher")
    void testCollectVoucherAlreadyCollected() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("You have already collected this voucher");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository).existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher not yet available")
    void testCollectVoucherNotYetAvailable() {
        // Given
        voucher.setStartDate(LocalDateTime.now().plusDays(1));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher is not yet available");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher has expired")
    void testCollectVoucherExpired() {
        // Given
        voucher.setEndDate(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher has expired");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher is out of stock")
    void testCollectVoucherOutOfStock() {
        // Given
        voucher.setQuantity(0);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> voucherService.collectVoucher(1L, customer.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher is out of stock");

        verify(userRepository).findByEmail(customer.getEmail());
        verify(voucherRepository).findById(1L);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle voucher with quantity of 1")
    void testCollectVoucherLastOne() {
        // Given
        voucher.setQuantity(1);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        assertThat(voucherCaptor.getValue().getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle platform voucher (shop is null)")
    void testCollectPlatformVoucher() {
        // Given
        voucher.setShop(null); // Platform voucher
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should verify all validations are performed in correct order")
    void testCollectVoucherValidationOrder() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        var inOrder = inOrder(userRepository, voucherRepository, userVoucherRepository);
        inOrder.verify(userRepository).findByEmail(customer.getEmail());
        inOrder.verify(voucherRepository).findById(1L);
        inOrder.verify(userVoucherRepository).existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L);
        inOrder.verify(voucherRepository).save(voucher);
        inOrder.verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void testCollectVoucherMapperCall() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        verify(voucherMapper).toCollectVoucherResponse(userVoucher);
    }

    @Test
    @DisplayName("Should handle voucher with FIXED discount type")
    void testCollectVoucherFixedDiscount() {
        // Given
        voucher.setDiscountType("FIXED");
        voucher.setDiscountValue(new BigDecimal("50000"));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should return response with success message")
    void testCollectVoucherResponseMessage() {
        // Given
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result.getMessage()).isEqualTo("Voucher collected successfully");
    }

    @Test
    @DisplayName("Should handle voucher starting exactly now")
    void testCollectVoucherStartingNow() {
        // Given
        voucher.setStartDate(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should handle voucher ending soon")
    void testCollectVoucherEndingSoon() {
        // Given
        voucher.setEndDate(LocalDateTime.now().plusSeconds(10));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.existsByUserUserIdAndVoucherVoucherId(customer.getUserId(), 1L))
                .thenReturn(false);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenReturn(userVoucher);
        when(voucherMapper.toCollectVoucherResponse(userVoucher)).thenReturn(response);

        // When
        CollectVoucherResponse result = voucherService.collectVoucher(1L, customer.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }
}
