package com.example.bankcards.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateCardDto(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotNull(message = "Validity period is required")
        @Future(message = "Validity period must be in the future")
        LocalDateTime validityPeriod
) {}
