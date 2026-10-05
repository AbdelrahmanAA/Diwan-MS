package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.UserPreference;
import com.diwan.portfolio.repository.UserPreferenceRepository;
import com.diwan.portfolio.web.dto.Requests.PreferencesRequest;
import com.diwan.portfolio.web.dto.Responses.PreferencesResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PreferenceService {

    public static final String DEFAULT_LANGUAGE = "en";
    public static final String DEFAULT_CURRENCY = "USD";

    private final UserPreferenceRepository repository;

    public PreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse get(Long userId) {
        return repository.findById(userId)
                .map(p -> new PreferencesResponse(p.getLanguage(), p.getBaseCurrency()))
                .orElse(new PreferencesResponse(DEFAULT_LANGUAGE, DEFAULT_CURRENCY));
    }

    @Transactional
    public PreferencesResponse update(Long userId, PreferencesRequest req) {
        if (req.baseCurrency() != null && !Labels.isCurrency(req.baseCurrency())) {
            throw ApiException.badRequest("currency.unknown", req.baseCurrency());
        }
        UserPreference p = repository.findById(userId)
                .orElseGet(() -> new UserPreference(userId, DEFAULT_LANGUAGE, DEFAULT_CURRENCY));
        if (req.language() != null) p.setLanguage(req.language());
        if (req.baseCurrency() != null) p.setBaseCurrency(req.baseCurrency());
        repository.save(p);
        return new PreferencesResponse(p.getLanguage(), p.getBaseCurrency());
    }

    public String baseCurrencyOf(Long userId) {
        return repository.findById(userId).map(UserPreference::getBaseCurrency).orElse(DEFAULT_CURRENCY);
    }

    public Locale localeOf(Long userId) {
        return Locale.forLanguageTag(repository.findById(userId).map(UserPreference::getLanguage).orElse(DEFAULT_LANGUAGE));
    }
}
