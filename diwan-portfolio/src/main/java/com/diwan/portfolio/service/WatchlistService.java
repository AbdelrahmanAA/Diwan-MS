package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.WatchlistItem;
import com.diwan.portfolio.repository.InstrumentQuoteRepository;
import com.diwan.portfolio.repository.WatchlistItemRepository;
import com.diwan.portfolio.web.dto.Responses.WatchlistItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WatchlistService {

    static final int MAX_ITEMS = 100;

    private final WatchlistItemRepository items;
    private final InstrumentQuoteRepository quotes;
    private final InstrumentService instruments;
    private final Views views;
    private final TransactionTemplate tx;

    public WatchlistService(WatchlistItemRepository items, InstrumentQuoteRepository quotes,
                            InstrumentService instruments, Views views, TransactionTemplate tx) {
        this.items = items;
        this.quotes = quotes;
        this.instruments = instruments;
        this.views = views;
        this.tx = tx;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> list(Long userId) {
        List<WatchlistItem> list = items.findByUserIdOrderByIdAsc(userId);
        Map<Long, InstrumentQuote> q = quotes.findAllById(list.stream().map(i -> i.getInstrument().getId()).toList())
                .stream().collect(Collectors.toMap(InstrumentQuote::getInstrumentId, Function.identity()));
        return list.stream().map(i -> new WatchlistItemResponse(i.getId(),
                views.instrument(i.getInstrument(), q.get(i.getInstrument().getId())), i.getCreatedAt())).toList();
    }

    /** Adding a symbol that is already on the list is not an error. */
    public List<WatchlistItemResponse> add(Long userId, String symbol) {
        Instrument instrument = instruments.resolve(symbol);
        tx.executeWithoutResult(s -> {
            if (items.findByUserIdAndInstrumentId(userId, instrument.getId()).isPresent()) {
                return;
            }
            if (items.findByUserIdOrderByIdAsc(userId).size() >= MAX_ITEMS) {
                throw ApiException.conflict("watchlist.limit_reached", MAX_ITEMS);
            }
            items.save(new WatchlistItem(userId, instrument));
        });
        return list(userId);
    }

    @Transactional
    public void remove(Long userId, String symbol) {
        Instrument instrument = instruments.find(symbol);
        items.findByUserIdAndInstrumentId(userId, instrument.getId()).ifPresent(items::delete);
    }
}
