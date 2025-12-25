package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Add address request")
public class AddAddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    @Schema(description = "Recipient's full name", example = "Nguyen Van A")
    String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid phone number format")
    @Schema(description = "Phone number", example = "+84123456789")
    String phone;

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province must not exceed 100 characters")
    @Schema(description = "Province/City", example = "Ho Chi Minh")
    String province;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(description = "District", example = "District 1")
    String district;

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    @Schema(description = "Ward/Commune", example = "Ben Nghe Ward")
    String ward;

    @NotBlank(message = "Street address is required")
    @Schema(description = "Street address", example = "123 Nguyen Hue Street")
    String streetAddress;

    @NotNull(message = "Default flag is required")
    @Schema(description = "Set as default address", example = "true")
    Boolean isDefault;
}
