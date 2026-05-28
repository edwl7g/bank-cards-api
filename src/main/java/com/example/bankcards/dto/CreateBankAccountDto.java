package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateBankAccountDto(
        @NotNull(message = "User ID is required")
        Long userId,

        @PositiveOrZero(message = "Initial balance must be >= 0")
        BigDecimal initialBalance
) {}
