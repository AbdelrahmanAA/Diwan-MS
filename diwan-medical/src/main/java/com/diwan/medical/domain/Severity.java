package com.diwan.medical.domain;

import com.diwan.common.web.ApiException;

import java.util.Arrays;
import java.util.Locale;

/** Severity of an allergy, mildest first. */
public enum Severity {
    MILD, MODERATE, SEVERE, LIFE_THREATENING;

    /** The stored code of a client value; null when blank. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String code = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).map(Enum::name).filter(code::equals).findFirst()
                .orElseThrow(() -> ApiException.badRequest("medical.severity.invalid"));
    }

    /** For sorting: the most serious first, unknown values last. */
    public static int rank(String code) {
        return Arrays.stream(values()).filter(s -> s.name().equals(code))
                .mapToInt(s -> -s.ordinal() - 1).findFirst().orElse(0);
    }
}
