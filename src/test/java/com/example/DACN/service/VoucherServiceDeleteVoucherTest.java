package com.example.DACN.service;

import com.example.DACN.dto.response.DeleteVoucherResponse;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.entity.Voucher;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
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
@DisplayName("VoucherService - Delete Voucher (Seller) Tests")
class VoucherServiceDeleteVoucherTest {

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

    private User seller;
    private Shop shop;
    private Voucher voucher;
    private DeleteVoucherResponse response;

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

        // Setup voucher
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

        // Setup response
        response = DeleteVoucherResponse.builder()
                .voucherId(1L)
                .message("Voucher deleted successfully")
                .build();
    }

    @Test
    @DisplayName("Should successfully delete voucher when seller owns it")
    void testDeleteVoucherSuccess() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Voucher deleted successfully");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).findById(1L);
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should set hasDeleted to true when deleting voucher")
    void testDeleteVoucherSetsHasDeleted() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();

        assertThat(savedVoucher.getHasDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testDeleteVoucherUserNotFound() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository, never()).findByUserUserId(any());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when shop not found")
    void testDeleteVoucherShopNotFound() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop not found for user");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher not found")
    void testDeleteVoucherVoucherNotFound() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found with ID: 1");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when voucher already deleted")
    void testDeleteVoucherAlreadyDeleted() {
        // Given
        voucher.setHasDeleted(true);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voucher not found with ID: 1");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when seller tries to delete another shop's voucher")
    void testDeleteVoucherUnauthorized() {
        // Given
        Shop anotherShop = new Shop();
        anotherShop.setShopId(2L);
        anotherShop.setShopName("Another Shop");
        voucher.setShop(anotherShop);

        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only delete your own shop vouchers");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when seller tries to delete platform voucher")
    void testDeleteVoucherPlatformVoucher() {
        // Given
        voucher.setShop(null); // Platform voucher
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        // When & Then
        assertThatThrownBy(() -> voucherService.deleteVoucher(1L, seller.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only delete your own shop vouchers");

        verify(userRepository).findByEmail(seller.getEmail());
        verify(shopRepository).findByUserUserId(seller.getUserId());
        verify(voucherRepository).findById(1L);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify all validations are performed in correct order")
    void testDeleteVoucherValidationOrder() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        var inOrder = inOrder(userRepository, shopRepository, voucherRepository);
        inOrder.verify(userRepository).findByEmail(seller.getEmail());
        inOrder.verify(shopRepository).findByUserUserId(seller.getUserId());
        inOrder.verify(voucherRepository).findById(1L);
        inOrder.verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void testDeleteVoucherMapperCall() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        verify(voucherMapper).toDeleteVoucherResponse(voucher);
    }

    @Test
    @DisplayName("Should return response with success message")
    void testDeleteVoucherResponseMessage() {
        // Given
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result.getMessage()).isEqualTo("Voucher deleted successfully");
    }

    @Test
    @DisplayName("Should handle voucher with FIXED discount type")
    void testDeleteVoucherFixedDiscount() {
        // Given
        voucher.setDiscountType("FIXED");
        voucher.setDiscountValue(new BigDecimal("50000"));
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete voucher regardless of quantity")
    void testDeleteVoucherWithZeroQuantity() {
        // Given
        voucher.setQuantity(0);
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete expired voucher")
    void testDeleteExpiredVoucher() {
        // Given
        voucher.setEndDate(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Should delete voucher that hasn't started yet")
    void testDeleteFutureVoucher() {
        // Given
        voucher.setStartDate(LocalDateTime.now().plusDays(1));
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(shopRepository.findByUserUserId(seller.getUserId())).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toDeleteVoucherResponse(voucher)).thenReturn(response);

        // When
        DeleteVoucherResponse result = voucherService.deleteVoucher(1L, seller.getEmail());

        // Then
        assertThat(result).isNotNull();
        verify(voucherRepository).save(any(Voucher.class));
    }
}
