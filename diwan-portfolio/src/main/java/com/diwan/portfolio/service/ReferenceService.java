package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.domain.TransactionType;
import com.diwan.portfolio.web.dto.Responses.CodeLabel;
import com.diwan.portfolio.web.dto.Responses.ReferenceResponse;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Set;

/** Codes plus labels in the request language, for pickers in the app. */
@Service
public class ReferenceService {

    /** Currencies offered in pickers (any ISO code is accepted by the API). */
    private static final Set<String> COMMON_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "CHF", "JPY", "CNY", "CAD", "AUD", "EGP", "SAR", "AED", "KWD", "QAR", "BHD", "OMR",
            "JOD", "MAD", "TRY", "INR");

    private final Labels labels;
    private final InstrumentService instruments;

    public ReferenceService(Labels labels, InstrumentService instruments) {
        this.labels = labels;
        this.instruments = instruments;
    }

    public ReferenceResponse reference() {
        Collator collator = Collator.getInstance(Labels.locale());
        Comparator<CodeLabel> byLabel = Comparator.comparing(CodeLabel::label, collator);
        return new ReferenceResponse(
                enums(AssetType.values()),
                enums(TransactionType.values()),
                enums(AlertType.values()),
                enums(AllocationDimension.values()),
                Labels.COUNTRIES.stream().map(c -> new CodeLabel(c, labels.country(c))).sorted(byLabel).toList(),
                Currency.getAvailableCurrencies().stream().map(Currency::getCurrencyCode)
                        .filter(COMMON_CURRENCIES::contains)
                        .map(c -> new CodeLabel(c, labels.currency(c))).sorted(byLabel).toList(),
                List.of(new CodeLabel("en", "English"), new CodeLabel("ar", "العربية")),
                instruments.providerName());
    }

    private List<CodeLabel> enums(Enum<?>[] values) {
        return Arrays.stream(values).map(v -> new CodeLabel(v.name(), labels.enumLabel(v))).toList();
    }
}
