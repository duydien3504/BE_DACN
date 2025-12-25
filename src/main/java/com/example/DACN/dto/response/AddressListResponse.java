package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Address list item response")
public class AddressListResponse {

    @Schema(description = "Address ID", example = "1")
    Long userAddressId;

    @Schema(description = "Recipient's full name", example = "Nguyen Van A")
    String recipientName;

    @Schema(description = "Phone number", example = "+84123456789")
    String phone;

    @Schema(description = "Province/City", example = "Ho Chi Minh")
    String province;

    @Schema(description = "District", example = "District 1")
    String district;

    @Schema(description = "Ward/Commune", example = "Ben Nghe Ward")
    String ward;

    @Schema(description = "Street address", example = "123 Nguyen Hue Street")
    String streetAddress;

    @Schema(description = "Is default address", example = "true")
    Boolean isDefault;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;
}
