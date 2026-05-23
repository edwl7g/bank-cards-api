package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.CardStatus;

import java.time.LocalDateTime;

public record CardResponseDto(
        Long id,
        String maskedNumber,      // замаскированный номер
        LocalDateTime validityPeriod,
        CardStatus cardStatus,
        Long userId,
        String userFullName,
        Long accountId
) {}
