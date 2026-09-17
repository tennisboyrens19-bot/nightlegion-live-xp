package com.revalclan.sync;

/** Single-flight, coalescing schedule; safe for client and HTTP callback threads. */
public final class SyncSchedule {
    public static final long PERIOD_MS = 60_000, DEBOUNCE_MS = 5_000, RETRY_MS = 15_000;
    private long generation, nextAllowed, deadline, periodic;
    private boolean dirty = true, inFlight;

    public synchronized void reset(long now) {
        generation++;
        inFlight = false;
        dirty = true;
        nextAllowed = now + 3_000;
        periodic = now;
    }
    public synchronized void request() { dirty = true; }
    public synchronized long begin(long now) {
        if (inFlight && now >= deadline) {
            generation++;
            inFlight = false;
            dirty = true;
            nextAllowed = now + RETRY_MS;
        }
        if (inFlight || now < nextAllowed || (!dirty && now < periodic)) return -1;
        dirty = false;
        inFlight = true;
        deadline = now + 45_000;
        return ++generation;
    }
    public synchronized boolean finish(long ticket, boolean success, long now) {
        if (!inFlight || ticket != generation) return false;
        inFlight = false;
        nextAllowed = now + (success ? DEBOUNCE_MS : RETRY_MS);
        periodic = now + PERIOD_MS;
        if (!success) dirty = true;
        return true;
    }
    public synchronized boolean isCurrent(long ticket) { return inFlight && generation == ticket; }
}
