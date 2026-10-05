package com.diwan.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Encrypts a String column at rest: {@code @Convert(converter = EncryptedStringConverter.class)}.
 * Encrypted columns cannot be searched, sorted or aggregated in SQL; size them for the ciphertext
 * (roughly {@code 4/3 * (utf8 bytes + 28) + 12}).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : FieldEncryption.current().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FieldEncryption.current().decrypt(dbData);
    }
}
