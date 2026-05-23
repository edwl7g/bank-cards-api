package com.example.bankcards.dto;

import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.UserRole;

import java.util.List;

public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        UserRole userRole,
        List<Account> account,
        List<Card> card
) {
}