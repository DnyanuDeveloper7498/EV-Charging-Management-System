package com.evcharging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentAdminRequest(

        @NotBlank(message = "Full name is required.") @Size(max = 100) String fullName,

        @NotBlank(message = "Mobile number is required.") @Pattern(regexp = "\\d{10}", message = "Mobile number must contain 10 digits.") String mobileNo,

        @NotBlank(message = "Department role is required.") @Pattern(regexp = "USER_ADMIN|VEHICLE_ADMIN|CHARGING_ADMIN|FINANCE_ADMIN|SUPPORT_ADMIN|STATION_ADMIN", message = "Invalid department admin role.") String role)
{
}
