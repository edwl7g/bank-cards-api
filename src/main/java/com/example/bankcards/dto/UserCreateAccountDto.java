package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;

public record UserCreateAccountDto(
        String firstName,
        String lastName,
        String email,
        String phone,
        String identityDocumentNumber,
        UserRole userRole,
        UserStatus userStatus
) {
}