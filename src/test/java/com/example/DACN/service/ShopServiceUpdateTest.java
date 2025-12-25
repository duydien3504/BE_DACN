package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.UpdateShopRequest;
import com.example.DACN.dto.response.UpdateShopResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.Shop;
import com.example.DACN.entity.User;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.ShopMapper;
import com.example.DACN.repository.PaymentRepository;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.ShopRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceUpdateTest {

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

    @Mock
    private ShopMapper shopMapper;

    @InjectMocks
    private ShopService shopService;

    private User sellerUser;
    private Role sellerRole;
    private Shop existingShop;
    private UpdateShopRequest updateRequest;
    private UpdateShopResponse updateResponse;

    @BeforeEach
    void setUp() {
        // Setup seller role
        sellerRole = new Role();
        sellerRole.setRoleId(2L);
        sellerRole.setRoleName(RoleConstants.SELLER);

        // Setup seller user
        sellerUser = new User();
        sellerUser.setUserId(UUID.randomUUID());
        sellerUser.setEmail("seller@example.com");
        sellerUser.setRole(sellerRole);

        // Setup existing shop
        existingShop = new Shop();
        existingShop.setShopId(1L);
        existingShop.setShopName("Old Shop Name");
        existingShop.setShopDescription("Old Description");
        existingShop.setLogoUrl("https://old-logo.com/logo.jpg");
        existingShop.setUser(sellerUser);
        existingShop.setIsApproved(true);
        existingShop.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // Setup update request
        updateRequest = new UpdateShopRequest();
        updateRequest.setShopName("New Shop Name");
        updateRequest.setShopDescription("New Description");

        // Setup update response
        updateResponse = new UpdateShopResponse();
        updateResponse.setShopId(1L);
        updateResponse.setShopName("New Shop Name");
        updateResponse.setShopDescription("New Description");
        updateResponse.setLogoUrl("https://old-logo.com/logo.jpg");
        updateResponse.setIsApproved(true);
        updateResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void updateMyShop_WithValidData_ShouldUpdateSuccessfully() throws IOException {
        // Arrange
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", updateRequest, null);

        // Assert
        assertNotNull(result);
        assertEquals("New Shop Name", result.getShopName());
        assertEquals("New Description", result.getShopDescription());
        verify(userRepository, times(1)).findByEmail("seller@example.com");
        verify(shopRepository, times(1)).findByUserUserId(sellerUser.getUserId());
        verify(shopRepository, times(1)).save(any(Shop.class));
        verify(shopMapper, times(1)).toUpdateShopResponse(any(Shop.class));
    }

    @Test
    void updateMyShop_WithNewLogo_ShouldUploadToCloudinary() throws IOException {
        // Arrange
        MultipartFile logoFile = mock(MultipartFile.class);
        when(logoFile.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadImage(logoFile)).thenReturn("https://new-logo.com/logo.jpg");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", updateRequest, logoFile);

        // Assert
        assertNotNull(result);
        verify(cloudinaryService, times(1)).uploadImage(logoFile);
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    void updateMyShop_WithOnlyShopName_ShouldUpdateOnlyName() throws IOException {
        // Arrange
        UpdateShopRequest nameOnlyRequest = new UpdateShopRequest();
        nameOnlyRequest.setShopName("Updated Name Only");

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", nameOnlyRequest, null);

        // Assert
        assertNotNull(result);
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    void updateMyShop_WithOnlyDescription_ShouldUpdateOnlyDescription() throws IOException {
        // Arrange
        UpdateShopRequest descOnlyRequest = new UpdateShopRequest();
        descOnlyRequest.setShopDescription("Updated Description Only");

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", descOnlyRequest, null);

        // Assert
        assertNotNull(result);
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    void updateMyShop_WithUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> shopService.updateMyShop("nonexistent@example.com", updateRequest, null));
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verify(shopRepository, never()).findByUserUserId(any());
    }

    @Test
    void updateMyShop_WithNonSellerUser_ShouldThrowIllegalStateException() {
        // Arrange
        Role customerRole = new Role();
        customerRole.setRoleId(1L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        User customerUser = new User();
        customerUser.setUserId(UUID.randomUUID());
        customerUser.setEmail("customer@example.com");
        customerUser.setRole(customerRole);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> shopService.updateMyShop("customer@example.com", updateRequest, null));
        verify(userRepository, times(1)).findByEmail("customer@example.com");
        verify(shopRepository, never()).findByUserUserId(any());
    }

    @Test
    void updateMyShop_WithShopNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> shopService.updateMyShop("seller@example.com", updateRequest, null));
        verify(userRepository, times(1)).findByEmail("seller@example.com");
        verify(shopRepository, times(1)).findByUserUserId(sellerUser.getUserId());
        verify(shopRepository, never()).save(any());
    }

    @Test
    void updateMyShop_WithCloudinaryFailure_ShouldThrowIOException() throws IOException {
        // Arrange
        MultipartFile logoFile = mock(MultipartFile.class);
        when(logoFile.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadImage(logoFile)).thenThrow(new IOException("Cloudinary upload failed"));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));

        // Act & Assert
        assertThrows(IOException.class, () -> shopService.updateMyShop("seller@example.com", updateRequest, logoFile));
        verify(cloudinaryService, times(1)).uploadImage(logoFile);
        verify(shopRepository, never()).save(any());
    }

    @Test
    void updateMyShop_WithEmptyFields_ShouldNotUpdateEmptyFields() throws IOException {
        // Arrange
        UpdateShopRequest emptyRequest = new UpdateShopRequest();
        emptyRequest.setShopName("");
        emptyRequest.setShopDescription("");

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", emptyRequest, null);

        // Assert
        assertNotNull(result);
        // Shop name and description should remain unchanged
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    void updateMyShop_WithNullFields_ShouldNotUpdateNullFields() throws IOException {
        // Arrange
        UpdateShopRequest nullRequest = new UpdateShopRequest();
        nullRequest.setShopName(null);
        nullRequest.setShopDescription(null);

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(sellerUser.getUserId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(existingShop);
        when(shopMapper.toUpdateShopResponse(any(Shop.class))).thenReturn(updateResponse);

        // Act
        UpdateShopResponse result = shopService.updateMyShop("seller@example.com", nullRequest, null);

        // Assert
        assertNotNull(result);
        verify(shopRepository, times(1)).save(any(Shop.class));
    }
}
