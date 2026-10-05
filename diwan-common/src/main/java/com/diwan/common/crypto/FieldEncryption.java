package com.diwan.common.crypto;

/**
 * Process-wide access to the configured {@link FieldEncryptor}. JPA instantiates attribute converters itself
 * (and slice tests such as {@code @DataJpaTest} do not load the Diwan auto-configuration), so
 * {@link EncryptedStringConverter} looks the encryptor up here at use time instead of having it injected.
 */
public final class FieldEncryption {

    private static volatile FieldEncryptor current;

    private FieldEncryption() {}

    public static void install(FieldEncryptor encryptor) {
        current = encryptor;
    }

    public static FieldEncryptor current() {
        FieldEncryptor e = current;
        if (e == null) {
            throw new IllegalStateException("Field encryption is not configured: set diwan.crypto.keys");
        }
        return e;
    }
}
