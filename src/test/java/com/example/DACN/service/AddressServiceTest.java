package com.example.DACN.service;

import com.example.DACN.dto.request.AddAddressRequest;
import com.example.DACN.dto.response.AddAddressResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.entity.UserAddress;
import com.example.DACN.mapper.AddressMapper;
import com.example.DACN.repository.UserAddressRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService Tests")
class AddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Role role;
    private AddAddressRequest request;
    private UserAddress userAddress;
    private AddAddressResponse response;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "customer@example.com";

        role = new Role();
        role.setRoleId(3L);
        role.setRoleName("Customer");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(userEmail);
        user.setFullName("Test Customer");
        user.setRole(role);

        request = AddAddressRequest.builder()
                .recipientName("Nguyen Van A")
                .phone("+84123456789")
                .province("Ho Chi Minh")
                .district("District 1")
                .ward("Ben Nghe Ward")
                .streetAddress("123 Nguyen Hue Street")
                .isDefault(false)
                .build();

        userAddress = new UserAddress();
        userAddress.setUserAddressId(1L);
        userAddress.setUser(user);
        userAddress.setRecipientName(request.getRecipientName());
        userAddress.setPhone(request.getPhone());
        userAddress.setProvince(request.getProvince());
        userAddress.setDistrict(request.getDistrict());
        userAddress.setWard(request.getWard());
        userAddress.setStreetAddress(request.getStreetAddress());
        userAddress.setIsDefault(request.getIsDefault());
        userAddress.setCreatedAt(LocalDateTime.now());

        response = AddAddressResponse.builder()
                .message("Address added successfully")
                .userAddressId(1L)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .streetAddress(request.getStreetAddress())
                .isDefault(request.getIsDefault())
                .createdAt(userAddress.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should add address successfully when not default")
    void testAddAddressNotDefault() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(addressMapper.toEntity(any(AddAddressRequest.class))).thenReturn(userAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);
        when(addressMapper.toAddAddressResponse(any(UserAddress.class))).thenReturn(response);

        // When
        AddAddressResponse result = addressService.addAddress(userEmail, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Address added successfully");
        assertThat(result.getRecipientName()).isEqualTo(request.getRecipientName());
        assertThat(result.getIsDefault()).isFalse();

        verify(userRepository).findByEmail(userEmail);
        verify(addressMapper).toEntity(request);
        verify(userAddressRepository).save(any(UserAddress.class));
        verify(userAddressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should add address as default and update existing default addresses")
    void testAddAddressAsDefault() {
        // Given
        request.setIsDefault(true);
        userAddress.setIsDefault(true);
        response.setIsDefault(true);

        UserAddress existingDefault = new UserAddress();
        existingDefault.setUserAddressId(2L);
        existingDefault.setUser(user);
        existingDefault.setIsDefault(true);

        List<UserAddress> existingAddresses = new ArrayList<>();
        existingAddresses.add(existingDefault);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUserUserIdAndHasDeletedFalse(any(UUID.class)))
                .thenReturn(existingAddresses);
        when(addressMapper.toEntity(any(AddAddressRequest.class))).thenReturn(userAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);
        when(addressMapper.toAddAddressResponse(any(UserAddress.class))).thenReturn(response);

        // When
        AddAddressResponse result = addressService.addAddress(userEmail, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsDefault()).isTrue();
        assertThat(existingDefault.getIsDefault()).isFalse();

        verify(userAddressRepository).findByUserUserIdAndHasDeletedFalse(user.getUserId());
        verify(userAddressRepository).saveAll(existingAddresses);
        verify(userAddressRepository).save(any(UserAddress.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testAddAddressUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.addAddress(userEmail, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail(userEmail);
        verify(userAddressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set user to address entity")
    void testSetUserToAddress() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(addressMapper.toEntity(any(AddAddressRequest.class))).thenReturn(userAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);
        when(addressMapper.toAddAddressResponse(any(UserAddress.class))).thenReturn(response);

        // When
        addressService.addAddress(userEmail, request);

        // Then
        verify(userAddressRepository)
                .save(argThat(address -> address.getUser() != null && address.getUser().equals(user)));
    }

    @Test
    @DisplayName("Should not update other addresses when new address is not default")
    void testNoUpdateWhenNotDefault() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(addressMapper.toEntity(any(AddAddressRequest.class))).thenReturn(userAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);
        when(addressMapper.toAddAddressResponse(any(UserAddress.class))).thenReturn(response);

        // When
        addressService.addAddress(userEmail, request);

        // Then
        verify(userAddressRepository, never()).findByUserUserIdAndHasDeletedFalse(any());
        verify(userAddressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle multiple existing default addresses")
    void testHandleMultipleDefaultAddresses() {
        // Given
        request.setIsDefault(true);
        userAddress.setIsDefault(true);

        UserAddress default1 = new UserAddress();
        default1.setUserAddressId(2L);
        default1.setIsDefault(true);

        UserAddress default2 = new UserAddress();
        default2.setUserAddressId(3L);
        default2.setIsDefault(true);

        List<UserAddress> existingAddresses = List.of(default1, default2);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUserUserIdAndHasDeletedFalse(any(UUID.class)))
                .thenReturn(existingAddresses);
        when(addressMapper.toEntity(any(AddAddressRequest.class))).thenReturn(userAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);
        when(addressMapper.toAddAddressResponse(any(UserAddress.class))).thenReturn(response);

        // When
        addressService.addAddress(userEmail, request);

        // Then
        assertThat(default1.getIsDefault()).isFalse();
        assertThat(default2.getIsDefault()).isFalse();
        verify(userAddressRepository).saveAll(existingAddresses);
    }
}
