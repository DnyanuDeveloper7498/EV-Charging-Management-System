package com.evcharging.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AmountRequest(@NotNull(message = "Amount is required.") BigDecimal amount) {
}
