package com.example.bankcards.dto;

import java.math.BigDecimal;

public record CreateAccountDto(
        Long userId,
        BigDecimal initialBalance
) {
}
