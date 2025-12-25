package com.example.DACN.service;

import com.example.DACN.dto.request.AddAddressRequest;
import com.example.DACN.dto.response.AddAddressResponse;
import com.example.DACN.dto.response.AddressListResponse;
import com.example.DACN.dto.response.DeleteAddressResponse;
import com.example.DACN.entity.User;
import com.example.DACN.entity.UserAddress;
import com.example.DACN.mapper.AddressMapper;
import com.example.DACN.repository.UserAddressRepository;
import com.example.DACN.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public AddAddressResponse addAddress(String email, AddAddressRequest request) {
        log.info("Adding address for user: {}", email);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        // If new address is default, set all other addresses to non-default
        if (request.getIsDefault()) {
            List<UserAddress> existingAddresses = userAddressRepository
                    .findByUserUserIdAndHasDeletedFalse(user.getUserId());

            existingAddresses.forEach(address -> {
                if (address.getIsDefault()) {
                    address.setIsDefault(false);
                    log.info("Set address {} to non-default", address.getUserAddressId());
                }
            });

            userAddressRepository.saveAll(existingAddresses);
        }

        // Create new address
        UserAddress userAddress = addressMapper.toEntity(request);
        userAddress.setUser(user);

        UserAddress savedAddress = userAddressRepository.save(userAddress);
        log.info("Address added successfully with ID: {}", savedAddress.getUserAddressId());

        return addressMapper.toAddAddressResponse(savedAddress);
    }

    @Transactional(readOnly = true)
    public List<AddressListResponse> getAddresses(String email) {
        log.info("Getting addresses for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        List<UserAddress> addresses = userAddressRepository
                .findByUserUserIdAndHasDeletedFalse(user.getUserId());

        log.info("Found {} addresses for user: {}", addresses.size(), email);
        return addressMapper.toAddressListResponseList(addresses);
    }

    @Transactional
    public DeleteAddressResponse deleteAddress(String email, Long addressId) {
        log.info("Deleting address {} for user: {}", addressId, email);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        // Find address
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> {
                    log.warn("Address not found: {}", addressId);
                    return new RuntimeException("Address not found");
                });

        // Verify ownership
        if (!userAddress.getUser().getUserId().equals(user.getUserId())) {
            log.warn("Address {} does not belong to user {}", addressId, email);
            throw new RuntimeException("Address does not belong to user");
        }

        // Soft delete
        userAddress.setHasDeleted(true);
        userAddressRepository.save(userAddress);

        log.info("Address {} deleted successfully", addressId);

        return DeleteAddressResponse.builder()
                .message("Address deleted successfully")
                .build();
    }
}
