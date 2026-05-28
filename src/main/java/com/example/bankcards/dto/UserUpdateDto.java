package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserUpdateDto(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number is invalid")
        String phone,

        @NotBlank(message = "Identity document number is required")
        String identityDocumentNumber,

        @NotNull(message = "User role is required")
        UserRole userRole,

        @NotNull(message = "User status is required")
        UserStatus userStatus
) {}
