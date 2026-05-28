package com.example.bankcards.dto;

import java.math.BigDecimal;

public record ResponseCreateBankAccountDto(
        Long accountId,
        BigDecimal initialBalance
) {
}