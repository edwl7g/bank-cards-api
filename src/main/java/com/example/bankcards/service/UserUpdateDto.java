package com.example.bankcards.service;

import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;

public record UserUpdateDto(
        String firstName,
        String lastName,
        String email,
        String phone,
        String identityDocumentNumber,
        UserRole userRole,
        UserStatus userStatus
) {}
