package com.example.bankcards.util;

public class CardUtil {
    // Вспомогательный метод для маскировки номера карты
     public static String maskCardNumber(String raw) {
        if (raw == null || raw.length() < 4) return "****";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 4) return "****";
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }
    // CardUtil.java
    public static String extractDigits(String input) {
        if (input == null) return "";
        return input.replaceAll("\\D", "");
    }
}
