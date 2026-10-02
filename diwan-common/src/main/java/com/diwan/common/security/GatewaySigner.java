package com.diwan.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * Signs/verifies the identity headers the gateway forwards to downstream services.
 * Services trust X-User-Id only if the HMAC over (userId, email, role, timestamp) is valid,
 * so a caller that reaches a service directly cannot forge an identity.
 * The HMAC key is derived from the shared JWT secret, so no extra secret has to be managed.
 */
public final class GatewaySigner {

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_EMAIL = "X-User-Email";
    public static final String HDR_ROLE = "X-User-Role";
    public static final String HDR_TIMESTAMP = "X-Gateway-Timestamp";
    public static final String HDR_SIGNATURE = "X-Gateway-Signature";

    /** Every header a client must never be able to supply (includes the retired X-Gateway-Validated). */
    public static final List<String> TRUSTED_HEADERS = List.of(
            HDR_USER_ID, HDR_EMAIL, HDR_ROLE, HDR_TIMESTAMP, HDR_SIGNATURE, "X-Gateway-Validated");

    private static final String HMAC = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final byte[] key;

    private GatewaySigner(byte[] key) {
        this.key = key;
    }

    public static GatewaySigner fromSecret(String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        return new GatewaySigner(hmac(secret.getBytes(StandardCharsets.UTF_8),
                "diwan-gateway-internal-v1".getBytes(StandardCharsets.UTF_8)));
    }

    public String sign(String userId, String email, String role, long timestampMillis) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac(key, canonical(userId, email, role, timestampMillis)));
    }

    public boolean verify(String userId, String email, String role, long timestampMillis, String signature) {
        if (signature == null) return false;
        byte[] expected = sign(userId, email, role, timestampMillis).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] canonical(String userId, String email, String role, long ts) {
        return (userId + "\n" + nullToEmpty(email) + "\n" + nullToEmpty(role) + "\n" + ts)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
