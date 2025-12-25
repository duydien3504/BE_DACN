package com.example.DACN.service;

import com.example.DACN.dto.response.AddressListResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService GetAddresses Tests")
class AddressServiceGetTest {

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
    }

    @Test
    @DisplayName("Should get all addresses successfully")
    void testGetAddressesSuccess() {
        // Given
        UserAddress address1 = createAddress(1L, "Address 1", true);
        UserAddress address2 = createAddress(2L, "Address 2", false);
        List<UserAddress> addresses = List.of(address1, address2);

        AddressListResponse response1 = createResponse(1L, "Address 1", true);
        AddressListResponse response2 = createResponse(2L, "Address 2", false);
        List<AddressListResponse> responses = List.of(response1, response2);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUserUserIdAndHasDeletedFalse(any(UUID.class)))
                .thenReturn(addresses);
        when(addressMapper.toAddressListResponseList(addresses)).thenReturn(responses);

        // When
        List<AddressListResponse> result = addressService.getAddresses(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRecipientName()).isEqualTo("Address 1");
        assertThat(result.get(0).getIsDefault()).isTrue();
        assertThat(result.get(1).getRecipientName()).isEqualTo("Address 2");
        assertThat(result.get(1).getIsDefault()).isFalse();

        verify(userRepository).findByEmail(userEmail);
        verify(userAddressRepository).findByUserUserIdAndHasDeletedFalse(user.getUserId());
        verify(addressMapper).toAddressListResponseList(addresses);
    }

    @Test
    @DisplayName("Should return empty list when user has no addresses")
    void testGetAddressesEmpty() {
        // Given
        List<UserAddress> emptyList = new ArrayList<>();
        List<AddressListResponse> emptyResponseList = new ArrayList<>();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUserUserIdAndHasDeletedFalse(any(UUID.class)))
                .thenReturn(emptyList);
        when(addressMapper.toAddressListResponseList(emptyList)).thenReturn(emptyResponseList);

        // When
        List<AddressListResponse> result = addressService.getAddresses(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userRepository).findByEmail(userEmail);
        verify(userAddressRepository).findByUserUserIdAndHasDeletedFalse(user.getUserId());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetAddressesUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.getAddresses(userEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail(userEmail);
        verify(userAddressRepository, never()).findByUserUserIdAndHasDeletedFalse(any());
    }

    @Test
    @DisplayName("Should filter out deleted addresses")
    void testGetAddressesFilterDeleted() {
        // Given
        List<UserAddress> addresses = List.of(createAddress(1L, "Active Address", true));
        List<AddressListResponse> responses = List.of(createResponse(1L, "Active Address", true));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUserUserIdAndHasDeletedFalse(any(UUID.class)))
                .thenReturn(addresses);
        when(addressMapper.toAddressListResponseList(addresses)).thenReturn(responses);

        // When
        List<AddressListResponse> result = addressService.getAddresses(userEmail);

        // Then
        assertThat(result).hasSize(1);
        verify(userAddressRepository).findByUserUserIdAndHasDeletedFalse(user.getUserId());
    }

    private UserAddress createAddress(Long id, String recipientName, boolean isDefault) {
        UserAddress address = new UserAddress();
        address.setUserAddressId(id);
        address.setUser(user);
        address.setRecipientName(recipientName);
        address.setPhone("+84123456789");
        address.setProvince("Ho Chi Minh");
        address.setDistrict("District 1");
        address.setWard("Ward 1");
        address.setStreetAddress("123 Street");
        address.setIsDefault(isDefault);
        address.setHasDeleted(false);
        address.setCreatedAt(LocalDateTime.now());
        return address;
    }

    private AddressListResponse createResponse(Long id, String recipientName, boolean isDefault) {
        return AddressListResponse.builder()
                .userAddressId(id)
                .recipientName(recipientName)
                .phone("+84123456789")
                .province("Ho Chi Minh")
                .district("District 1")
                .ward("Ward 1")
                .streetAddress("123 Street")
                .isDefault(isDefault)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
