package com.diwan.portfolio.web.dto;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.domain.PriceSource;
import com.diwan.portfolio.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response bodies. Codes (assetType, countryCode, type...) are stable for client logic; the matching
 * *Label fields are display text in the request language (Accept-Language: ar | en).
 * Amounts without a currency suffix are in the portfolio's base currency; *Local amounts are in the
 * holding's own currency.
 */
public final class Responses {

    private Responses() {}

    public record CodeLabel(String code, String label) {}

    public record PortfolioResponse(Long id, String name, String description, String baseCurrency, String color,
                                    String icon, int sortOrder, Instant createdAt, Instant updatedAt) {}

    public record Totals(BigDecimal marketValue, BigDecimal costBasis, BigDecimal unrealizedPnl,
                         BigDecimal unrealizedPnlPercent, BigDecimal realizedPnl, BigDecimal netIncome,
                         BigDecimal totalReturn, BigDecimal totalReturnPercent, BigDecimal dayChange,
                         BigDecimal dayChangePercent) {}

    public record PortfolioOverview(PortfolioResponse portfolio, int holdingsCount, Totals totals,
                                    List<String> missingRates) {}

    public record TagResponse(Long id, String name, String color) {}

    public record HoldingResponse(
            Long id, Long portfolioId, String name,
            AssetType assetType, String assetTypeLabel,
            String countryCode, String countryLabel,
            String currency, String symbol, String instrumentName,
            BigDecimal quantity, BigDecimal averageCostLocal, BigDecimal costBasisLocal,
            BigDecimal priceLocal, PriceSource priceSource, Instant priceAsOf, BigDecimal marketValueLocal,
            boolean convertible, BigDecimal marketValue, BigDecimal costBasis,
            BigDecimal unrealizedPnl, BigDecimal unrealizedPnlPercent, BigDecimal realizedPnl,
            BigDecimal netIncome, BigDecimal totalReturn, BigDecimal dayChange, BigDecimal dayChangePercent,
            BigDecimal weightPercent, String notes, Map<String, Object> attributes, boolean closed,
            List<TagResponse> tags, Instant createdAt) {}

    public record GroupNode(AllocationDimension dimension, String key, String label, BigDecimal weightPercent,
                            Totals totals, List<GroupNode> groups, List<HoldingResponse> holdings) {}

    public record SummaryResponse(PortfolioResponse portfolio, List<AllocationDimension> groupBy, Totals totals,
                                  List<GroupNode> groups, List<String> missingRates, LocalDate fxAsOf) {}

    public record TransactionResponse(Long id, Long holdingId, TransactionType type, String typeLabel,
                                      LocalDate tradeDate, BigDecimal quantity, BigDecimal price, BigDecimal amount,
                                      BigDecimal fees, String note, Instant createdAt) {}

    public record ValuationResponse(Long id, Long holdingId, LocalDate valuationDate, BigDecimal value,
                                    String note, Instant createdAt) {}

    public record AllocationRow(String key, String label, BigDecimal value, BigDecimal currentPercent,
                                BigDecimal targetPercent, BigDecimal driftPercent, BigDecimal suggestedChange) {}

    /** suggestedChange: positive = buy / add this much, negative = sell / reduce (base currency). */
    public record AllocationResponse(AllocationDimension dimension, String dimensionLabel, String baseCurrency,
                                     BigDecimal totalValue, boolean hasTargets, BigDecimal maxDriftPercent,
                                     List<AllocationRow> rows) {}

    public record TargetResponse(AllocationDimension dimension, String key, String label, BigDecimal percent) {}

    public record GoalResponse(Long id, Long portfolioId, String name, BigDecimal targetAmount, LocalDate targetDate,
                               String currency, BigDecimal currentValue, BigDecimal progressPercent,
                               BigDecimal remainingAmount, Integer monthsLeft, BigDecimal monthlyNeeded,
                               boolean reached) {}

    public record PreferencesResponse(String language, String baseCurrency) {}

    public record ReferenceResponse(List<CodeLabel> assetTypes, List<CodeLabel> transactionTypes,
                                    List<CodeLabel> alertTypes, List<CodeLabel> dimensions,
                                    List<CodeLabel> countries, List<CodeLabel> currencies,
                                    List<CodeLabel> languages, String marketDataProvider) {}

    public record QuoteResponse(String symbol, BigDecimal price, BigDecimal previousClose, BigDecimal change,
                                BigDecimal changePercent, BigDecimal high, BigDecimal low, Instant time,
                                String provider) {}

    public record InstrumentResponse(String symbol, String name, String exchange, String countryCode,
                                     String countryLabel, String currency, AssetType assetType,
                                     String assetTypeLabel, QuoteResponse quote) {}

    public record SymbolMatchResponse(String symbol, String description, String type) {}

    public record PricePoint(LocalDate date, BigDecimal close) {}

    public record WatchlistItemResponse(Long id, InstrumentResponse instrument, Instant addedAt) {}

    public record NewsResponse(Long id, String symbol, String instrumentName, String headline, String summary,
                               String source, String url, String imageUrl, String language, Instant publishedAt) {}

    public record AlertRuleResponse(Long id, AlertType type, String typeLabel, String symbol, Long holdingId,
                                    Long portfolioId, BigDecimal threshold, boolean active, String note,
                                    Instant lastTriggeredAt, Instant createdAt) {}

    public record AlertResponse(Long id, Long ruleId, AlertType type, String typeLabel, String symbol,
                                Long holdingId, Long portfolioId, String title, String message,
                                BigDecimal observedValue, BigDecimal threshold, Instant triggeredAt, Instant readAt) {}

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
}
