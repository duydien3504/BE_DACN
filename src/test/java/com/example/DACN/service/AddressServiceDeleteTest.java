package com.example.DACN.service;

import com.example.DACN.dto.response.DeleteAddressResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.entity.UserAddress;
import com.example.DACN.repository.UserAddressRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService DeleteAddress Tests")
class AddressServiceDeleteTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private UserAddress userAddress;
    private String userEmail;
    private Long addressId;

    @BeforeEach
    void setUp() {
        userEmail = "customer@example.com";
        addressId = 1L;

        Role role = new Role();
        role.setRoleId(3L);
        role.setRoleName("Customer");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(userEmail);
        user.setRole(role);

        userAddress = new UserAddress();
        userAddress.setUserAddressId(addressId);
        userAddress.setUser(user);
        userAddress.setHasDeleted(false);
    }

    @Test
    @DisplayName("Should soft delete address successfully")
    void testDeleteAddressSuccess() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(userAddress));
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(userAddress);

        // When
        DeleteAddressResponse response = addressService.deleteAddress(userEmail, addressId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Address deleted successfully");
        assertThat(userAddress.getHasDeleted()).isTrue();

        verify(userRepository).findByEmail(userEmail);
        verify(userAddressRepository).findById(addressId);
        verify(userAddressRepository).save(userAddress);
    }

    @Test
    @DisplayName("Should throw exception when address belongs to another user")
    void testDeleteAddressNotBelongToUser() {
        // Given
        User anotherUser = new User();
        anotherUser.setUserId(UUID.randomUUID());
        userAddress.setUser(anotherUser);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(userAddress));

        // When & Then
        assertThatThrownBy(() -> addressService.deleteAddress(userEmail, addressId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Address does not belong to user");

        verify(userAddressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testDeleteAddressUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.deleteAddress(userEmail, addressId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userAddressRepository, never()).findById(any());
        verify(userAddressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when address not found")
    void testDeleteAddressNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findById(addressId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.deleteAddress(userEmail, addressId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Address not found");

        verify(userAddressRepository, never()).save(any());
    }
}
