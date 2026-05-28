package com.example.bankcards.util.converter;

import com.example.bankcards.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter(autoApply = false)
public class CryptoConverter implements AttributeConverter<String, String> {

    private static CryptoService cryptoService;

    @Autowired
    public void setCryptoService(CryptoService service) {
        cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return (attribute == null) ? null : cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return (dbData == null) ? null : cryptoService.decrypt(dbData);
    }
}