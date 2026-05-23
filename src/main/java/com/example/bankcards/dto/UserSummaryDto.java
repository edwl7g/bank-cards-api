package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;

public record UserSummaryDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserRole userRole,
        UserStatus userStatus
) {}
