package com.evcharging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentAdminRequest(

        @NotBlank(message = "Full name is required.") @Size(max = 100) String fullName,

        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 150) String email,

        @NotBlank(message = "Mobile number is required.") @Pattern(regexp = "\\d{10}", message = "Mobile number must contain 10 digits.") String mobileNo,

        @NotBlank(message = "Password is required.") @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters.") String password,

        @NotBlank(message = "Department role is required.") @Pattern(regexp = "USER_ADMIN|VEHICLE_ADMIN|CHARGING_ADMIN|FINANCE_ADMIN|SUPPORT_ADMIN|STATION_ADMIN", message = "Invalid department admin role.") String role) {
}
