package com.example.bankcards.dto;

import java.math.BigDecimal;

public record AccountResponseDto(Long id, BigDecimal balance) {
}
