package com.diwan.portfolio.market;

/** Spaces calls evenly so a provider's per-minute quota is never exceeded (blocks the calling job thread). */
public class RateLimiter {

    private final long intervalNanos;
    private long next = System.nanoTime();

    public RateLimiter(int callsPerMinute) {
        this.intervalNanos = 60_000_000_000L / Math.max(1, callsPerMinute);
    }

    public synchronized void acquire() {
        long now = System.nanoTime();
        long wait = next - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        next = Math.max(now, next) + intervalNanos;
    }
}
