package com.example.bankcards.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.bankcards.util.AesUtil;

@Service
public class CryptoService {
    private final AesUtil aesUtil;

    public CryptoService(@Value("${app.crypto.key}") String base64Key) {
        this.aesUtil = new AesUtil(base64Key);
    }

    public String encrypt(String data) {
        if (data == null) return null;
        return aesUtil.encrypt(data);
    }

    public String decrypt(String data) {
        if (data == null) return null;
        return aesUtil.decrypt(data);
    }
}