package com.hady.saas.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter JPA que criptografa automaticamente ao persistir
 * e descriptografa ao carregar da base.
 *
 * Uso na entidade:
 *   @Convert(converter = EncryptedStringConverter.class)
 *   private String mpAccessToken;
 *
 * Nenhum service ou repository precisa ser alterado —
 * a criptografia é transparente para o resto do código.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static AesEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(AesEncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return encryptionService.decrypt(dbData);
    }
}
