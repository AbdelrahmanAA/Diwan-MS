package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.AllocationDimension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * Display names in the request language: enum labels from messages*.properties, country and currency
 * names from the JDK's CLDR data (which includes Arabic), so no translation table has to be maintained.
 */
@Component
public class Labels {

    public static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

    private final MessageSource messages;

    public Labels(MessageSource messages) {
        this.messages = messages;
    }

    public static Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    public String message(String code, Object... args) {
        return messages.getMessage(code, args, code, locale());
    }

    public String message(Locale locale, String code, Object... args) {
        return messages.getMessage(code, args, code, locale);
    }

    public String enumLabel(Enum<?> value) {
        return value == null ? null : message(value.getDeclaringClass().getSimpleName() + "." + value.name());
    }

    public String country(String code) {
        if (code == null) {
            return null;
        }
        String name = new Locale("", code).getDisplayCountry(locale());
        return name.isBlank() ? code : name;
    }

    public String currency(String code) {
        try {
            return Currency.getInstance(code).getDisplayName(locale());
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    /** Label of a grouping / target key in the given dimension. */
    public String key(AllocationDimension dimension, String key) {
        return switch (dimension) {
            case ASSET_TYPE -> message("AssetType." + key);
            case COUNTRY -> country(key);
            case CURRENCY -> currency(key);
        };
    }

    public static boolean isCountry(String code) {
        return code != null && COUNTRIES.contains(code);
    }

    public static boolean isCurrency(String code) {
        if (code == null || code.length() != 3) {
            return false;
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
