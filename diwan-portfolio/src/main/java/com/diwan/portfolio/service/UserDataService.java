package com.diwan.portfolio.service;

import com.diwan.portfolio.repository.AlertRuleRepository;
import com.diwan.portfolio.repository.PortfolioAlertRepository;
import com.diwan.portfolio.repository.PortfolioRepository;
import com.diwan.portfolio.repository.TagRepository;
import com.diwan.portfolio.repository.UserPreferenceRepository;
import com.diwan.portfolio.repository.WatchlistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What happens to a user's portfolio data when diwan-users deletes or deactivates the account. */
@Service
public class UserDataService {

    private static final Logger log = LoggerFactory.getLogger(UserDataService.class);

    private final PortfolioRepository portfolios;
    private final TagRepository tags;
    private final WatchlistItemRepository watchlist;
    private final AlertRuleRepository rules;
    private final PortfolioAlertRepository alerts;
    private final UserPreferenceRepository preferences;

    public UserDataService(PortfolioRepository portfolios, TagRepository tags, WatchlistItemRepository watchlist,
                           AlertRuleRepository rules, PortfolioAlertRepository alerts,
                           UserPreferenceRepository preferences) {
        this.portfolios = portfolios;
        this.tags = tags;
        this.watchlist = watchlist;
        this.rules = rules;
        this.alerts = alerts;
        this.preferences = preferences;
    }

    /** Deleted account: remove everything (idempotent, events are delivered at least once). */
    @Transactional
    public void deleteAll(Long userId) {
        int deletedAlerts = alerts.deleteAllOfUser(userId);
        rules.deleteAllOfUser(userId);
        watchlist.deleteAllOfUser(userId);
        int deletedPortfolios = portfolios.deleteAllOfUser(userId); // holdings, transactions... cascade in the DB
        tags.deleteAllOfUser(userId);
        preferences.deleteById(userId);
        log.info("[Portfolio] deleted data of user {}: {} portfolios, {} alerts", userId, deletedPortfolios, deletedAlerts);
    }

    /** Deactivated account: keep the data, stop alerting. */
    @Transactional
    public void deactivate(Long userId) {
        int n = rules.deactivateAllOfUser(userId);
        log.info("[Portfolio] user {} deactivated: {} alert rules paused", userId, n);
    }
}
