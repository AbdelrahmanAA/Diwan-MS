package com.diwan.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AES-256-GCM encryption of single column values.
 *
 * <p>Keys are configured as {@code id:base64key[,id:base64key...]}; the FIRST key encrypts, every listed key
 * can decrypt. To rotate, put a new key in front and keep the old ones until every row was re-written.
 * Stored format: {@code enc:<keyId>:<base64url(iv || ciphertext+tag)>}. The key id is bound to the ciphertext
 * as associated data, so a value cannot be relabelled to another key. A value without the {@code enc:} prefix
 * is returned unchanged by {@link #decrypt}, which lets an existing plaintext column be migrated row by row.
 */
public final class FieldEncryptor {

    static final String PREFIX = "enc:";
    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9_-]{1,16}");
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String activeKeyId;
    private final Map<String, SecretKey> keys;
    private final SecureRandom random = new SecureRandom();

    private FieldEncryptor(String activeKeyId, Map<String, SecretKey> keys) {
        this.activeKeyId = activeKeyId;
        this.keys = keys;
    }

    /** @param spec {@code v2:base64key,v1:base64key} - each key must decode to exactly 32 bytes */
    public static FieldEncryptor fromSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("No data encryption key configured (diwan.crypto.keys)");
        }
        Map<String, SecretKey> keys = new LinkedHashMap<>();
        String active = null;
        for (String entry : spec.split(",")) {
            String trimmed = entry.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("Data encryption keys must look like id:base64key");
            }
            String id = trimmed.substring(0, colon);
            if (!KEY_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid data encryption key id '" + id + "'");
            }
            byte[] raw;
            try {
                raw = Base64.getDecoder().decode(trimmed.substring(colon + 1).trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Data encryption key '" + id + "' is not valid base64");
            }
            if (raw.length != 32) {
                throw new IllegalArgumentException("Data encryption key '" + id + "' must be 32 bytes (256 bit)");
            }
            if (keys.put(id, new SecretKeySpec(raw, "AES")) != null) {
                throw new IllegalArgumentException("Data encryption key id '" + id + "' is listed twice");
            }
            if (active == null) {
                active = id;
            }
        }
        return new FieldEncryptor(active, Map.copyOf(keys));
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(activeKeyId.getBytes(StandardCharsets.UTF_8));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
            return PREFIX + activeKeyId + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Field encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored; // null or legacy plaintext
        }
        int sep = stored.indexOf(':', PREFIX.length());
        if (sep < 0) {
            throw new IllegalStateException("Malformed encrypted value");
        }
        String keyId = stored.substring(PREFIX.length(), sep);
        SecretKey key = keys.get(keyId);
        if (key == null) {
            throw new IllegalStateException("Value was encrypted with unknown key '" + keyId + "'");
        }
        try {
            byte[] in = Base64.getUrlDecoder().decode(stored.substring(sep + 1));
            if (in.length < IV_BYTES + TAG_BITS / 8) {
                throw new IllegalStateException("Malformed encrypted value");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
            byte[] pt = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Field decryption failed (wrong key or tampered value)", e);
        }
    }

    /** True when the value is not encrypted with the active key (plaintext or an older key). */
    public boolean needsReEncryption(String stored) {
        return stored != null && !stored.startsWith(PREFIX + activeKeyId + ":");
    }
}
