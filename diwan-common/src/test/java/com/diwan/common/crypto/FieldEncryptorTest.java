package com.diwan.common.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptorTest {

    private static String key(int seed) {
        byte[] k = new byte[32];
        for (int i = 0; i < k.length; i++) k[i] = (byte) (seed + i);
        return Base64.getEncoder().encodeToString(k);
    }

    private final FieldEncryptor v1 = FieldEncryptor.fromSpec("v1:" + key(1));

    @Test
    void roundTripsArabicAndEnglishAndHidesThePlaintext() {
        String plain = "شقة التجمع الخامس - Apartment 12";
        String stored = v1.encrypt(plain);

        assertThat(stored).startsWith("enc:v1:").doesNotContain("Apartment").doesNotContain("شقة");
        assertThat(v1.decrypt(stored)).isEqualTo(plain);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        assertThat(v1.encrypt("x")).isNotEqualTo(v1.encrypt("x"));
    }

    @Test
    void nullStaysNullAndLegacyPlaintextIsReadAsIs() {
        assertThat(v1.encrypt(null)).isNull();
        assertThat(v1.decrypt(null)).isNull();
        assertThat(v1.decrypt("old plain value")).isEqualTo("old plain value");
        assertThat(v1.needsReEncryption("old plain value")).isTrue();
    }

    @Test
    void tamperedValueIsRejected() {
        String stored = v1.encrypt("secret");
        int i = stored.length() - 10; // a ciphertext character (the last one may only carry padding bits)
        String tampered = stored.substring(0, i) + (stored.charAt(i) == 'A' ? 'B' : 'A') + stored.substring(i + 1);

        assertThatThrownBy(() -> v1.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyCannotDecrypt() {
        FieldEncryptor other = FieldEncryptor.fromSpec("v1:" + key(99));
        assertThatThrownBy(() -> other.decrypt(v1.encrypt("secret"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rotationEncryptsWithTheFirstKeyAndStillReadsTheOldOne() {
        String old = v1.encrypt("secret");
        FieldEncryptor rotated = FieldEncryptor.fromSpec("v2:" + key(2) + ", v1:" + key(1));

        assertThat(rotated.decrypt(old)).isEqualTo("secret");
        assertThat(rotated.needsReEncryption(old)).isTrue();
        String fresh = rotated.encrypt("secret");
        assertThat(fresh).startsWith("enc:v2:");
        assertThat(rotated.needsReEncryption(fresh)).isFalse();
    }

    @Test
    void relabellingTheKeyIdBreaksDecryption() {
        FieldEncryptor both = FieldEncryptor.fromSpec("v1:" + key(1) + ",v2:" + key(1));
        String stored = both.encrypt("secret"); // enc:v1:...
        String relabelled = stored.replaceFirst("enc:v1:", "enc:v2:");
        assertThatThrownBy(() -> both.decrypt(relabelled)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidKeySpecsFailFastWithoutEchoingTheKey() {
        assertThatThrownBy(() -> FieldEncryptor.fromSpec("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FieldEncryptor.fromSpec("nokeyid")).isInstanceOf(IllegalArgumentException.class);
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> FieldEncryptor.fromSpec("v1:" + shortKey))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining(shortKey);
        assertThatThrownBy(() -> FieldEncryptor.fromSpec("v1:" + key(1) + ",v1:" + key(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void converterUsesTheInstalledEncryptor() {
        FieldEncryption.install(v1);
        EncryptedStringConverter converter = new EncryptedStringConverter();

        String column = converter.convertToDatabaseColumn("note");
        assertThat(column).startsWith("enc:v1:");
        assertThat(converter.convertToEntityAttribute(column)).isEqualTo("note");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}
