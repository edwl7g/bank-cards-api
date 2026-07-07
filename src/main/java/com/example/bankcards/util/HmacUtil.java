package com.example.bankcards.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class HmacUtil {

    private static final String ALGO = "HmacSHA256";
    private static final byte[] SECRET;

    static {
        // Считываем секрет из переменной окружения
        String secret = System.getenv("HMAC_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("HMAC_SECRET environment variable is not set");
        }
        SECRET = secret.getBytes(StandardCharsets.UTF_8);
    }

    public static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(SECRET, ALGO));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}