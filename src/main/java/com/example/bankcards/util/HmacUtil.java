package com.example.bankcards.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class HmacUtil {
    private static final String ALGO = "HmacSHA256";
    private static final byte[] SECRET = "your-secret-key-for-hmac".getBytes(StandardCharsets.UTF_8);

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