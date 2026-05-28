package com.example.bankcards.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Object details
) {
    public ErrorResponse(int status, String error, String message, String path, Object details) {
        this(LocalDateTime.now(), status, error, message, path, details);
    }
}