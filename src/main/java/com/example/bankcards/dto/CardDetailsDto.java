package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.CardStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CardDetailsDto(
        Long id,
        String maskedNumber,
        String fullNumber,   // только для админа, для пользователя null
        LocalDateTime validityPeriod,
        CardStatus cardStatus,
        Long userId,
        String userFullName,
        String userEmail,
        Long accountId,
        BigDecimal balance
) {}