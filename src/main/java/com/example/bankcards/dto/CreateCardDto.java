package com.example.bankcards.dto;

import java.time.LocalDateTime;

public record CreateCardDto(
        Long userId,
        Long accountId,
        LocalDateTime validityPeriod
) {
}
