package com.example.bankcards.dto;

import java.math.BigDecimal;

public record CreateBankAccountDto(
        Long userId,
        BigDecimal initialBalance
) {
}
