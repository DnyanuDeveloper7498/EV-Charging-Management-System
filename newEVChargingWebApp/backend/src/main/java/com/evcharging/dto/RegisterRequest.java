package com.evcharging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 100, message = "Full name must be at most 100 characters.")
        String fullName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 150, message = "Email must be at most 150 characters.")
        String email,

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "\\d{10}", message = "Mobile number must contain 10 digits.")
        String mobileNo,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters.")
        String password) {
}
