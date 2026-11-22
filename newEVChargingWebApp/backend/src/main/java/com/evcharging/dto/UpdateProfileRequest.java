package com.evcharging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 100, message = "Full name must be at most 100 characters.")
        String fullName,

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "\\d{10}", message = "Mobile number must contain 10 digits.")
        String mobileNo) {
}
