package com.diwan.portfolio.web.dto;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request bodies. Validation messages are message keys ({@code {key}}), resolved in the request language.
 * Identity never comes from a body: the user is the gateway-signed X-User-Id header.
 */
public final class Requests {

    private Requests() {}

    public record PortfolioRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.size}") String name,
            @Size(max = 2000, message = "{validation.size}") String description,
            @Pattern(regexp = "[A-Z]{3}", message = "{validation.currency}") String baseCurrency,
            @Size(max = 16, message = "{validation.size}") String color,
            @Size(max = 32, message = "{validation.size}") String icon,
            Integer sortOrder) {}

    public record HoldingRequest(
            @NotNull(message = "{validation.required}") AssetType assetType,
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.size}") String name,
            @Pattern(regexp = "[A-Z]{2}", message = "{validation.country}") String countryCode,
            @Pattern(regexp = "[A-Z]{3}", message = "{validation.currency}") String currency,
            @Size(max = 32, message = "{validation.size}") String symbol,
            @Size(max = 4000, message = "{validation.size}") String notes,
            Map<String, Object> attributes,
            @Valid TransactionRequest initialTransaction) {}

    public record TransactionRequest(
            @NotNull(message = "{validation.required}") TransactionType type,
            @NotNull(message = "{validation.required}") LocalDate tradeDate,
            @Positive(message = "{validation.positive}") BigDecimal quantity,
            @PositiveOrZero(message = "{validation.positive}") BigDecimal price,
            @Positive(message = "{validation.positive}") BigDecimal amount,
            @PositiveOrZero(message = "{validation.positive}") BigDecimal fees,
            @Size(max = 2000, message = "{validation.size}") String note) {}

    public record ValuationRequest(
            @NotNull(message = "{validation.required}") LocalDate valuationDate,
            @NotNull(message = "{validation.required}") @PositiveOrZero(message = "{validation.positive}") BigDecimal value,
            @Size(max = 2000, message = "{validation.size}") String note) {}

    public record TargetItem(
            @NotBlank(message = "{validation.required}") String key,
            @NotNull(message = "{validation.required}")
            @DecimalMin(value = "0", message = "{validation.percent}")
            @DecimalMax(value = "100", message = "{validation.percent}") BigDecimal percent) {}

    public record TargetsRequest(
            @NotNull(message = "{validation.required}") AllocationDimension dimension,
            @NotNull(message = "{validation.required}") @Size(max = 50, message = "{validation.size}")
            List<@Valid TargetItem> targets) {}

    public record GoalRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.size}") String name,
            @NotNull(message = "{validation.required}") @Positive(message = "{validation.positive}") BigDecimal targetAmount,
            LocalDate targetDate) {}

    public record TagRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 40, message = "{validation.size}") String name,
            @Size(max = 16, message = "{validation.size}") String color) {}

    public record HoldingTagsRequest(@NotNull(message = "{validation.required}") List<Long> tagIds) {}

    public record PreferencesRequest(
            @Pattern(regexp = "en|ar", message = "{validation.language}") String language,
            @Pattern(regexp = "[A-Z]{3}", message = "{validation.currency}") String baseCurrency) {}

    public record WatchRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 32, message = "{validation.size}") String symbol) {}

    /**
     * symbol: PRICE_ABOVE / PRICE_BELOW / DAILY_MOVE_PERCENT; holdingId: GAIN_PERCENT / LOSS_PERCENT;
     * portfolioId: ALLOCATION_DRIFT.
     */
    public record AlertRuleRequest(
            @NotNull(message = "{validation.required}") AlertType type,
            @Size(max = 32, message = "{validation.size}") String symbol,
            Long holdingId,
            Long portfolioId,
            @NotNull(message = "{validation.required}") @Positive(message = "{validation.positive}") BigDecimal threshold,
            Boolean active,
            @Size(max = 500, message = "{validation.size}") String note) {}
}
