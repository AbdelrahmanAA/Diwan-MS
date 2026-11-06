package com.diwan.portfolio.market;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Counts requests against a provider's daily allowance, which restarts at midnight in the provider's time
 * zone. The provider's own count is taken over when it reports one, so a restart of this service does not
 * forget what was already used today.
 */
public class DailyQuota {

    private final int limit;
    private final ZoneId zone;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public DailyQuota(int limit, ZoneId zone, Clock clock) {
        this.limit = limit;
        this.zone = zone;
        this.clock = clock;
    }

    public synchronized boolean tryAcquire() {
        rollOver();
        if (used >= limit) {
            return false;
        }
        used++;
        return true;
    }

    /** The provider says this many requests were used today (it may count more than we did). */
    public synchronized void reportUsed(int reported) {
        rollOver();
        used = Math.max(used, reported);
    }

    /** The provider refused a request because the allowance is gone: no more calls today. */
    public synchronized void exhaust() {
        rollOver();
        used = limit;
    }

    private void rollOver() {
        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
    }
}
